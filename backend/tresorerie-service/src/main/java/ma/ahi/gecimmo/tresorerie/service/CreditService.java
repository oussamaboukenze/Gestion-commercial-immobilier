package ma.ahi.gecimmo.tresorerie.service;

import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.tresorerie.client.ReservationDto;
import ma.ahi.gecimmo.tresorerie.client.VenteClient;
import ma.ahi.gecimmo.tresorerie.domain.CreditBancaire;
import ma.ahi.gecimmo.tresorerie.domain.Encaissement;
import ma.ahi.gecimmo.tresorerie.exception.BusinessException;
import ma.ahi.gecimmo.tresorerie.repository.CreditBancaireRepository;
import ma.ahi.gecimmo.tresorerie.security.CurrentUser;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.AccordCreditRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.CreditRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.CreditResponse;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.DeblocageRequest;

/** Trésorerie > Crédits bancaires : suivi accord / déblocage. */
@Service
@Transactional
public class CreditService {

    private final CreditBancaireRepository creditRepository;
    private final EncaissementService encaissementService;
    private final VenteClient venteClient;
    private final CurrentUser currentUser;

    public CreditService(CreditBancaireRepository creditRepository, EncaissementService encaissementService,
                         VenteClient venteClient, CurrentUser currentUser) {
        this.creditRepository = creditRepository;
        this.encaissementService = encaissementService;
        this.venteClient = venteClient;
        this.currentUser = currentUser;
    }

    public CreditResponse declarer(CreditRequest req) {
        ReservationDto dossier = venteClient.getReservation(req.reservationId());
        currentUser.verifierAccesProjet(dossier.projetCode());
        if (!dossier.estActive()) {
            throw new BusinessException("Le dossier " + dossier.numero() + " n'est pas actif.");
        }
        CreditBancaire c = new CreditBancaire(dossier.id(), dossier.numero(), dossier.clientNom(),
                dossier.projetCode(), req.banque(), req.montantDemande(), req.dateDemande());
        return CreditResponse.of(creditRepository.save(c));
    }

    public CreditResponse decider(Long id, AccordCreditRequest req) {
        CreditBancaire c = trouver(id);
        if (c.getStatut() != CreditBancaire.Statut.DEMANDE) {
            throw BusinessException.conflict("Ce crédit n'est plus au statut DEMANDE.");
        }
        if (req.accorde()) {
            if (req.montantAccorde() == null || req.montantAccorde().signum() <= 0) {
                throw new BusinessException("Montant accordé obligatoire.");
            }
            c.accorder(req.montantAccorde(), req.date());
        } else {
            c.refuser(req.date());
        }
        return CreditResponse.of(c);
    }

    /** Déblocage : crée automatiquement l'encaissement (virement de la banque). */
    public CreditResponse debloquer(Long id, DeblocageRequest req) {
        CreditBancaire c = trouver(id);
        if (c.getStatut() != CreditBancaire.Statut.ACCORDE) {
            throw BusinessException.conflict("Seul un crédit accordé peut être débloqué.");
        }
        ReservationDto dossier = venteClient.getReservation(c.getReservationId());
        if (c.getMontantAccorde().compareTo(dossier.resteAPayer()) > 0) {
            throw new BusinessException("Montant débloqué supérieur au reste à payer (" + dossier.resteAPayer() + " DH).");
        }
        Encaissement e = encaissementService.enregistrerDeblocage(dossier, c.getMontantAccorde(), c.getBanque(),
                req.reference(), req.dateDeblocage());
        c.debloquer(req.dateDeblocage(), e.getId());
        return CreditResponse.of(c);
    }

    @Transactional(readOnly = true)
    public List<CreditResponse> enCours() {
        return creditRepository.findByStatutInOrderByDateDemande(EnumSet.of(CreditBancaire.Statut.DEMANDE,
                        CreditBancaire.Statut.ACCORDE)).stream()
                .filter(c -> currentUser.peutVoirProjet(c.getProjetCode()))
                .map(CreditResponse::of).toList();
    }

    private CreditBancaire trouver(Long id) {
        CreditBancaire c = creditRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Crédit introuvable : " + id));
        currentUser.verifierAccesProjet(c.getProjetCode());
        return c;
    }
}
