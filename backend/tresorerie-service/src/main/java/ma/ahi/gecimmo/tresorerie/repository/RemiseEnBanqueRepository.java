package ma.ahi.gecimmo.tresorerie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.tresorerie.domain.RemiseEnBanque;

public interface RemiseEnBanqueRepository extends JpaRepository<RemiseEnBanque, Long> {
    List<RemiseEnBanque> findAllByOrderByDateRemiseDesc();
}
