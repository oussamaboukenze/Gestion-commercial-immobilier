package ma.ahi.gecimmo.postvente.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.postvente.domain.Livraison;

public interface LivraisonRepository extends JpaRepository<Livraison, Long> {
    Optional<Livraison> findByReservationId(Long reservationId);
    boolean existsByReservationId(Long reservationId);
    List<Livraison> findByDatePrevueBetweenAndStatutInOrderByDatePrevue(LocalDate du, LocalDate au,
                                                                       Collection<Livraison.Statut> statuts);
    List<Livraison> findByStatutOrderByDateLivraisonDesc(Livraison.Statut statut);
}
