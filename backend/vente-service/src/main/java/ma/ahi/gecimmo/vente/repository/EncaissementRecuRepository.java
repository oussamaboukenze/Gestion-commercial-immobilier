package ma.ahi.gecimmo.vente.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.vente.domain.EncaissementRecu;

public interface EncaissementRecuRepository extends JpaRepository<EncaissementRecu, Long> {
    boolean existsByEncaissementIdAndType(Long encaissementId, EncaissementRecu.Type type);
}
