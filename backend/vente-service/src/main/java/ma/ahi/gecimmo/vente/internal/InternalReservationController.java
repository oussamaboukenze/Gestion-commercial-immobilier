package ma.ahi.gecimmo.vente.internal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ma.ahi.gecimmo.vente.service.ReservationService;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.ReservationResponse;

/** API interne pour tresorerie-service (vérifier un dossier avant un encaissement). */
@RestController
@RequestMapping("/internal/reservations")
public class InternalReservationController {

    private final ReservationService reservationService;

    public InternalReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/{id}")
    public ReservationResponse reservation(@PathVariable Long id) {
        return reservationService.getInterne(id);
    }
}
