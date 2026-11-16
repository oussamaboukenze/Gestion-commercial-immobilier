package ma.ahi.gecimmo.postvente.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import ma.ahi.gecimmo.postvente.client.ReferentielClient;
import ma.ahi.gecimmo.postvente.client.ReferentielClient.ChangementStatutRequest;
import ma.ahi.gecimmo.postvente.client.ReservationDto;
import ma.ahi.gecimmo.postvente.client.VenteClient;
import ma.ahi.gecimmo.postvente.domain.DossierNotaire;
import ma.ahi.gecimmo.postvente.domain.Livraison;
import ma.ahi.gecimmo.postvente.exception.BusinessException;
import ma.ahi.gecimmo.postvente.messaging.PostventeEventPublisher;
import ma.ahi.gecimmo.postvente.messaging.PostventeEventPublisher.LotLivreEvent;
import ma.ahi.gecimmo.postvente.repository.LivraisonRepository;
import ma.ahi.gecimmo.postvente.security.CurrentUser;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.LivraisonResponse;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.PlanificationRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.PreReceptionRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.PvRequest;

/** Livraison > Planning / Pré-réception / PV de livraison / Remise des clés. */
@Service
@Transactional
public class LivraisonService {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LivraisonRepository livraisonRepository;
    private final NotaireService notaireService;
    private final VenteClient venteClient;
    private final ReferentielClient referentielClient;
    private final PostventeEventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public LivraisonService(LivraisonRepository livraisonRepository, NotaireService notaireService,
                            VenteClient venteClient, ReferentielClient referentielClient,
                            PostventeEventPublisher eventPublisher, CurrentUser currentUser) {
        this.livraisonRepository = livraisonRepository;
        this.notaireService = notaireService;
        this.venteClient = venteClient;
        this.referentielClient = referentielClient;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    public LivraisonResponse planifier(PlanificationRequest req) {
        ReservationDto r = venteClient.getReservation(req.reservationId());
        currentUser.verifierAccesProjet(r.projetCode());
        DossierNotaire acte = notaireService.acteSignePour(r.id());
        if (livraisonRepository.existsByReservationId(r.id())) {
            throw BusinessException.conflict("Une livraison est déjà planifiée pour " + r.numero()
                    + " : utilisez la replanification.");
        }
        Livraison l = new Livraison(r.id(), r.numero(), acte.getLotId(), acte.getLotCode(), r.projetCode(),
                r.clientNom(), r.clientTelephone(), req.datePrevue());
        return LivraisonResponse.of(livraisonRepository.save(l));
    }

    public LivraisonResponse replanifier(Long id, LocalDate date) {
        Livraison l = trouver(id);
        verifierNonLivree(l);
        l.replanifier(date);
        return LivraisonResponse.of(l);
    }

    public LivraisonResponse preReception(Long id, PreReceptionRequest req) {
        Livraison l = trouver(id);
        verifierNonLivree(l);
        l.preReception(req.date(), req.reserves());
        return LivraisonResponse.of(l);
    }

    public LivraisonResponse leverReserves(Long id) {
        Livraison l = trouver(id);
        l.leverReserves();
        return LivraisonResponse.of(l);
    }

    /** PV de livraison + remise des clés uniquement si solde = 0. Le lot passe "Livré". */
    public LivraisonResponse livrer(Long id, PvRequest req) {
        Livraison l = trouver(id);
        verifierNonLivree(l);
        if (l.getStatut() != Livraison.Statut.PRE_RECEPTION_FAITE) {
            throw new BusinessException("Faites d'abord la pré-réception avec le client.");
        }
        ReservationDto r = venteClient.getReservation(l.getReservationId());
        if (!r.soldeNul()) {
            throw new BusinessException("Remise des clés impossible : solde de " + r.resteAPayer() + " DH non réglé.");
        }
        referentielClient.changerStatut(l.getLotId(), new ChangementStatutRequest("LIVRE", List.of("VENDU")));
        l.livrer(req.dateLivraison(), req.nombreCles(), currentUser.username());
        eventPublisher.lotLivre(new LotLivreEvent(l.getReservationId(), l.getReservationNumero(), l.getLotId(),
                l.getLotCode(), l.getDateLivraison()));
        return LivraisonResponse.of(l);
    }

    @Transactional(readOnly = true)
    public List<LivraisonResponse> planning(LocalDate du, LocalDate au) {
        return livraisonRepository.findByDatePrevueBetweenAndStatutInOrderByDatePrevue(du, au,
                        EnumSet.of(Livraison.Statut.PLANIFIEE, Livraison.Statut.PRE_RECEPTION_FAITE)).stream()
                .filter(l -> currentUser.peutVoirProjet(l.getProjetCode()))
                .map(LivraisonResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<LivraisonResponse> livrees() {
        return livraisonRepository.findByStatutOrderByDateLivraisonDesc(Livraison.Statut.LIVREE).stream()
                .filter(l -> currentUser.peutVoirProjet(l.getProjetCode()))
                .map(LivraisonResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public LivraisonResponse get(Long id) {
        return LivraisonResponse.of(trouver(id));
    }

    /** PV de livraison imprimable. */
    @Transactional(readOnly = true)
    public String pvHtml(Long id) {
        Livraison l = trouver(id);
        if (l.getStatut() != Livraison.Statut.LIVREE) {
            throw new BusinessException("Le PV est disponible après la livraison.");
        }
        return """
                <!DOCTYPE html>
                <html lang="fr"><head><meta charset="UTF-8"><title>PV de livraison %1$s</title>
                <style>body{font-family:Arial,sans-serif;max-width:720px;margin:40px auto;color:#222;line-height:1.5}
                h1{font-size:20px;text-align:center}td{padding:6px 8px}td:first-child{color:#555;width:40%%}
                .sign{display:flex;justify-content:space-between;margin-top:60px}</style></head><body>
                <h1>Atlas Horizon Immobilier SARL<br>PROCÈS-VERBAL DE LIVRAISON</h1>
                <table>
                <tr><td>Dossier</td><td>%1$s</td></tr>
                <tr><td>Client</td><td>%2$s</td></tr>
                <tr><td>Lot</td><td>%3$s (projet %4$s)</td></tr>
                <tr><td>Date de livraison</td><td>%5$s</td></tr>
                <tr><td>Nombre de clés remises</td><td>%6$d</td></tr>
                <tr><td>Réserves de pré-réception</td><td>%7$s</td></tr>
                <tr><td>Fin de la période SAV</td><td>%8$s</td></tr>
                </table>
                <p>Le client reconnaît avoir pris possession du lot ci-dessus et en avoir reçu les clés.</p>
                <div class="sign"><div>Pour AHI<br><br>________________</div><div>Le client<br><br>________________</div></div>
                </body></html>
                """.formatted(esc(l.getReservationNumero()), esc(l.getClientNom()), esc(l.getLotCode()),
                esc(l.getProjetCode()), l.getDateLivraison().format(DATE_FR), l.getNombreCles(),
                l.getReserves() == null || l.getReserves().isBlank() ? "Aucune"
                        : esc(l.getReserves()) + (l.isReservesLevees() ? " (levées)" : " (à lever)"),
                l.getDateLivraison().plusMonths(Livraison.DUREE_SAV_MOIS).format(DATE_FR));
    }

    Livraison trouver(Long id) {
        Livraison l = livraisonRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Livraison introuvable : " + id));
        currentUser.verifierAccesProjet(l.getProjetCode());
        return l;
    }

    private static void verifierNonLivree(Livraison l) {
        if (l.getStatut() == Livraison.Statut.LIVREE) {
            throw BusinessException.conflict("Ce lot est déjà livré.");
        }
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }
}
