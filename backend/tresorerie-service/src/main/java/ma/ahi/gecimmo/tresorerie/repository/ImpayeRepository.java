package ma.ahi.gecimmo.tresorerie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.tresorerie.domain.Impaye;

public interface ImpayeRepository extends JpaRepository<Impaye, Long> {
    List<Impaye> findAllByOrderByDateRejetDesc();
}
