package ma.ahi.gecimmo.tresorerie.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.tresorerie.domain.RelanceClient;

public interface RelanceClientRepository extends JpaRepository<RelanceClient, Long> {
    boolean existsByEcheanceIdAndNiveau(Long echeanceId, RelanceClient.Niveau niveau);
    List<RelanceClient> findByStatutInOrderByDateCreation(Collection<RelanceClient.Statut> statuts);
    List<RelanceClient> findByReservationIdOrderByDateCreationDesc(Long reservationId);
}
