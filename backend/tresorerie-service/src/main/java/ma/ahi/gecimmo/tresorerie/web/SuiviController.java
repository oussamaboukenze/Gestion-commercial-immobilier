package ma.ahi.gecimmo.tresorerie.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import ma.ahi.gecimmo.tresorerie.service.CreditService;
import ma.ahi.gecimmo.tresorerie.service.DocumentService;
import ma.ahi.gecimmo.tresorerie.service.RelanceService;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.AccordCreditRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.CreditRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.CreditResponse;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.DeblocageRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.RelanceResponse;

/** Trésorerie > Relances / Crédits bancaires + Reporting > Exports. */
@RestController
@RequestMapping("/api/tresorerie")
public class SuiviController {

    private final RelanceService relanceService;
    private final CreditService creditService;
    private final DocumentService documentService;

    public SuiviController(RelanceService relanceService, CreditService creditService,
                           DocumentService documentService) {
        this.relanceService = relanceService;
        this.creditService = creditService;
        this.documentService = documentService;
    }

    // ---------- Relances ----------

    @GetMapping("/relances")
    @PreAuthorize("hasAnyRole('COMPTABLE','DAF')")
    public List<RelanceResponse> aTraiter() {
        return relanceService.aTraiter();
    }

    @GetMapping("/dossiers/{reservationId}/relances")
    @PreAuthorize("hasAnyRole('COMPTABLE','DAF','ADV')")
    public List<RelanceResponse> parDossier(@PathVariable Long reservationId) {
        return relanceService.parDossier(reservationId);
    }

    @GetMapping(value = "/relances/{id}/lettre", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAnyRole('COMPTABLE','DAF')")
    public String lettre(@PathVariable Long id) {
        return relanceService.lettre(id);
    }

    /** Mise en demeure (J+30) : validation DAF obligatoire. */
    @PostMapping("/relances/{id}/valider-daf")
    @PreAuthorize("hasRole('DAF')")
    public RelanceResponse validerDaf(@PathVariable Long id) {
        return relanceService.validerMiseEnDemeure(id);
    }

    @PostMapping("/relances/{id}/envoyee")
    @PreAuthorize("hasRole('COMPTABLE')")
    public RelanceResponse envoyee(@PathVariable Long id) {
        return relanceService.marquerEnvoyee(id);
    }

    @PostMapping("/relances/{id}/annuler")
    @PreAuthorize("hasAnyRole('COMPTABLE','DAF')")
    public RelanceResponse annuler(@PathVariable Long id) {
        return relanceService.annuler(id);
    }

    // ---------- Crédits bancaires ----------

    @PostMapping("/credits")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPTABLE')")
    public CreditResponse declarer(@Valid @RequestBody CreditRequest req) {
        return creditService.declarer(req);
    }

    @GetMapping("/credits")
    @PreAuthorize("hasAnyRole('COMPTABLE','DAF','ADV')")
    public List<CreditResponse> enCours() {
        return creditService.enCours();
    }

    @PostMapping("/credits/{id}/decision")
    @PreAuthorize("hasRole('COMPTABLE')")
    public CreditResponse decision(@PathVariable Long id, @Valid @RequestBody AccordCreditRequest req) {
        return creditService.decider(id, req);
    }

    @PostMapping("/credits/{id}/deblocage")
    @PreAuthorize("hasRole('COMPTABLE')")
    public CreditResponse deblocage(@PathVariable Long id, @Valid @RequestBody DeblocageRequest req) {
        return creditService.debloquer(id, req);
    }

    // ---------- Export comptable ----------

    /** Ex. GET /api/tresorerie/exports/sage?du=2026-11-01&au=2026-11-30 (fin de chaque mois). */
    @GetMapping("/exports/sage")
    @PreAuthorize("hasAnyRole('COMPTABLE','DAF')")
    public ResponseEntity<byte[]> exportSage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        byte[] csv = documentService.exportSage(du, au);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"encaissements_" + du + "_" + au + ".csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }
}
