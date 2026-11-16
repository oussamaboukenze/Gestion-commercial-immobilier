package ma.ahi.gecimmo.postvente.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Changement de statut du lot : COMPROMIS_SIGNE -> VENDU -> LIVRE. */
@FeignClient(name = "referentiel-service", path = "/internal/lots")
public interface ReferentielClient {

    record ChangementStatutRequest(String nouveauStatut, List<String> statutsAttendus) {
    }

    @PostMapping("/{id}/statut")
    void changerStatut(@PathVariable("id") Long id, @RequestBody ChangementStatutRequest request);
}
