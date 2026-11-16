package ma.ahi.gecimmo.vente.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.vente.domain.Echeance;

public interface EcheanceRepository extends JpaRepository<Echeance, Long> {
}
