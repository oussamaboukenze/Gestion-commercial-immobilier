package ma.ahi.gecimmo.vente.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.vente.domain.ChangementLot;

public interface ChangementLotRepository extends JpaRepository<ChangementLot, Long> {
    List<ChangementLot> findByStatutOrderByDateSaisie(ChangementLot.Statut statut);
    boolean existsByReservationIdAndStatut(Long reservationId, ChangementLot.Statut statut);
}
