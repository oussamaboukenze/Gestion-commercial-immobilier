package ma.ahi.gecimmo.postvente.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ma.ahi.gecimmo.postvente.service.LivraisonService;
import ma.ahi.gecimmo.postvente.service.NotaireService;
import ma.ahi.gecimmo.postvente.service.SavService;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.ActeRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.ClotureRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.DossierNotaireRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.DossierNotaireResponse;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.InterventionRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.LivraisonResponse;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.PlanificationRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.PreReceptionRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.PvRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.ReplanificationRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.TicketRequest;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.TicketResponse;
import ma.ahi.gecimmo.postvente.web.dto.PostventeDtos.TransmissionRequest;

/** Menus Notaire & Actes, Livraison, SAV. */
@RestController
@RequestMapping("/api/postvente")
public class PostventeController {

    private static final String LECTURE = "hasAnyRole('ADV','SAV','COMPTABLE','DAF','DIRECTION','RESP_COMMERCIAL')";

    private final NotaireService notaireService;
    private final LivraisonService livraisonService;
    private final SavService savService;

    public PostventeController(NotaireService notaireService, LivraisonService livraisonService,
                               SavService savService) {
        this.notaireService = notaireService;
        this.livraisonService = livraisonService;
        this.savService = savService;
    }

    // ---------- Notaire & Actes ----------

    @PostMapping("/notaire/dossiers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADV')")
    public DossierNotaireResponse ouvrirDossier(@Valid @RequestBody DossierNotaireRequest req) {
        return notaireService.ouvrir(req);
    }

    @GetMapping("/notaire/dossiers")
    @PreAuthorize(LECTURE)
    public List<DossierNotaireResponse> dossiersEnCours() {
        return notaireService.enCours();
    }

    @GetMapping("/notaire/actes")
    @PreAuthorize(LECTURE)
    public List<DossierNotaireResponse> actesSignes() {
        return notaireService.actesSignes();
    }

    @GetMapping("/notaire/dossiers/{id}")
    @PreAuthorize(LECTURE)
    public DossierNotaireResponse dossier(@PathVariable Long id) {
        return notaireService.get(id);
    }

    @GetMapping("/notaire/reservations/{reservationId}")
    @PreAuthorize(LECTURE)
    public DossierNotaireResponse dossierParReservation(@PathVariable Long reservationId) {
        return notaireService.parReservation(reservationId);
    }

    @PostMapping("/notaire/dossiers/{id}/transmettre")
    @PreAuthorize("hasRole('ADV')")
    public DossierNotaireResponse transmettre(@PathVariable Long id, @Valid @RequestBody TransmissionRequest req) {
        return notaireService.transmettre(id, req);
    }

    @PostMapping("/notaire/dossiers/{id}/acte")
    @PreAuthorize("hasRole('ADV')")
    public DossierNotaireResponse acte(@PathVariable Long id, @Valid @RequestBody ActeRequest req) {
        return notaireService.signerActe(id, req);
    }

    // ---------- Livraison ----------

    @PostMapping("/livraisons")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADV')")
    public LivraisonResponse planifier(@Valid @RequestBody PlanificationRequest req) {
        return livraisonService.planifier(req);
    }

    /** Ex. GET /api/postvente/livraisons/planning?du=2026-11-01&au=2026-12-31 */
    @GetMapping("/livraisons/planning")
    @PreAuthorize(LECTURE)
    public List<LivraisonResponse> planning(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return livraisonService.planning(du, au);
    }

    @GetMapping("/livraisons/livrees")
    @PreAuthorize(LECTURE)
    public List<LivraisonResponse> livrees() {
        return livraisonService.livrees();
    }

    @GetMapping("/livraisons/{id}")
    @PreAuthorize(LECTURE)
    public LivraisonResponse livraison(@PathVariable Long id) {
        return livraisonService.get(id);
    }

    @PutMapping("/livraisons/{id}/date")
    @PreAuthorize("hasRole('ADV')")
    public LivraisonResponse replanifier(@PathVariable Long id, @Valid @RequestBody ReplanificationRequest req) {
        return livraisonService.replanifier(id, req.datePrevue());
    }

    @PostMapping("/livraisons/{id}/pre-reception")
    @PreAuthorize("hasRole('ADV')")
    public LivraisonResponse preReception(@PathVariable Long id, @Valid @RequestBody PreReceptionRequest req) {
        return livraisonService.preReception(id, req);
    }

    @PostMapping("/livraisons/{id}/lever-reserves")
    @PreAuthorize("hasRole('ADV')")
    public LivraisonResponse leverReserves(@PathVariable Long id) {
        return livraisonService.leverReserves(id);
    }

    /** PV de livraison + remise des clés (solde = 0). */
    @PostMapping("/livraisons/{id}/pv")
    @PreAuthorize("hasRole('ADV')")
    public LivraisonResponse livrer(@PathVariable Long id, @Valid @RequestBody PvRequest req) {
        return livraisonService.livrer(id, req);
    }

    @GetMapping(value = "/livraisons/{id}/pv", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize(LECTURE)
    public String pv(@PathVariable Long id) {
        return livraisonService.pvHtml(id);
    }

    // ---------- SAV ----------

    @PostMapping("/sav/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SAV','ADV')")
    public TicketResponse ouvrirTicket(@Valid @RequestBody TicketRequest req) {
        return savService.ouvrir(req);
    }

    @GetMapping("/sav/tickets")
    @PreAuthorize("hasAnyRole('SAV','ADV','DIRECTION')")
    public List<TicketResponse> tickets() {
        return savService.ouverts();
    }

    @GetMapping("/sav/tickets/{id}")
    @PreAuthorize("hasAnyRole('SAV','ADV','DIRECTION')")
    public TicketResponse ticket(@PathVariable Long id) {
        return savService.get(id);
    }

    @GetMapping("/livraisons/{id}/tickets")
    @PreAuthorize("hasAnyRole('SAV','ADV','DIRECTION')")
    public List<TicketResponse> ticketsLivraison(@PathVariable Long id) {
        return savService.parLivraison(id);
    }

    @PostMapping("/sav/tickets/{id}/interventions")
    @PreAuthorize("hasRole('SAV')")
    public TicketResponse intervention(@PathVariable Long id, @Valid @RequestBody InterventionRequest req) {
        return savService.ajouterIntervention(id, req);
    }

    @PostMapping("/sav/tickets/{id}/resoudre")
    @PreAuthorize("hasRole('SAV')")
    public TicketResponse resoudre(@PathVariable Long id) {
        return savService.resoudre(id);
    }

    @PostMapping("/sav/tickets/{id}/cloturer")
    @PreAuthorize("hasRole('SAV')")
    public TicketResponse cloturer(@PathVariable Long id, @RequestBody(required = false) ClotureRequest req) {
        return savService.cloturer(id, req == null ? null : req.commentaire());
    }
}
