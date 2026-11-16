package ma.ahi.gecimmo.crm.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ma.ahi.gecimmo.crm.domain.Prospect;
import ma.ahi.gecimmo.crm.domain.ProspectStatut;

public interface ProspectRepository extends JpaRepository<Prospect, Long> {

    /**
     * Recherche multi-critères. commercial = null -> tous les commerciaux ;
     * projets = liste des projets autorisés de l'utilisateur.
     */
    @Query("""
            select p from Prospect p
            where (:tousProjets = true or p.projetCode in :projets)
              and (:commercial is null or p.commercial = :commercial)
              and (:statut is null or p.statut = :statut)
              and (:q is null
                   or lower(p.nom) like lower(concat('%', cast(:q as String), '%'))
                   or lower(p.prenom) like lower(concat('%', cast(:q as String), '%'))
                   or p.telephone like concat('%', cast(:q as String), '%')
                   or upper(p.cin) like upper(concat('%', cast(:q as String), '%')))
            """)
    Page<Prospect> rechercher(@Param("tousProjets") boolean tousProjets,
                              @Param("projets") Collection<String> projets,
                              @Param("commercial") String commercial,
                              @Param("statut") ProspectStatut statut,
                              @Param("q") String q,
                              Pageable pageable);

    boolean existsByCinAndProjetCode(String cin, String projetCode);

    List<Prospect> findByCommercial(String commercial);

    long countByCommercialAndStatutAndDateConversionBetween(String commercial, ProspectStatut statut,
                                                            LocalDateTime debut, LocalDateTime fin);

    /** Départ d'un collaborateur : réaffecter tous ses prospects. */
    @Modifying
    @Query("update Prospect p set p.commercial = :nouveau where p.commercial = :ancien")
    int reaffecter(@Param("ancien") String ancien, @Param("nouveau") String nouveau);
}
