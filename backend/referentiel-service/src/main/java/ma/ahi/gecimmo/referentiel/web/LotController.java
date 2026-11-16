package ma.ahi.gecimmo.referentiel.web;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import ma.ahi.gecimmo.referentiel.domain.LotStatut;
import ma.ahi.gecimmo.referentiel.domain.Typologie;
import ma.ahi.gecimmo.referentiel.service.GrillePrixService;
import ma.ahi.gecimmo.referentiel.service.LotService;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.DemandePrixRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.DemandePrixResponse;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.LotRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.LotResponse;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.LotUpdateRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.StockResponse;

/** Menu Référentiel : Lots / Grilles de prix + Reporting > Stock. */
@RestController
@RequestMapping("/api/referentiel")
public class LotController {

    private final LotService lotService;
    private final GrillePrixService grillePrixService;

    public LotController(LotService lotService, GrillePrixService grillePrixService) {
        this.lotService = lotService;
        this.grillePrixService = grillePrixService;
    }

    /** Ex. GET /api/referentiel/lots?projet=RNA&typologie=F3&statut=DISPONIBLE */
    @GetMapping("/lots")
    public Page<LotResponse> rechercher(@RequestParam(required = false) String projet,
                                        @RequestParam(required = false) Typologie typologie,
                                        @RequestParam(required = false) LotStatut statut,
                                        @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        return lotService.rechercher(projet, typologie, statut, pageable);
    }

    @GetMapping("/lots/{id}")
    public LotResponse lot(@PathVariable Long id) {
        return lotService.getLot(id);
    }

    @GetMapping("/immeubles/{id}/lots")
    public List<LotResponse> lotsImmeuble(@PathVariable Long id) {
        return lotService.listerLotsImmeuble(id);
    }

    @PostMapping("/immeubles/{id}/lots")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_SI')")
    public LotResponse creerLot(@PathVariable Long id, @Valid @RequestBody LotRequest req) {
        return lotService.creerLot(id, req);
    }

    @PutMapping("/lots/{id}")
    @PreAuthorize("hasRole('ADMIN_SI')")
    public LotResponse modifierLot(@PathVariable Long id, @Valid @RequestBody LotUpdateRequest req) {
        return lotService.modifierLot(id, req);
    }

    @GetMapping("/projets/{code}/stock")
    public StockResponse stock(@PathVariable String code) {
        return lotService.etatStock(code);
    }

    // ---------- Grilles de prix ----------

    @PostMapping("/lots/{id}/demandes-prix")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_SI')")
    public DemandePrixResponse demanderPrix(@PathVariable Long id, @Valid @RequestBody DemandePrixRequest req) {
        return grillePrixService.demander(id, req);
    }

    @GetMapping("/demandes-prix")
    @PreAuthorize("hasAnyRole('DIRECTION','ADMIN_SI')")
    public List<DemandePrixResponse> demandesEnAttente() {
        return grillePrixService.enAttente();
    }

    @PostMapping("/demandes-prix/{id}/valider")
    @PreAuthorize("hasRole('DIRECTION')")
    public DemandePrixResponse validerPrix(@PathVariable Long id) {
        return grillePrixService.decider(id, true);
    }

    @PostMapping("/demandes-prix/{id}/refuser")
    @PreAuthorize("hasRole('DIRECTION')")
    public DemandePrixResponse refuserPrix(@PathVariable Long id) {
        return grillePrixService.decider(id, false);
    }
}
