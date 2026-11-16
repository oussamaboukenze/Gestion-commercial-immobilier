package ma.ahi.gecimmo.postvente.service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.postvente.domain.Livraison;
import ma.ahi.gecimmo.postvente.domain.TicketSav;
import ma.ahi.gecimmo.postvente.exception.BusinessException;
import ma.ahi.gecimmo.postvente.repository.TicketSavRepository;
import ma.ahi.gecimmo.postvente.security.CurrentUser;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.InterventionRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.TicketRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.TicketResponse;

/** SAV > Tickets / Interventions / Clôture. */
@Service
@Transactional
public class SavService {

    private final TicketSavRepository ticketRepository;
    private final LivraisonService livraisonService;
    private final CurrentUser currentUser;

    public SavService(TicketSavRepository ticketRepository, LivraisonService livraisonService,
                      CurrentUser currentUser) {
        this.ticketRepository = ticketRepository;
        this.livraisonService = livraisonService;
        this.currentUser = currentUser;
    }

    public TicketResponse ouvrir(TicketRequest req) {
        Livraison l = livraisonService.trouver(req.livraisonId());
        if (l.getStatut() != Livraison.Statut.LIVREE) {
            throw new BusinessException("Le SAV concerne uniquement les lots livrés.");
        }
        if (!l.savOuvert(LocalDate.now())) {
            throw new BusinessException("Réclamation hors délai : le SAV est ouvert 12 mois après la livraison ("
                    + l.getDateLivraison() + ").");
        }
        TicketSav t = ticketRepository.saveAndFlush(new TicketSav(l, req.categorie(), req.priorite(),
                req.description(), currentUser.username()));
        t.attribuerNumero();
        return TicketResponse.of(t);
    }

    public TicketResponse ajouterIntervention(Long id, InterventionRequest req) {
        TicketSav t = trouver(id);
        verifierOuvert(t);
        t.ajouterIntervention(req.dateIntervention(), req.intervenant(), req.travaux());
        return TicketResponse.of(ticketRepository.saveAndFlush(t));
    }

    public TicketResponse resoudre(Long id) {
        TicketSav t = trouver(id);
        verifierOuvert(t);
        t.resoudre();
        return TicketResponse.of(t);
    }

    public TicketResponse cloturer(Long id, String commentaire) {
        TicketSav t = trouver(id);
        if (t.getStatut() == TicketSav.Statut.CLOS) {
            throw BusinessException.conflict("Ticket déjà clôturé.");
        }
        t.cloturer(commentaire);
        return TicketResponse.of(t);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> ouverts() {
        return ticketRepository.findByStatutInOrderByDateOuverture(EnumSet.of(TicketSav.Statut.OUVERT,
                        TicketSav.Statut.EN_COURS, TicketSav.Statut.RESOLU)).stream()
                .filter(t -> currentUser.peutVoirProjet(t.getLivraison().getProjetCode()))
                .map(TicketResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> parLivraison(Long livraisonId) {
        livraisonService.trouver(livraisonId);
        return ticketRepository.findByLivraisonIdOrderByDateOuvertureDesc(livraisonId).stream()
                .map(TicketResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Long id) {
        return TicketResponse.of(trouver(id));
    }

    private TicketSav trouver(Long id) {
        TicketSav t = ticketRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Ticket introuvable : " + id));
        currentUser.verifierAccesProjet(t.getLivraison().getProjetCode());
        return t;
    }

    private static void verifierOuvert(TicketSav t) {
        if (t.getStatut() == TicketSav.Statut.CLOS) {
            throw BusinessException.conflict("Ticket clôturé.");
        }
    }
}
