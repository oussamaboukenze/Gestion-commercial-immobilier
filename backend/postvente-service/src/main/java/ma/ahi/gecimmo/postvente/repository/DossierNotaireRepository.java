package ma.ahi.gecimmo.postvente.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.postvente.domain.DossierNotaire;

public interface DossierNotaireRepository extends JpaRepository<DossierNotaire, Long> {
    Optional<DossierNotaire> findByReservationId(Long reservationId);
    boolean existsByReservationId(Long reservationId);
    List<DossierNotaire> findByStatutInOrderByDateOuvertureDesc(Collection<DossierNotaire.Statut> statuts);
}
