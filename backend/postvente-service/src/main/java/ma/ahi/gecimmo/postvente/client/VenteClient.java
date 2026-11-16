package ma.ahi.gecimmo.postvente.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "vente-service", path = "/internal/reservations")
public interface VenteClient {

    @GetMapping("/{id}")
    ReservationDto getReservation(@PathVariable("id") Long id);
}
