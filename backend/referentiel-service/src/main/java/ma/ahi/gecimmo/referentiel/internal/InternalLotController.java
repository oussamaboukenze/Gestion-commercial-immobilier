package ma.ahi.gecimmo.referentiel.internal;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import ma.ahi.gecimmo.referentiel.domain.LotStatut;
import ma.ahi.gecimmo.referentiel.service.LotService;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.LotResponse;

/**
 * API interne appelée par vente-service via OpenFeign.
 * Non routée par la gateway : seulement accessible à l'intérieur du réseau des services.
 */
@RestController
@RequestMapping("/internal/lots")
public class InternalLotController {

    public record ChangementStatutRequest(@NotNull LotStatut nouveauStatut, List<LotStatut> statutsAttendus) {
    }

    private final LotService lotService;

    public InternalLotController(LotService lotService) {
        this.lotService = lotService;
    }

    @GetMapping("/{id}")
    public LotResponse lot(@PathVariable Long id) {
        return lotService.lotInterne(id);
    }

    @PostMapping("/{id}/statut")
    public LotResponse changerStatut(@PathVariable Long id, @Valid @RequestBody ChangementStatutRequest req) {
        return lotService.changerStatut(id, req.nouveauStatut(), req.statutsAttendus());
    }
}
