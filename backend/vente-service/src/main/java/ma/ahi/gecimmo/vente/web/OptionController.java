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
import ma.ahi.gecimmo.vente.service.OptionService;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.OptionRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.OptionResponse;

/** Fiche du lot > Mettre en option. */
@RestController
@RequestMapping("/api/ventes/options")
@PreAuthorize("hasAnyRole('COMMERCIAL','RESP_COMMERCIAL')")
public class OptionController {

    private final OptionService optionService;

    public OptionController(OptionService optionService) {
        this.optionService = optionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OptionResponse poser(@Valid @RequestBody OptionRequest req) {
        return optionService.poser(req.lotId(), req.prospectId());
    }

    @GetMapping("/mes-options")
    public List<OptionResponse> mesOptions() {
        return optionService.mesOptions();
    }

    @PostMapping("/{id}/lever")
    public OptionResponse lever(@PathVariable Long id) {
        return optionService.lever(id);
    }
}
