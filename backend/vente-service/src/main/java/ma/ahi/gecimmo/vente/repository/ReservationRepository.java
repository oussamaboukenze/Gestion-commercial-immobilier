package ma.ahi.gecimmo.vente.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ma.ahi.gecimmo.vente.domain.Reservation;
import ma.ahi.gecimmo.vente.domain.ReservationStatut;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select r from Reservation r
            where (:tousProjets = true or r.projetCode in :projets)
              and (:commercial is null or r.commercial = :commercial)
              and (:statut is null or r.statut = :statut)
            """)
    Page<Reservation> rechercher(@Param("tousProjets") boolean tousProjets,
                                 @Param("projets") Collection<String> projets,
                                 @Param("commercial") String commercial,
                                 @Param("statut") ReservationStatut statut,
                                 Pageable pageable);

    List<Reservation> findByStatutAndStatutRemiseInOrderByDateCreation(ReservationStatut statut,
                                                                     Collection<ma.ahi.gecimmo.vente.domain.StatutRemise> statuts);

    Optional<Reservation> findFirstByLotIdAndStatutIn(Long lotId, Collection<ReservationStatut> statuts);

    /** Réservations validées depuis plus de 30 jours sans compromis. */
    List<Reservation> findByStatutAndDateLimiteCompromisBeforeOrderByDateLimiteCompromis(ReservationStatut statut,
                                                                                         LocalDate date);

    @Query("""
            select r.statut, count(r), coalesce(sum(r.prixNet), cast(0 as BigDecimal)) from Reservation r
            where r.projetCode = :projet group by r.statut
            """)
    List<Object[]> statistiquesParStatut(@Param("projet") String projetCode);

    /** CA réservé : réservations validées dans la période (encore actives). */
    @Query("""
            select coalesce(sum(r.prixNet), cast(0 as BigDecimal)) from Reservation r
            where r.commercial = :commercial
              and r.dateValidation >= :debut and r.dateValidation < :fin
              and r.statut in :statuts
            """)
    BigDecimal caReserve(@Param("commercial") String commercial,
                         @Param("debut") LocalDateTime debut,
                         @Param("fin") LocalDateTime fin,
                         @Param("statuts") Collection<ReservationStatut> statuts);
}
