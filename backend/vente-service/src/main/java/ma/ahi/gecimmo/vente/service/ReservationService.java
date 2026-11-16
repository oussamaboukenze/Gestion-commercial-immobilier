package ma.ahi.gecimmo.vente.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.vente.client.CrmClient;
import ma.ahi.gecimmo.vente.client.LotDto;
import ma.ahi.gecimmo.vente.client.ProspectDto;
import ma.ahi.gecimmo.vente.client.ReferentielClient;
import ma.ahi.gecimmo.vente.client.ReferentielClient.ChangementStatutRequest;
import ma.ahi.gecimmo.vente.domain.LotEngagement;
import ma.ahi.gecimmo.vente.domain.LotStatut;
import ma.ahi.gecimmo.vente.domain.OptionLot;
import ma.ahi.gecimmo.vente.domain.RegleVente;
import ma.ahi.gecimmo.vente.domain.Reservation;
import ma.ahi.gecimmo.vente.domain.ReservationStatut;
import ma.ahi.gecimmo.vente.domain.StatutRemise;
import ma.ahi.gecimmo.vente.exception.BusinessException;
import ma.ahi.gecimmo.vente.messaging.Events.ReservationValideeEvent;
import ma.ahi.gecimmo.vente.messaging.VenteEventPublisher;
import ma.ahi.gecimmo.vente.repository.LotEngagementRepository;
import ma.ahi.gecimmo.vente.repository.OptionLotRepository;
import ma.ahi.gecimmo.vente.repository.ReservationRepository;
import ma.ahi.gecimmo.vente.security.CurrentUser;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.PiecesRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.ReservationRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.ReservationResponse;

/**
 * Ventes > Réservations.
 * Commercial : crée la réservation -> "En attente de validation ADV".
 * ADV : contrôle pièces + avance confirmée par la trésorerie -> Valider (lot "Réservé").
 */
@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final LotEngagementRepository engagementRepository;
    private final OptionLotRepository optionRepository;
    private final ReferentielClient referentielClient;
    private final CrmClient crmClient;
    private final VenteEventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public ReservationService(ReservationRepository reservationRepository,
                              LotEngagementRepository engagementRepository, OptionLotRepository optionRepository,
                              ReferentielClient referentielClient, CrmClient crmClient,
                              VenteEventPublisher eventPublisher, CurrentUser currentUser) {
        this.reservationRepository = reservationRepository;
        this.engagementRepository = engagementRepository;
        this.optionRepository = optionRepository;
        this.referentielClient = referentielClient;
        this.crmClient = crmClient;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    // ---------- Création (commercial) ----------

    public ReservationResponse creer(ReservationRequest req) {
        LocalDateTime maintenant = LocalDateTime.now();
        LotDto lot = referentielClient.getLot(req.lotId());
        currentUser.verifierAccesProjet(lot.projetCode());
        ProspectDto prospect = crmClient.getProspect(req.prospectId());
        if (currentUser.estCommercialSimple() && !currentUser.username().equals(prospect.commercial())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Ce prospect appartient à un autre commercial.");
        }

        // Le lot doit être Disponible, ou en option pour CE prospect
        if (lot.statut() == LotStatut.OPTION) {
            OptionLot option = optionRepository.findFirstByLotIdAndStatut(lot.id(), OptionLot.Statut.ACTIVE)
                    .orElse(null);
            if (option == null || !option.getProspectId().equals(prospect.id()) || !option.estActive(maintenant)) {
                throw BusinessException.conflict("Le lot " + lot.code()
                        + " est en option pour un autre prospect.");
            }
        } else if (lot.statut() != LotStatut.DISPONIBLE) {
            throw BusinessException.conflict("Le lot " + lot.code() + " n'est pas disponible (" + lot.statut() + ").");
        }

        BigDecimal remise = req.remisePct() == null ? BigDecimal.ZERO : req.remisePct();
        BigDecimal prixNet = RegleVente.prixNet(lot.prixTTC(), remise);
        BigDecimal avanceMin = RegleVente.avanceMinimale(prixNet);
        if (req.avance().compareTo(avanceMin) < 0) {
            throw new BusinessException("Avance insuffisante : minimum 5 % du prix TTC, soit " + avanceMin + " DH.");
        }
        if (req.avance().compareTo(prixNet) > 0) {
            throw new BusinessException("L'avance ne peut pas dépasser le prix net (" + prixNet + " DH).");
        }

        Reservation reservation = new Reservation(lot.id(), lot.code(), lot.projetCode(), prospect.id(),
                prospect.nomComplet(), prospect.cin(), prospect.telephone(), currentUser.username(),
                lot.prixTTC(), remise, req.avance());
        reservation.setPieces(req.pieceCin(), req.pieceJustificatifRevenus(), req.pieceRecuAvance());
        reservation = reservationRepository.saveAndFlush(reservation);
        reservation.attribuerNumero();

        // Verrou base : 1 seule réservation active par lot
        try {
            engagementRepository.saveAndFlush(new LotEngagement(lot.id(), reservation.getId()));
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.conflict("Le lot " + lot.code() + " a déjà une réservation active.");
        }
        return ReservationResponse.of(reservation);
    }

    public ReservationResponse majPieces(Long id, PiecesRequest req) {
        Reservation r = trouverAvecControle(id);
        if (r.getStatut() != ReservationStatut.EN_ATTENTE_VALIDATION_ADV) {
            throw BusinessException.conflict("Les pièces ne sont modifiables qu'avant la validation ADV.");
        }
        r.setPieces(req.pieceCin(), req.pieceJustificatifRevenus(), req.pieceRecuAvance());
        return ReservationResponse.of(r);
    }

    // ---------- Remise (responsable commercial / direction) ----------

    public ReservationResponse deciderRemise(Long id, boolean accordee) {
        Reservation r = trouverAvecControle(id);
        StatutRemise statut = r.getStatutRemise();
        if (statut == StatutRemise.EN_ATTENTE_RESP_COMMERCIAL) {
            if (!currentUser.hasRole("RESP_COMMERCIAL") && !currentUser.hasRole("DIRECTION")) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "Une remise entre 2 % et 5 % est validée par le responsable commercial.");
            }
        } else if (statut == StatutRemise.EN_ATTENTE_DIRECTION) {
            if (!currentUser.hasRole("DIRECTION")) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "Une remise au-delà de 5 % est validée par la direction.");
            }
        } else {
            throw BusinessException.conflict("Aucune remise en attente de validation sur cette réservation.");
        }
        r.deciderRemise(accordee, currentUser.username());
        return ReservationResponse.of(r);
    }

    // ---------- Validation ADV ----------

    public ReservationResponse valider(Long id) {
        Reservation r = trouverAvecControle(id);
        if (r.getStatut() != ReservationStatut.EN_ATTENTE_VALIDATION_ADV) {
            throw BusinessException.conflict("Cette réservation n'est pas en attente de validation.");
        }
        if (!r.getStatutRemise().estAcquise()) {
            throw new BusinessException("Remise de " + r.getRemisePct() + " % non validée ("
                    + r.getStatutRemise() + ").");
        }
        if (!r.dossierComplet()) {
            throw new BusinessException("Dossier incomplet : CIN, justificatif de revenus et reçu de l'avance requis.");
        }
        if (!r.avanceConfirmee()) {
            throw new BusinessException("Avance non confirmée par la trésorerie (encaissé : " + r.getTotalEncaisse()
                    + " DH / avance : " + r.getAvance() + " DH).");
        }
        referentielClient.changerStatut(r.getLotId(),
                new ChangementStatutRequest(LotStatut.RESERVE, List.of(LotStatut.DISPONIBLE, LotStatut.OPTION)));
        optionRepository.findFirstByLotIdAndStatut(r.getLotId(), OptionLot.Statut.ACTIVE)
                .ifPresent(o -> o.changerStatut(OptionLot.Statut.TRANSFORMEE));
        r.valider(currentUser.username(), LocalDateTime.now());
        eventPublisher.reservationValidee(new ReservationValideeEvent(r.getId(), r.getProspectId(), r.getLotId(),
                r.getLotCode(), r.getProjetCode(), r.getCommercial(), r.getPrixNet()));
        return ReservationResponse.of(r);
    }

    public ReservationResponse rejeter(Long id, String motif) {
        Reservation r = trouverAvecControle(id);
        if (r.getStatut() != ReservationStatut.EN_ATTENTE_VALIDATION_ADV) {
            throw BusinessException.conflict("Seule une réservation en attente peut être rejetée.");
        }
        r.rejeter(motif);
        engagementRepository.deleteById(r.getLotId());
        return ReservationResponse.of(r);
    }

    // ---------- Consultation ----------

    @Transactional(readOnly = true)
    public Page<ReservationResponse> rechercher(ReservationStatut statut, String projet, Pageable pageable) {
        boolean tous = false;
        Collection<String> projets;
        if (projet != null && !projet.isBlank()) {
            currentUser.verifierAccesProjet(projet.toUpperCase());
            projets = Set.of(projet.toUpperCase());
        } else if (currentUser.voitTousLesProjets()) {
            tous = true;
            projets = Set.of("*");
        } else {
            projets = currentUser.projetsAutorises();
            if (projets.isEmpty()) {
                return Page.empty(pageable);
            }
        }
        String commercial = currentUser.estCommercialSimple() ? currentUser.username() : null;
        return reservationRepository.rechercher(tous, projets, commercial, statut, pageable)
                .map(ReservationResponse::of);
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(Long id) {
        return ReservationResponse.of(trouverAvecControle(id));
    }

    /** Réservations validées depuis plus de 30 jours sans compromis : à régulariser par l'ADV. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> aRegulariser() {
        return reservationRepository.findByStatutAndDateLimiteCompromisBeforeOrderByDateLimiteCompromis(
                        ReservationStatut.VALIDEE, LocalDate.now()).stream()
                .filter(r -> currentUser.peutVoirProjet(r.getProjetCode()))
                .map(ReservationResponse::of).toList();
    }

    /** Remises en attente : 2-5 % pour le responsable commercial, > 5 % pour la direction. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> remisesEnAttente() {
        java.util.EnumSet<StatutRemise> statuts = currentUser.hasRole("DIRECTION")
                ? java.util.EnumSet.of(StatutRemise.EN_ATTENTE_RESP_COMMERCIAL, StatutRemise.EN_ATTENTE_DIRECTION)
                : java.util.EnumSet.of(StatutRemise.EN_ATTENTE_RESP_COMMERCIAL);
        return reservationRepository.findByStatutAndStatutRemiseInOrderByDateCreation(
                        ReservationStatut.EN_ATTENTE_VALIDATION_ADV, statuts).stream()
                .filter(r -> currentUser.peutVoirProjet(r.getProjetCode()))
                .map(ReservationResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getInterne(Long id) {
        return ReservationResponse.of(trouver(id));
    }

    Reservation trouver(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Réservation introuvable : " + id));
    }

    Reservation trouverAvecControle(Long id) {
        Reservation r = trouver(id);
        currentUser.verifierAccesProjet(r.getProjetCode());
        if (currentUser.estCommercialSimple() && !currentUser.username().equals(r.getCommercial())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cette réservation appartient à un autre commercial.");
        }
        return r;
    }
}
