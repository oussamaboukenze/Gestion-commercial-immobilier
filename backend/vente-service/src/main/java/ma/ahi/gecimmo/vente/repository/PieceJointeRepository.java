package ma.ahi.gecimmo.vente.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.vente.domain.PieceJointe;

public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {
    List<PieceJointe> findByReservationIdOrderByDateDepot(Long reservationId);
    boolean existsByReservationIdAndType(Long reservationId, PieceJointe.Type type);
}
