package ma.ahi.gecimmo.crm.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.crm.domain.RendezVous;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {

    List<RendezVous> findByCommercialAndDateHeureBetweenOrderByDateHeure(String commercial,
                                                                        LocalDateTime debut, LocalDateTime fin);

    List<RendezVous> findByProspectIdOrderByDateHeureDesc(Long prospectId);

    /** Visites passées depuis plus de 24 h sans compte rendu. */
    List<RendezVous> findByCommercialAndStatutAndDateHeureBeforeOrderByDateHeure(String commercial,
                                                                                RendezVous.Statut statut,
                                                                                LocalDateTime limite);

    long countByCommercialAndStatutAndTypeAndDateHeureBetween(String commercial, RendezVous.Statut statut,
                                                              RendezVous.Type type,
                                                              LocalDateTime debut, LocalDateTime fin);
}
