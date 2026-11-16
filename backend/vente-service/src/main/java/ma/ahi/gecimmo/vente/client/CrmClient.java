package ma.ahi.gecimmo.vente.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "crm-service", path = "/internal/prospects")
public interface CrmClient {

    @GetMapping("/{id}")
    ProspectDto getProspect(@PathVariable("id") Long id);
}
