package ma.ahi.gecimmo.vente.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.vente.client.LotDto;
import ma.ahi.gecimmo.vente.client.ReferentielClient;
import ma.ahi.gecimmo.vente.client.ReferentielClient.ChangementStatutRequest;
import ma.ahi.gecimmo.vente.domain.ChangementLot;
import ma.ahi.gecimmo.vente.domain.LotEngagement;
import ma.ahi.gecimmo.vente.domain.LotStatut;
import ma.ahi.gecimmo.vente.domain.Reservation;
import ma.ahi.gecimmo.vente.domain.ReservationStatut;
import ma.ahi.gecimmo.vente.exception.BusinessException;
import ma.ahi.gecimmo.vente.messaging.VenteEventPublisher;
import ma.ahi.gecimmo.vente.repository.ChangementLotRepository;
import ma.ahi.gecimmo.vente.repository.CompromisRepository;
import ma.ahi.gecimmo.vente.repository.LotEngagementRepository;
import ma.ahi.gecimmo.vente.security.CurrentUser;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.ChangementLotRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.ChangementLotResponse;

/**
 * Ventes > Changements de lot : l'ancien lot est libéré, les versements sont transférés
 * (ils restent sur le dossier). Validation du responsable commercial.
 */
@Service
@Transactional
public class ChangementLotService {

    private final ChangementLotRepository changementRepository;
    private final LotEngagementRepository engagementRepository;
    private final CompromisRepository compromisRepository;
    private final ReservationService reservationService;
    private final ReferentielClient referentielClient;
    private final VenteEventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public ChangementLotService(ChangementLotRepository changementRepository,
                                LotEngagementRepository engagementRepository,
                                CompromisRepository compromisRepository, ReservationService reservationService,
                                ReferentielClient referentielClient, VenteEventPublisher eventPublisher,
                                CurrentUser currentUser) {
        this.changementRepository = changementRepository;
        this.engagementRepository = engagementRepository;
        this.compromisRepository = compromisRepository;
        this.reservationService = reservationService;
        this.referentielClient = referentielClient;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    /** ADV : demande de changement de lot. */
    public ChangementLotResponse demander(ChangementLotRequest req) {
        Reservation r = reservationService.trouverAvecControle(req.reservationId());
        if (!r.estActive()) {
            throw BusinessException.conflict("La réservation n'est pas active (" + r.getStatut() + ").");
        }
        if (changementRepository.existsByReservationIdAndStatut(r.getId(), ChangementLot.Statut.EN_ATTENTE)) {
            throw BusinessException.conflict("Un changement de lot est déjà en attente pour ce dossier.");
        }
        LotDto nouveau = referentielClient.getLot(req.nouveauLotId());
        if (nouveau.id().equals(r.getLotId())) {
            throw new BusinessException("Le nouveau lot est identique au lot actuel.");
        }
        if (!nouveau.projetCode().equals(r.getProjetCode())) {
            throw new BusinessException("Le changement de lot se fait à l'intérieur du même projet.");
        }
        if (nouveau.statut() != LotStatut.DISPONIBLE) {
            throw BusinessException.conflict("Le lot " + nouveau.code() + " n'est pas disponible (" + nouveau.statut() + ").");
        }
        ChangementLot c = new ChangementLot(r, nouveau.id(), nouveau.code(), nouveau.prixTTC(), req.motif(),
                currentUser.username());
        return ChangementLotResponse.of(changementRepository.save(c));
    }

    /** Responsable commercial : validation -> bascule effective. */
    public ChangementLotResponse valider(Long id) {
        ChangementLot c = trouver(id);
        Reservation r = c.getReservation();
        if (!r.estActive()) {
            throw BusinessException.conflict("La réservation n'est plus active.");
        }

        // 1. Verrouiller le nouveau lot (1 réservation active par lot)
        try {
            engagementRepository.saveAndFlush(new LotEngagement(c.getNouveauLotId(), r.getId()));
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.conflict("Le lot " + c.getNouveauLotCode() + " a été réservé entre-temps.");
        }

        // 2. Statut du nouveau lot dans le référentiel (même étape du cycle que l'ancien)
        if (r.getStatut() == ReservationStatut.VALIDEE || r.getStatut() == ReservationStatut.COMPROMIS_SIGNE) {
            referentielClient.changerStatut(c.getNouveauLotId(),
                    new ChangementStatutRequest(LotStatut.RESERVE, List.of(LotStatut.DISPONIBLE)));
            if (r.getStatut() == ReservationStatut.COMPROMIS_SIGNE) {
                referentielClient.changerStatut(c.getNouveauLotId(),
                        new ChangementStatutRequest(LotStatut.COMPROMIS_SIGNE, List.of(LotStatut.RESERVE)));
            }
            // 3. Libérer l'ancien lot
            eventPublisher.lotStatut(c.getAncienLotId(), LotStatut.DISPONIBLE,
                    List.of(LotStatut.RESERVE, LotStatut.COMPROMIS_SIGNE), "changement de lot " + r.getNumero());
        }
        engagementRepository.deleteById(c.getAncienLotId());

        // 4. Le dossier suit le nouveau lot ; l'échéancier est recalculé, les versements conservés
        r.changerLot(c.getNouveauLotId(), c.getNouveauLotCode(), c.getNouveauPrix());
        compromisRepository.findByReservationId(r.getId()).ifPresent(cp -> cp.recalculerMontants());
        c.decider(true, currentUser.username());
        return ChangementLotResponse.of(c);
    }

    public ChangementLotResponse refuser(Long id) {
        ChangementLot c = trouver(id);
        c.decider(false, currentUser.username());
        return ChangementLotResponse.of(c);
    }

    @Transactional(readOnly = true)
    public List<ChangementLotResponse> enAttente() {
        return changementRepository.findByStatutOrderByDateSaisie(ChangementLot.Statut.EN_ATTENTE).stream()
                .filter(c -> currentUser.peutVoirProjet(c.getReservation().getProjetCode()))
                .map(ChangementLotResponse::of).toList();
    }

    private ChangementLot trouver(Long id) {
        ChangementLot c = changementRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Changement de lot introuvable : " + id));
        currentUser.verifierAccesProjet(c.getReservation().getProjetCode());
        if (c.getStatut() != ChangementLot.Statut.EN_ATTENTE) {
            throw BusinessException.conflict("Ce changement de lot a déjà été traité.");
        }
        return c;
    }
}
