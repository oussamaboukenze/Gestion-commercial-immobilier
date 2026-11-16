package ma.ahi.gecimmo.referentiel.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.referentiel.domain.Projet;

public interface ProjetRepository extends JpaRepository<Projet, Long> {
    Optional<Projet> findByCode(String code);
    boolean existsByCode(String code);
    List<Projet> findByCodeInOrderByCode(Collection<String> codes);
    List<Projet> findAllByOrderByCode();
}
