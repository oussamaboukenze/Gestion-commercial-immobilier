package ma.ahi.gecimmo.vente.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.vente.domain.OptionLot;

public interface OptionLotRepository extends JpaRepository<OptionLot, Long> {

    Optional<OptionLot> findFirstByProspectIdAndStatutAndDateExpirationAfter(Long prospectId, OptionLot.Statut statut,
                                                                           LocalDateTime maintenant);

    Optional<OptionLot> findFirstByLotIdAndStatut(Long lotId, OptionLot.Statut statut);

    List<OptionLot> findByCommercialAndStatutOrderByDateExpiration(String commercial, OptionLot.Statut statut);

    /** Options dépassant 48 h : à expirer par le planificateur. */
    List<OptionLot> findByStatutAndDateExpirationBefore(OptionLot.Statut statut, LocalDateTime maintenant);
}
