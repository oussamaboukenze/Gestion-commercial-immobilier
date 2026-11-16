package ma.ahi.gecimmo.vente.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.vente.domain.Compromis;

public interface CompromisRepository extends JpaRepository<Compromis, Long> {
    Optional<Compromis> findByReservationId(Long reservationId);
    boolean existsByReservationId(Long reservationId);
    List<Compromis> findByStatut(Compromis.Statut statut);
}
