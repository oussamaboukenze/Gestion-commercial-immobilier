package ma.ahi.gecimmo.vente.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ma.ahi.gecimmo.vente.service.ChangementLotService;
import ma.ahi.gecimmo.vente.service.DesistementService;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.ChangementLotRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.ChangementLotResponse;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.DesistementRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.DesistementResponse;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.MotifRequest;

/** Ventes > Désistements / Changements de lot (actions sensibles : validation obligatoire). */
@RestController
@RequestMapping("/api/ventes")
public class DesistementController {

    private final DesistementService desistementService;
    private final ChangementLotService changementLotService;

    public DesistementController(DesistementService desistementService, ChangementLotService changementLotService) {
        this.desistementService = desistementService;
        this.changementLotService = changementLotService;
    }

    // ---------- Désistements ----------

    @PostMapping("/desistements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADV')")
    public DesistementResponse saisir(@Valid @RequestBody DesistementRequest req) {
        return desistementService.saisir(req);
    }

    @GetMapping("/desistements/en-attente")
    @PreAuthorize("hasAnyRole('ADV','RESP_COMMERCIAL','DAF','DIRECTION')")
    public List<DesistementResponse> enAttente() {
        return desistementService.enAttente();
    }

    @GetMapping("/desistements/{id}")
    @PreAuthorize("hasAnyRole('ADV','RESP_COMMERCIAL','DAF','DIRECTION')")
    public DesistementResponse get(@PathVariable Long id) {
        return desistementService.get(id);
    }

    @PostMapping("/desistements/{id}/valider-resp-commercial")
    @PreAuthorize("hasRole('RESP_COMMERCIAL')")
    public DesistementResponse validerResp(@PathVariable Long id) {
        return desistementService.validerRespCommercial(id);
    }

    @PostMapping("/desistements/{id}/valider-daf")
    @PreAuthorize("hasRole('DAF')")
    public DesistementResponse validerDaf(@PathVariable Long id) {
        return desistementService.validerDaf(id);
    }

    @PostMapping("/desistements/{id}/refuser")
    @PreAuthorize("hasAnyRole('RESP_COMMERCIAL','DAF')")
    public DesistementResponse refuser(@PathVariable Long id, @Valid @RequestBody MotifRequest req) {
        return desistementService.refuser(id, req.motif());
    }

    // ---------- Changements de lot ----------

    @PostMapping("/changements-lot")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADV')")
    public ChangementLotResponse demander(@Valid @RequestBody ChangementLotRequest req) {
        return changementLotService.demander(req);
    }

    @GetMapping("/changements-lot/en-attente")
    @PreAuthorize("hasAnyRole('ADV','RESP_COMMERCIAL','DIRECTION')")
    public List<ChangementLotResponse> changementsEnAttente() {
        return changementLotService.enAttente();
    }

    @PostMapping("/changements-lot/{id}/valider")
    @PreAuthorize("hasRole('RESP_COMMERCIAL')")
    public ChangementLotResponse validerChangement(@PathVariable Long id) {
        return changementLotService.valider(id);
    }

    @PostMapping("/changements-lot/{id}/refuser")
    @PreAuthorize("hasRole('RESP_COMMERCIAL')")
    public ChangementLotResponse refuserChangement(@PathVariable Long id) {
        return changementLotService.refuser(id);
    }
}
