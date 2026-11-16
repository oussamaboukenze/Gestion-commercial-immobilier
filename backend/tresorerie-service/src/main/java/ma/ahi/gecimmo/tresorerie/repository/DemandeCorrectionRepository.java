package ma.ahi.gecimmo.tresorerie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.tresorerie.domain.DemandeCorrection;

public interface DemandeCorrectionRepository extends JpaRepository<DemandeCorrection, Long> {
    List<DemandeCorrection> findByStatutOrderByDateDemande(DemandeCorrection.Statut statut);
    boolean existsByEncaissementIdAndStatut(Long encaissementId, DemandeCorrection.Statut statut);
}
