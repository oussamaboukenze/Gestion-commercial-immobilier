package ma.ahi.gecimmo.postvente.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.postvente.domain.TicketSav;

public interface TicketSavRepository extends JpaRepository<TicketSav, Long> {
    List<TicketSav> findByStatutInOrderByDateOuverture(Collection<TicketSav.Statut> statuts);
    List<TicketSav> findByLivraisonIdOrderByDateOuvertureDesc(Long livraisonId);
}
