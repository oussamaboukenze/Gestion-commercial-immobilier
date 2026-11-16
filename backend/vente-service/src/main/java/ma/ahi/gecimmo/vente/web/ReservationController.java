package ma.ahi.gecimmo.vente.web;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import ma.ahi.gecimmo.vente.domain.ReservationStatut;
import ma.ahi.gecimmo.vente.service.ReservationService;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.MotifRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.PiecesRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.ReservationRequest;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.ReservationResponse;

/** Ventes > Réservations. */
@RestController
@RequestMapping("/api/ventes/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** Ventes > Réservations > Nouvelle (commercial) -> "En attente de validation ADV". */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('COMMERCIAL','RESP_COMMERCIAL')")
    public ReservationResponse creer(@Valid @RequestBody ReservationRequest req) {
        return reservationService.creer(req);
    }

    /** Ex. GET /api/ventes/reservations?statut=EN_ATTENTE_VALIDATION_ADV&projet=RNA */
    @GetMapping
    @PreAuthorize("hasAnyRole('COMMERCIAL','RESP_COMMERCIAL','ADV','COMPTABLE','DAF','DIRECTION')")
    public Page<ReservationResponse> rechercher(@RequestParam(required = false) ReservationStatut statut,
                                                @RequestParam(required = false) String projet,
                                                @PageableDefault(size = 20, sort = "dateCreation",
                                                        direction = Sort.Direction.DESC) Pageable pageable) {
        return reservationService.rechercher(statut, projet, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMERCIAL','RESP_COMMERCIAL','ADV','COMPTABLE','DAF','DIRECTION')")
    public ReservationResponse get(@PathVariable Long id) {
        return reservationService.get(id);
    }

    @PutMapping("/{id}/pieces")
    @PreAuthorize("hasAnyRole('COMMERCIAL','RESP_COMMERCIAL','ADV')")
    public ReservationResponse pieces(@PathVariable Long id, @RequestBody PiecesRequest req) {
        return reservationService.majPieces(id, req);
    }

    /** Remise 2-5 % : responsable commercial ; > 5 % : direction. */
    @PostMapping("/{id}/remise/accorder")
    @PreAuthorize("hasAnyRole('RESP_COMMERCIAL','DIRECTION')")
    public ReservationResponse accorderRemise(@PathVariable Long id) {
        return reservationService.deciderRemise(id, true);
    }

    @PostMapping("/{id}/remise/refuser")
    @PreAuthorize("hasAnyRole('RESP_COMMERCIAL','DIRECTION')")
    public ReservationResponse refuserRemise(@PathVariable Long id) {
        return reservationService.deciderRemise(id, false);
    }

    /** ADV : contrôle pièces + avance -> Valider (lot "Réservé"). */
    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('ADV')")
    public ReservationResponse valider(@PathVariable Long id) {
        return reservationService.valider(id);
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('ADV')")
    public ReservationResponse rejeter(@PathVariable Long id, @Valid @RequestBody MotifRequest req) {
        return reservationService.rejeter(id, req.motif());
    }

    @GetMapping("/remises-en-attente")
    @PreAuthorize("hasAnyRole('RESP_COMMERCIAL','DIRECTION')")
    public List<ReservationResponse> remisesEnAttente() {
        return reservationService.remisesEnAttente();
    }

    /** Réservations de plus de 30 jours sans compromis. */
    @GetMapping("/a-regulariser")
    @PreAuthorize("hasAnyRole('ADV','RESP_COMMERCIAL','DIRECTION')")
    public List<ReservationResponse> aRegulariser() {
        return reservationService.aRegulariser();
    }
}
