package ma.ahi.gecimmo.crm.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import ma.ahi.gecimmo.crm.security.CurrentUser;
import ma.ahi.gecimmo.crm.service.AgendaService;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.CompteRenduRequest;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.IndicateursCommercial;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.RelanceFaiteRequest;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.RelanceRequest;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.RelanceResponse;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.RendezVousRequest;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.RendezVousResponse;

/** CRM > Rendez-vous & visites, Relances, indicateurs. */
@RestController
@RequestMapping("/api/crm")
@PreAuthorize("hasAnyRole('COMMERCIAL','RESP_COMMERCIAL')")
public class AgendaController {

    private final AgendaService agendaService;
    private final CurrentUser currentUser;

    public AgendaController(AgendaService agendaService, CurrentUser currentUser) {
        this.agendaService = agendaService;
        this.currentUser = currentUser;
    }

    // ---------- Rendez-vous ----------

    @PostMapping("/rendez-vous")
    @ResponseStatus(HttpStatus.CREATED)
    public RendezVousResponse planifier(@Valid @RequestBody RendezVousRequest req) {
        return agendaService.planifier(req);
    }

    /** Ex. GET /api/crm/rendez-vous?du=2026-11-16&au=2026-11-22 */
    @GetMapping("/rendez-vous")
    public List<RendezVousResponse> mesRendezVous(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return agendaService.mesRendezVous(du, au);
    }

    @GetMapping("/rendez-vous/comptes-rendus-en-retard")
    public List<RendezVousResponse> comptesRendusEnRetard() {
        return agendaService.comptesRendusEnRetard();
    }

    @GetMapping("/prospects/{id}/rendez-vous")
    public List<RendezVousResponse> historique(@PathVariable Long id) {
        return agendaService.historiqueProspect(id);
    }

    @PostMapping("/rendez-vous/{id}/compte-rendu")
    public RendezVousResponse compteRendu(@PathVariable Long id, @Valid @RequestBody CompteRenduRequest req) {
        return agendaService.saisirCompteRendu(id, req.compteRendu());
    }

    @PostMapping("/rendez-vous/{id}/annuler")
    public RendezVousResponse annuler(@PathVariable Long id) {
        return agendaService.annuler(id);
    }

    // ---------- Relances ----------

    @PostMapping("/relances")
    @ResponseStatus(HttpStatus.CREATED)
    public RelanceResponse creerRelance(@Valid @RequestBody RelanceRequest req) {
        return agendaService.creerRelance(req);
    }

    /** CRM > Relances > "Mes relances du jour" */
    @GetMapping("/relances/du-jour")
    public List<RelanceResponse> mesRelancesDuJour() {
        return agendaService.mesRelancesDuJour();
    }

    @GetMapping("/prospects/{id}/relances")
    public List<RelanceResponse> relancesProspect(@PathVariable Long id) {
        return agendaService.relancesProspect(id);
    }

    @PostMapping("/relances/{id}/faite")
    public RelanceResponse faite(@PathVariable Long id, @RequestBody(required = false) RelanceFaiteRequest req) {
        return agendaService.marquerFaite(id, req == null ? null : req.resultat());
    }

    // ---------- Indicateurs ----------

    /** Ex. GET /api/crm/indicateurs/moi?mois=2026-11 */
    @GetMapping("/indicateurs/moi")
    public IndicateursCommercial mesIndicateurs(@RequestParam(required = false) String mois) {
        YearMonth ym = mois == null ? YearMonth.now() : YearMonth.parse(mois);
        return agendaService.indicateurs(currentUser.username(), ym);
    }

    /** Responsable commercial : indicateurs d'un commercial de son équipe. */
    @GetMapping("/indicateurs/{commercial}")
    @PreAuthorize("hasAnyRole('RESP_COMMERCIAL','DIRECTION')")
    public IndicateursCommercial indicateurs(@PathVariable String commercial,
                                             @RequestParam(required = false) String mois) {
        YearMonth ym = mois == null ? YearMonth.now() : YearMonth.parse(mois);
        return agendaService.indicateurs(commercial, ym);
    }
}
