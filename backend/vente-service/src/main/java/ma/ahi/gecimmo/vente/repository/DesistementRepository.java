package ma.ahi.gecimmo.vente.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.vente.domain.Desistement;

public interface DesistementRepository extends JpaRepository<Desistement, Long> {
    List<Desistement> findByStatutInOrderByDateSaisie(Collection<Desistement.Statut> statuts);
    boolean existsByReservationIdAndStatutIn(Long reservationId, Collection<Desistement.Statut> statuts);
    List<Desistement> findByReservationIdOrderByDateSaisieDesc(Long reservationId);
}
