package ma.ahi.gecimmo.vente.service;

import java.util.EnumSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.vente.domain.Desistement;
import ma.ahi.gecimmo.vente.domain.LotStatut;
import ma.ahi.gecimmo.vente.domain.Reservation;
import ma.ahi.gecimmo.vente.domain.ReservationStatut;
import ma.ahi.gecimmo.vente.exception.BusinessException;
import ma.ahi.gecimmo.vente.messaging.VenteEventPublisher;
import ma.ahi.gecimmo.vente.repository.DesistementRepository;
import ma.ahi.gecimmo.vente.repository.LotEngagementRepository;
import ma.ahi.gecimmo.vente.security.CurrentUser;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.DesistementRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.DesistementResponse;

/**
 * Ventes > Désistements.
 * Avant compromis : remboursement - 10 % de frais, validation responsable commercial.
 * Après compromis : selon clauses, validation responsable commercial + DAF.
 */
@Service
@Transactional
public class DesistementService {

    private static final EnumSet<Desistement.Statut> EN_ATTENTE =
            EnumSet.of(Desistement.Statut.EN_ATTENTE_RESP_COMMERCIAL, Desistement.Statut.EN_ATTENTE_DAF);

    private final DesistementRepository desistementRepository;
    private final LotEngagementRepository engagementRepository;
    private final ReservationService reservationService;
    private final VenteEventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public DesistementService(DesistementRepository desistementRepository,
                              LotEngagementRepository engagementRepository, ReservationService reservationService,
                              VenteEventPublisher eventPublisher, CurrentUser currentUser) {
        this.desistementRepository = desistementRepository;
        this.engagementRepository = engagementRepository;
        this.reservationService = reservationService;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    /** ADV : saisie du motif + calcul du remboursement. */
    public DesistementResponse saisir(DesistementRequest req) {
        Reservation r = reservationService.trouverAvecControle(req.reservationId());
        if (!r.estActive()) {
            throw BusinessException.conflict("La réservation " + r.getNumero() + " n'est pas active (" + r.getStatut() + ").");
        }
        if (desistementRepository.existsByReservationIdAndStatutIn(r.getId(), EN_ATTENTE)) {
            throw BusinessException.conflict("Un désistement est déjà en attente de validation pour ce dossier.");
        }
        try {
            Desistement d = new Desistement(r, req.motif(), req.montantSelonClauses(), currentUser.username());
            return DesistementResponse.of(desistementRepository.save(d));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    /** Responsable commercial (1re validation). */
    public DesistementResponse validerRespCommercial(Long id) {
        Desistement d = trouver(id);
        if (d.getStatut() != Desistement.Statut.EN_ATTENTE_RESP_COMMERCIAL) {
            throw BusinessException.conflict("Ce désistement n'attend pas la validation du responsable commercial.");
        }
        d.validerParRespCommercial(currentUser.username());
        if (d.getStatut() == Desistement.Statut.VALIDE) {
            appliquer(d);
        }
        return DesistementResponse.of(d);
    }

    /** DAF (obligatoire après compromis). */
    public DesistementResponse validerDaf(Long id) {
        Desistement d = trouver(id);
        if (d.getStatut() != Desistement.Statut.EN_ATTENTE_DAF) {
            throw BusinessException.conflict("Ce désistement n'attend pas la validation du DAF.");
        }
        d.validerParDaf(currentUser.username());
        appliquer(d);
        return DesistementResponse.of(d);
    }

    public DesistementResponse refuser(Long id, String motif) {
        Desistement d = trouver(id);
        if (!d.estEnAttente()) {
            throw BusinessException.conflict("Ce désistement a déjà été traité.");
        }
        boolean attendDaf = d.getStatut() == Desistement.Statut.EN_ATTENTE_DAF;
        if (attendDaf && !currentUser.hasRole("DAF")) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Ce désistement est en attente du DAF.");
        }
        if (!attendDaf && !currentUser.hasRole("RESP_COMMERCIAL")) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Ce désistement est en attente du responsable commercial.");
        }
        d.refuser(currentUser.username(), motif);
        return DesistementResponse.of(d);
    }

    @Transactional(readOnly = true)
    public List<DesistementResponse> enAttente() {
        return desistementRepository.findByStatutInOrderByDateSaisie(EN_ATTENTE).stream()
                .filter(d -> currentUser.peutVoirProjet(d.getReservation().getProjetCode()))
                .map(DesistementResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public DesistementResponse get(Long id) {
        return DesistementResponse.of(trouver(id));
    }

    /** Vente annulée -> le lot redevient Disponible. */
    private void appliquer(Desistement d) {
        Reservation r = d.getReservation();
        ReservationStatut avant = d.getStatutReservationInitial();
        r.setStatut(ReservationStatut.DESISTEE);
        engagementRepository.deleteById(r.getLotId());
        if (avant == ReservationStatut.VALIDEE || avant == ReservationStatut.COMPROMIS_SIGNE) {
            eventPublisher.lotStatut(r.getLotId(), LotStatut.DISPONIBLE,
                    List.of(LotStatut.RESERVE, LotStatut.COMPROMIS_SIGNE), "désistement " + r.getNumero());
        }
    }

    private Desistement trouver(Long id) {
        Desistement d = desistementRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Désistement introuvable : " + id));
        currentUser.verifierAccesProjet(d.getReservation().getProjetCode());
        return d;
    }
}
