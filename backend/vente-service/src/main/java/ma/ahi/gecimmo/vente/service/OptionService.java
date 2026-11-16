package ma.ahi.gecimmo.vente.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.vente.client.LotDto;
import ma.ahi.gecimmo.vente.client.ProspectDto;
import ma.ahi.gecimmo.vente.client.ReferentielClient;
import ma.ahi.gecimmo.vente.client.ReferentielClient.ChangementStatutRequest;
import ma.ahi.gecimmo.vente.client.CrmClient;
import ma.ahi.gecimmo.vente.domain.LotStatut;
import ma.ahi.gecimmo.vente.domain.OptionLot;
import ma.ahi.gecimmo.vente.domain.ReservationStatut;
import ma.ahi.gecimmo.vente.exception.BusinessException;
import ma.ahi.gecimmo.vente.messaging.VenteEventPublisher;
import ma.ahi.gecimmo.vente.repository.LotEngagementRepository;
import ma.ahi.gecimmo.vente.repository.OptionLotRepository;
import ma.ahi.gecimmo.vente.repository.ReservationRepository;
import ma.ahi.gecimmo.vente.security.CurrentUser;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.OptionResponse;

/** Fiche du lot > Mettre en option (48 h max, 1 option active par prospect). */
@Service
@Transactional
public class OptionService {

    private static final Logger log = LoggerFactory.getLogger(OptionService.class);

    private final OptionLotRepository optionRepository;
    private final ReservationRepository reservationRepository;
    private final LotEngagementRepository engagementRepository;
    private final ReferentielClient referentielClient;
    private final CrmClient crmClient;
    private final VenteEventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public OptionService(OptionLotRepository optionRepository, ReservationRepository reservationRepository,
                         LotEngagementRepository engagementRepository, ReferentielClient referentielClient,
                         CrmClient crmClient, VenteEventPublisher eventPublisher, CurrentUser currentUser) {
        this.optionRepository = optionRepository;
        this.reservationRepository = reservationRepository;
        this.engagementRepository = engagementRepository;
        this.referentielClient = referentielClient;
        this.crmClient = crmClient;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    public OptionResponse poser(Long lotId, Long prospectId) {
        LocalDateTime maintenant = LocalDateTime.now();
        LotDto lot = referentielClient.getLot(lotId);
        currentUser.verifierAccesProjet(lot.projetCode());
        ProspectDto prospect = crmClient.getProspect(prospectId);
        if (currentUser.estCommercialSimple() && !currentUser.username().equals(prospect.commercial())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Ce prospect appartient à un autre commercial.");
        }
        optionRepository.findFirstByProspectIdAndStatutAndDateExpirationAfter(prospectId,
                OptionLot.Statut.ACTIVE, maintenant).ifPresent(o -> {
                    throw BusinessException.conflict("Ce prospect a déjà une option active sur le lot "
                            + o.getLotCode() + " (1 option active par prospect).");
                });
        if (engagementRepository.existsById(lotId)
                || reservationRepository.findFirstByLotIdAndStatutIn(lotId, ReservationStatut.ACTIFS).isPresent()) {
            throw BusinessException.conflict("Le lot " + lot.code() + " a déjà une réservation en cours.");
        }
        // Blocage atomique du lot dans le référentiel (409 si un autre commercial a été plus rapide)
        referentielClient.changerStatut(lotId,
                new ChangementStatutRequest(LotStatut.OPTION, List.of(LotStatut.DISPONIBLE)));
        try {
            OptionLot option = new OptionLot(lotId, lot.code(), lot.projetCode(), prospectId,
                    currentUser.username(), maintenant);
            return OptionResponse.of(optionRepository.saveAndFlush(option));
        } catch (RuntimeException e) {
            // Compensation : on libère le lot si l'option n'a pas pu être enregistrée
            log.error("Option non enregistrée, libération du lot {}", lotId, e);
            eventPublisher.lotStatutImmediat(lotId, LotStatut.DISPONIBLE, List.of(LotStatut.OPTION),
                    "compensation option");
            throw e;
        }
    }

    /** Le commercial libère son option avant les 48 h. */
    public OptionResponse lever(Long optionId) {
        OptionLot option = optionRepository.findById(optionId)
                .orElseThrow(() -> BusinessException.notFound("Option introuvable : " + optionId));
        if (currentUser.estCommercialSimple() && !currentUser.username().equals(option.getCommercial())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cette option appartient à un autre commercial.");
        }
        if (option.getStatut() != OptionLot.Statut.ACTIVE) {
            throw BusinessException.conflict("Cette option n'est plus active (" + option.getStatut() + ").");
        }
        option.changerStatut(OptionLot.Statut.LEVEE);
        eventPublisher.lotStatut(option.getLotId(), LotStatut.DISPONIBLE, List.of(LotStatut.OPTION),
                "option levée");
        return OptionResponse.of(option);
    }

    @Transactional(readOnly = true)
    public List<OptionResponse> mesOptions() {
        return optionRepository.findByCommercialAndStatutOrderByDateExpiration(currentUser.username(),
                OptionLot.Statut.ACTIVE).stream().map(OptionResponse::of).toList();
    }

    /** Option non transformée sous 48 h -> le lot repasse automatiquement "Disponible". */
    public int expirerOptions() {
        List<OptionLot> expirees = optionRepository.findByStatutAndDateExpirationBefore(OptionLot.Statut.ACTIVE,
                LocalDateTime.now());
        for (OptionLot option : expirees) {
            option.changerStatut(OptionLot.Statut.EXPIREE);
            eventPublisher.lotStatut(option.getLotId(), LotStatut.DISPONIBLE, List.of(LotStatut.OPTION),
                    "option expirée (48 h)");
        }
        return expirees.size();
    }
}
