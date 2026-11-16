package ma.ahi.gecimmo.crm.internal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ma.ahi.gecimmo.crm.service.ProspectService;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.ProspectResponse;

/** API interne (OpenFeign depuis vente-service), non exposée par la gateway. */
@RestController
@RequestMapping("/internal/prospects")
public class InternalProspectController {

    private final ProspectService prospectService;

    public InternalProspectController(ProspectService prospectService) {
        this.prospectService = prospectService;
    }

    @GetMapping("/{id}")
    public ProspectResponse prospect(@PathVariable Long id) {
        return prospectService.getInterne(id);
    }
}
