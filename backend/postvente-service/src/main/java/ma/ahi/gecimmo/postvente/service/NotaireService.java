package ma.ahi.gecimmo.postvente.service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.postvente.client.ReferentielClient;
import ma.ahi.gecimmo.postvente.client.ReferentielClient.ChangementStatutRequest;
import ma.ahi.gecimmo.postvente.client.ReservationDto;
import ma.ahi.gecimmo.postvente.client.VenteClient;
import ma.ahi.gecimmo.postvente.domain.DossierNotaire;
import ma.ahi.gecimmo.postvente.exception.BusinessException;
import ma.ahi.gecimmo.postvente.messaging.PostventeEventPublisher;
import ma.ahi.gecimmo.postvente.messaging.PostventeEventPublisher.ActeSigneEvent;
import ma.ahi.gecimmo.postvente.repository.DossierNotaireRepository;
import ma.ahi.gecimmo.postvente.security.CurrentUser;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.ActeRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.DossierNotaireRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.DossierNotaireResponse;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.TransmissionRequest;

/**
 * Notaire & Actes. L'acte définitif se signe chez le notaire partenaire quand le solde est payé
 * (le déblocage d'un crédit bancaire est saisi en trésorerie et compte comme encaissement).
 */
@Service
@Transactional
public class NotaireService {

    private final DossierNotaireRepository dossierRepository;
    private final VenteClient venteClient;
    private final ReferentielClient referentielClient;
    private final PostventeEventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public NotaireService(DossierNotaireRepository dossierRepository, VenteClient venteClient,
                          ReferentielClient referentielClient, PostventeEventPublisher eventPublisher,
                          CurrentUser currentUser) {
        this.dossierRepository = dossierRepository;
        this.venteClient = venteClient;
        this.referentielClient = referentielClient;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    /** Notaire & Actes > Dossiers notaire > Nouveau (compromis signé obligatoire). */
    public DossierNotaireResponse ouvrir(DossierNotaireRequest req) {
        ReservationDto r = venteClient.getReservation(req.reservationId());
        currentUser.verifierAccesProjet(r.projetCode());
        if (!"COMPROMIS_SIGNE".equals(r.statut())) {
            throw new BusinessException("Le dossier notaire s'ouvre après la signature du compromis (statut : "
                    + r.statut() + ").");
        }
        if (dossierRepository.existsByReservationId(r.id())) {
            throw BusinessException.conflict("Un dossier notaire existe déjà pour " + r.numero() + ".");
        }
        DossierNotaire d = new DossierNotaire(r.id(), r.numero(), r.lotId(), r.lotCode(), r.projetCode(),
                r.clientNom(), req.notaire(), req.observations(), currentUser.username());
        return DossierNotaireResponse.of(dossierRepository.save(d), r.resteAPayer());
    }

    public DossierNotaireResponse transmettre(Long id, TransmissionRequest req) {
        DossierNotaire d = trouver(id);
        if (d.getStatut() != DossierNotaire.Statut.EN_PREPARATION) {
            throw BusinessException.conflict("Le dossier a déjà été transmis.");
        }
        d.transmettre(req.dateTransmission());
        if (req.observations() != null) {
            d.setObservations(req.observations());
        }
        return DossierNotaireResponse.of(d, null);
    }

    /** Acte signé : solde payé obligatoire, le lot passe "Vendu". */
    public DossierNotaireResponse signerActe(Long id, ActeRequest req) {
        DossierNotaire d = trouver(id);
        if (d.getStatut() == DossierNotaire.Statut.ACTE_SIGNE) {
            throw BusinessException.conflict("L'acte est déjà signé.");
        }
        ReservationDto r = venteClient.getReservation(d.getReservationId());
        if (!r.soldeNul()) {
            throw new BusinessException("Solde non réglé : " + r.resteAPayer()
                    + " DH restent à encaisser (paiement client ou déblocage du crédit).");
        }
        referentielClient.changerStatut(d.getLotId(),
                new ChangementStatutRequest("VENDU", List.of("COMPROMIS_SIGNE")));
        d.signerActe(req.numeroActe(), req.dateSignature());
        eventPublisher.acteSigne(new ActeSigneEvent(d.getReservationId(), d.getReservationNumero(), d.getLotId(),
                d.getLotCode(), d.getNumeroActe(), d.getDateSignatureActe()));
        return DossierNotaireResponse.of(d, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<DossierNotaireResponse> enCours() {
        return dossierRepository.findByStatutInOrderByDateOuvertureDesc(
                        EnumSet.of(DossierNotaire.Statut.EN_PREPARATION, DossierNotaire.Statut.TRANSMIS)).stream()
                .filter(d -> currentUser.peutVoirProjet(d.getProjetCode()))
                .map(d -> DossierNotaireResponse.of(d, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<DossierNotaireResponse> actesSignes() {
        return dossierRepository.findByStatutInOrderByDateOuvertureDesc(EnumSet.of(DossierNotaire.Statut.ACTE_SIGNE))
                .stream().filter(d -> currentUser.peutVoirProjet(d.getProjetCode()))
                .map(d -> DossierNotaireResponse.of(d, null)).toList();
    }

    @Transactional(readOnly = true)
    public DossierNotaireResponse get(Long id) {
        DossierNotaire d = trouver(id);
        return DossierNotaireResponse.of(d, venteClient.getReservation(d.getReservationId()).resteAPayer());
    }

    @Transactional(readOnly = true)
    public DossierNotaireResponse parReservation(Long reservationId) {
        DossierNotaire d = dossierRepository.findByReservationId(reservationId)
                .orElseThrow(() -> BusinessException.notFound("Aucun dossier notaire pour cette réservation."));
        currentUser.verifierAccesProjet(d.getProjetCode());
        return DossierNotaireResponse.of(d, null);
    }

    DossierNotaire acteSignePour(Long reservationId) {
        DossierNotaire d = dossierRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new BusinessException("Aucun dossier notaire : l'acte doit être signé avant la livraison."));
        if (d.getStatut() != DossierNotaire.Statut.ACTE_SIGNE) {
            throw new BusinessException("L'acte définitif n'est pas encore signé.");
        }
        return d;
    }

    private DossierNotaire trouver(Long id) {
        DossierNotaire d = dossierRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Dossier notaire introuvable : " + id));
        currentUser.verifierAccesProjet(d.getProjetCode());
        return d;
    }
}
