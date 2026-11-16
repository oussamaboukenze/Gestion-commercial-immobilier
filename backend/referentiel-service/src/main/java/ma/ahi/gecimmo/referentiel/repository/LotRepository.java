package ma.ahi.gecimmo.referentiel.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ma.ahi.gecimmo.referentiel.domain.Lot;
import ma.ahi.gecimmo.referentiel.domain.LotStatut;
import ma.ahi.gecimmo.referentiel.domain.Typologie;

public interface LotRepository extends JpaRepository<Lot, Long> {

    boolean existsByCode(String code);

    /** Recherche de lots (Référentiel > Lots) limitée aux projets autorisés. */
    @Query("""
            select l from Lot l
            where l.projetCode in :projets
              and (:typologie is null or l.typologie = :typologie)
              and (:statut is null or l.statut = :statut)
            """)
    Page<Lot> rechercher(@Param("projets") Collection<String> projets,
                         @Param("typologie") Typologie typologie,
                         @Param("statut") LotStatut statut,
                         Pageable pageable);

    @Query("""
            select l from Lot l
            where (:typologie is null or l.typologie = :typologie)
              and (:statut is null or l.statut = :statut)
            """)
    Page<Lot> rechercherTous(@Param("typologie") Typologie typologie,
                             @Param("statut") LotStatut statut,
                             Pageable pageable);

    List<Lot> findByImmeubleIdOrderByEtageAscNumeroAsc(Long immeubleId);

    /** Etat du stock : nombre de lots par statut pour un projet. */
    @Query("select l.statut, count(l) from Lot l where l.projetCode = :projet group by l.statut")
    List<Object[]> compterParStatut(@Param("projet") String projetCode);
}
