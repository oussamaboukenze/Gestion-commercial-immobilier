package ma.ahi.gecimmo.vente.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import ma.ahi.gecimmo.vente.domain.LotStatut;

/** Appels synchrones vers referentiel-service (nom résolu par Eureka). */
@FeignClient(name = "referentiel-service", path = "/internal/lots")
public interface ReferentielClient {

    record ChangementStatutRequest(LotStatut nouveauStatut, List<LotStatut> statutsAttendus) {
    }

    @GetMapping("/{id}")
    LotDto getLot(@PathVariable("id") Long id);

    /** Changement de statut atomique : 409 si le lot n'est plus dans un des statuts attendus. */
    @PostMapping("/{id}/statut")
    LotDto changerStatut(@PathVariable("id") Long id, @RequestBody ChangementStatutRequest request);
}
