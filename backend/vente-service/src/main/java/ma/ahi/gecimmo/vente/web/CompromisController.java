package ma.ahi.gecimmo.vente.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ma.ahi.gecimmo.vente.service.CompromisService;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.CompromisRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.CompromisResponse;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.DateEcheanceRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.EcheanceResponse;

/** Ventes > Compromis / Echéanciers. */
@RestController
@RequestMapping("/api/ventes")
public class CompromisController {

    private static final String LECTURE = "hasAnyRole('ADV','COMPTABLE','DAF','RESP_COMMERCIAL','DIRECTION','COMMERCIAL')";

    private final CompromisService compromisService;

    public CompromisController(CompromisService compromisService) {
        this.compromisService = compromisService;
    }

    @PostMapping("/compromis")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADV')")
    public CompromisResponse etablir(@Valid @RequestBody CompromisRequest req) {
        return compromisService.etablir(req.reservationId());
    }

    @PostMapping("/compromis/{id}/signer")
    @PreAuthorize("hasRole('ADV')")
    public CompromisResponse signer(@PathVariable Long id) {
        return compromisService.signer(id);
    }

    @GetMapping("/compromis/{id}")
    @PreAuthorize(LECTURE)
    public CompromisResponse get(@PathVariable Long id) {
        return compromisService.get(id);
    }

    /** Echéancier d'un dossier. */
    @GetMapping("/reservations/{id}/echeancier")
    @PreAuthorize(LECTURE)
    public CompromisResponse echeancier(@PathVariable Long id) {
        return compromisService.parReservation(id);
    }

    @PutMapping("/echeances/{id}/date")
    @PreAuthorize("hasRole('ADV')")
    public EcheanceResponse fixerDate(@PathVariable Long id, @Valid @RequestBody DateEcheanceRequest req) {
        return compromisService.fixerDate(id, req.dateEcheance());
    }
}
