package ma.ahi.gecimmo.tresorerie.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.tresorerie.domain.CreditBancaire;

public interface CreditBancaireRepository extends JpaRepository<CreditBancaire, Long> {
    List<CreditBancaire> findByStatutInOrderByDateDemande(Collection<CreditBancaire.Statut> statuts);
    List<CreditBancaire> findByReservationId(Long reservationId);
}
