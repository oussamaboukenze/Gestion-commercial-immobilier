package ma.ahi.gecimmo.vente.web;

import java.time.YearMonth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ma.ahi.gecimmo.vente.security.CurrentUser;
import ma.ahi.gecimmo.vente.service.ReportingService;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.ChiffresCommercialResponse;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.EtatVentesResponse;

/** Reporting > Etats de vente / Performance commerciale. */
@RestController
@RequestMapping("/api/ventes/reporting")
public class ReportingController {

    private final ReportingService reportingService;
    private final CurrentUser currentUser;

    public ReportingController(ReportingService reportingService, CurrentUser currentUser) {
        this.reportingService = reportingService;
        this.currentUser = currentUser;
    }

    @GetMapping("/etat-ventes/{projet}")
    @PreAuthorize("hasAnyRole('ADV','RESP_COMMERCIAL','DAF','DIRECTION')")
    public EtatVentesResponse etatVentes(@PathVariable String projet) {
        return reportingService.etatVentes(projet);
    }

    /** Commercial : ses propres chiffres. Ex. GET /api/ventes/reporting/mes-chiffres?mois=2026-11 */
    @GetMapping("/mes-chiffres")
    @PreAuthorize("hasAnyRole('COMMERCIAL','RESP_COMMERCIAL')")
    public ChiffresCommercialResponse mesChiffres(@RequestParam(required = false) String mois) {
        return reportingService.chiffres(currentUser.username(), mois == null ? YearMonth.now() : YearMonth.parse(mois));
    }

    @GetMapping("/commerciaux/{commercial}")
    @PreAuthorize("hasAnyRole('RESP_COMMERCIAL','DIRECTION')")
    public ChiffresCommercialResponse chiffres(@PathVariable String commercial,
                                               @RequestParam(required = false) String mois) {
        return reportingService.chiffres(commercial, mois == null ? YearMonth.now() : YearMonth.parse(mois));
    }
}
