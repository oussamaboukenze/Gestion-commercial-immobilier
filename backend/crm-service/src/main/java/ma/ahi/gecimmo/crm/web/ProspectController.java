package ma.ahi.gecimmo.crm.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
import ma.ahi.gecimmo.crm.domain.ProspectStatut;
import ma.ahi.gecimmo.crm.service.ProspectService;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.PerteRequest;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.ProspectRequest;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.ProspectResponse;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.ReaffectationRequest;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.ReaffectationResponse;

/** CRM > Prospects. */
@RestController
@RequestMapping("/api/crm/prospects")
public class ProspectController {

    private static final String LECTURE = "hasAnyRole('COMMERCIAL','RESP_COMMERCIAL','ADV','DIRECTION')";
    private static final String ECRITURE = "hasAnyRole('COMMERCIAL','RESP_COMMERCIAL')";

    private final ProspectService prospectService;

    public ProspectController(ProspectService prospectService) {
        this.prospectService = prospectService;
    }

    /** Ex. GET /api/crm/prospects?q=alami&statut=EN_COURS&projet=RNA */
    @GetMapping
    @PreAuthorize(LECTURE)
    public Page<ProspectResponse> rechercher(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) ProspectStatut statut,
                                             @RequestParam(required = false) String projet,
                                             @PageableDefault(size = 20, sort = "dateCreation",
                                                     direction = Sort.Direction.DESC) Pageable pageable) {
        return prospectService.rechercher(q, statut, projet, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(LECTURE)
    public ProspectResponse get(@PathVariable Long id) {
        return prospectService.get(id);
    }

    /** CRM > Prospects > Nouveau */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ECRITURE)
    public ProspectResponse creer(@Valid @RequestBody ProspectRequest req) {
        return prospectService.creer(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ECRITURE)
    public ProspectResponse modifier(@PathVariable Long id, @Valid @RequestBody ProspectRequest req) {
        return prospectService.modifier(id, req);
    }

    @PostMapping("/{id}/perdu")
    @PreAuthorize(ECRITURE)
    public ProspectResponse perdu(@PathVariable Long id, @Valid @RequestBody PerteRequest req) {
        return prospectService.marquerPerdu(id, req.motif());
    }

    /** Départ d'un collaborateur : réaffecter ses prospects (admin SI / responsable commercial). */
    @PostMapping("/reaffectation")
    @PreAuthorize("hasAnyRole('ADMIN_SI','RESP_COMMERCIAL')")
    public ReaffectationResponse reaffecter(@Valid @RequestBody ReaffectationRequest req) {
        return prospectService.reaffecter(req.ancienCommercial(), req.nouveauCommercial());
    }
}
