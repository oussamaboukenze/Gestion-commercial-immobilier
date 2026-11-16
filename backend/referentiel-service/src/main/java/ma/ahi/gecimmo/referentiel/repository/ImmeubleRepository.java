package ma.ahi.gecimmo.referentiel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.referentiel.domain.Immeuble;

public interface ImmeubleRepository extends JpaRepository<Immeuble, Long> {
    List<Immeuble> findByTrancheIdOrderByCode(Long trancheId);
    boolean existsByTrancheIdAndCode(Long trancheId, String code);
}
