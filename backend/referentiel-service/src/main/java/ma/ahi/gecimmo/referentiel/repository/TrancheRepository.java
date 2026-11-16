package ma.ahi.gecimmo.referentiel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.referentiel.domain.Tranche;

public interface TrancheRepository extends JpaRepository<Tranche, Long> {
    List<Tranche> findByProjetCodeOrderByCode(String projetCode);
    boolean existsByProjetIdAndCode(Long projetId, String code);
}
