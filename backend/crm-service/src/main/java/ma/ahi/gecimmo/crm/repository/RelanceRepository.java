package ma.ahi.gecimmo.crm.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ma.ahi.gecimmo.crm.domain.Relance;

public interface RelanceRepository extends JpaRepository<Relance, Long> {

    /** "Mes relances du jour" : relances non faites dont la date est aujourd'hui ou dépassée. */
    List<Relance> findByCommercialAndFaiteFalseAndDateRelanceLessThanEqualOrderByDateRelance(String commercial,
                                                                                            LocalDate date);

    List<Relance> findByProspectIdOrderByDateRelanceDesc(Long prospectId);

    @Modifying
    @Query("update Relance r set r.commercial = :nouveau where r.commercial = :ancien and r.faite = false")
    int reaffecter(@Param("ancien") String ancien, @Param("nouveau") String nouveau);
}
