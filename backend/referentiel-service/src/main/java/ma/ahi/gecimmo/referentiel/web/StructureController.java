package ma.ahi.gecimmo.referentiel.web;

import java.util.List;

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
import ma.ahi.gecimmo.referentiel.domain.Typologie;
import ma.ahi.gecimmo.referentiel.service.StructureService;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.ImmeubleRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.ImmeubleResponse;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.ProjetRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.ProjetResponse;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.TrancheRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.TrancheResponse;

/** Menu Référentiel : Projets / Tranches / Immeubles / Typologies. */
@RestController
@RequestMapping("/api/referentiel")
public class StructureController {

    private final StructureService structureService;

    public StructureController(StructureService structureService) {
        this.structureService = structureService;
    }

    @GetMapping("/projets")
    public List<ProjetResponse> projets() {
        return structureService.listerProjets();
    }

    @GetMapping("/projets/{code}")
    public ProjetResponse projet(@PathVariable String code) {
        return structureService.getProjet(code);
    }

    @PostMapping("/projets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_SI')")
    public ProjetResponse creerProjet(@Valid @RequestBody ProjetRequest req) {
        return structureService.creerProjet(req);
    }

    @PutMapping("/projets/{code}")
    @PreAuthorize("hasRole('ADMIN_SI')")
    public ProjetResponse modifierProjet(@PathVariable String code, @Valid @RequestBody ProjetRequest req) {
        return structureService.modifierProjet(code, req);
    }

    @GetMapping("/projets/{code}/tranches")
    public List<TrancheResponse> tranches(@PathVariable String code) {
        return structureService.listerTranches(code);
    }

    @PostMapping("/projets/{code}/tranches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_SI')")
    public TrancheResponse creerTranche(@PathVariable String code, @Valid @RequestBody TrancheRequest req) {
        return structureService.creerTranche(code, req);
    }

    @GetMapping("/tranches/{id}/immeubles")
    public List<ImmeubleResponse> immeubles(@PathVariable Long id) {
        return structureService.listerImmeubles(id);
    }

    @PostMapping("/tranches/{id}/immeubles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_SI')")
    public ImmeubleResponse creerImmeuble(@PathVariable Long id, @Valid @RequestBody ImmeubleRequest req) {
        return structureService.creerImmeuble(id, req);
    }

    @GetMapping("/typologies")
    public Typologie[] typologies() {
        return Typologie.values();
    }
}
