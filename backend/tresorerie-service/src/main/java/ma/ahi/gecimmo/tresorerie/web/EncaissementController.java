package ma.ahi.gecimmo.tresorerie.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ma.ahi.gecimmo.tresorerie.domain.ModePaiement;
import ma.ahi.gecimmo.tresorerie.service.DocumentService;
import ma.ahi.gecimmo.tresorerie.service.EncaissementService;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.CorrectionRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.CorrectionResponse;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.EncaissementRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.EncaissementResponse;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.ImpayeRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.ImpayeResponse;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.RemiseRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.RemiseResponse;

/** Trésorerie > Encaissements / Remises en banque / Impayés / Corrections. */
@RestController
@RequestMapping("/api/tresorerie")
public class EncaissementController {

    private static final String LECTURE = "hasAnyRole('COMPTABLE','DAF','ADV','DIRECTION')";

    private final EncaissementService encaissementService;
    private final DocumentService documentService;

    public EncaissementController(EncaissementService encaissementService, DocumentService documentService) {
        this.encaissementService = encaissementService;
        this.documentService = documentService;
    }

    // ---------- Encaissements ----------

    @PostMapping("/encaissements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPTABLE')")
    public EncaissementResponse enregistrer(@Valid @RequestBody EncaissementRequest req) {
        return encaissementService.enregistrer(req);
    }

    @GetMapping("/encaissements")
    @PreAuthorize(LECTURE)
    public Page<EncaissementResponse> rechercher(
            @RequestParam(required = false) String projet,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) ModePaiement mode,
            @PageableDefault(size = 20, sort = "dateEncaissement", direction = Sort.Direction.DESC) Pageable pageable) {
        return encaissementService.rechercher(projet, du, au, mode, pageable);
    }

    @GetMapping("/encaissements/{id}")
    @PreAuthorize(LECTURE)
    public EncaissementResponse get(@PathVariable Long id) {
        return encaissementService.get(id);
    }

    /** Reçu imprimable (ouvrir dans le navigateur puis Ctrl+P). */
    @GetMapping(value = "/encaissements/{id}/recu", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize(LECTURE)
    public String recu(@PathVariable Long id) {
        return documentService.recuHtml(id);
    }

    @GetMapping("/dossiers/{reservationId}/encaissements")
    @PreAuthorize(LECTURE)
    public List<EncaissementResponse> parDossier(@PathVariable Long reservationId) {
        return encaissementService.parDossier(reservationId);
    }

    // ---------- Remises en banque ----------

    @GetMapping("/remises/cheques-a-remettre")
    @PreAuthorize("hasAnyRole('COMPTABLE','DAF')")
    public List<EncaissementResponse> chequesARemettre() {
        return encaissementService.chequesARemettre();
    }

    @PostMapping("/remises")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPTABLE')")
    public RemiseResponse remettre(@Valid @RequestBody RemiseRequest req) {
        return encaissementService.remettreEnBanque(req);
    }

    @GetMapping("/remises")
    @PreAuthorize("hasAnyRole('COMPTABLE','DAF')")
    public List<RemiseResponse> remises() {
        return encaissementService.remises();
    }

    // ---------- Impayés ----------

    @PostMapping("/impayes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPTABLE')")
    public ImpayeResponse declarerImpaye(@Valid @RequestBody ImpayeRequest req) {
        return encaissementService.declarerImpaye(req);
    }

    @GetMapping("/impayes")
    @PreAuthorize(LECTURE)
    public List<ImpayeResponse> impayes() {
        return encaissementService.impayes();
    }

    // ---------- Corrections (contre-passation) ----------

    @PostMapping("/encaissements/{id}/contre-passation")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPTABLE')")
    public CorrectionResponse demanderCorrection(@PathVariable Long id, @Valid @RequestBody CorrectionRequest req) {
        return encaissementService.demanderCorrection(id, req.motif());
    }

    @GetMapping("/corrections/en-attente")
    @PreAuthorize("hasAnyRole('COMPTABLE','DAF')")
    public List<CorrectionResponse> correctionsEnAttente() {
        return encaissementService.correctionsEnAttente();
    }

    @PostMapping("/corrections/{id}/valider")
    @PreAuthorize("hasRole('DAF')")
    public CorrectionResponse validerCorrection(@PathVariable Long id) {
        return encaissementService.deciderCorrection(id, true);
    }

    @PostMapping("/corrections/{id}/refuser")
    @PreAuthorize("hasRole('DAF')")
    public CorrectionResponse refuserCorrection(@PathVariable Long id) {
        return encaissementService.deciderCorrection(id, false);
    }
}
