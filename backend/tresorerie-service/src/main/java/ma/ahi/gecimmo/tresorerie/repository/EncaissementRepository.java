package ma.ahi.gecimmo.tresorerie.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ma.ahi.gecimmo.tresorerie.domain.Encaissement;
import ma.ahi.gecimmo.tresorerie.domain.ModePaiement;

public interface EncaissementRepository extends JpaRepository<Encaissement, Long> {

    List<Encaissement> findByReservationIdOrderByDateEncaissement(Long reservationId);

    @Query("""
            select e from Encaissement e
            where (:tousProjets = true or e.projetCode in :projets)
              and (:debut is null or e.dateEncaissement >= :debut)
              and (:fin is null or e.dateEncaissement <= :fin)
              and (:mode is null or e.mode = :mode)
            """)
    Page<Encaissement> rechercher(@Param("tousProjets") boolean tousProjets,
                                  @Param("projets") Collection<String> projets,
                                  @Param("debut") LocalDate debut,
                                  @Param("fin") LocalDate fin,
                                  @Param("mode") ModePaiement mode,
                                  Pageable pageable);

    /** Chèques encaissés pas encore remis en banque. */
    List<Encaissement> findByModeAndStatutAndRemiseIsNullOrderByDateEncaissement(ModePaiement mode,
                                                                                 Encaissement.Statut statut);

    /** Mouvements de la période pour l'export Sage (encaissements + annulations). */
    @Query("""
            select e from Encaissement e
            where (e.dateEncaissement between :debut and :fin)
               or (e.dateAnnulation between :debut and :fin)
            order by e.dateEncaissement, e.id
            """)
    List<Encaissement> mouvementsPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}
