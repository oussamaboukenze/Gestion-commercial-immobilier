package ma.ahi.gecimmo.referentiel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.referentiel.domain.DemandePrix;

public interface DemandePrixRepository extends JpaRepository<DemandePrix, Long> {
    List<DemandePrix> findByStatutOrderByDateDemandeAsc(DemandePrix.Statut statut);
    boolean existsByLotIdAndStatut(Long lotId, DemandePrix.Statut statut);
}
