package ma.ahi.gecimmo.crm.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.crm.domain.Prospect;
import ma.ahi.gecimmo.crm.domain.ProspectStatut;
import ma.ahi.gecimmo.crm.domain.Relance;
import ma.ahi.gecimmo.crm.domain.RendezVous;
import ma.ahi.gecimmo.crm.exception.BusinessException;
import ma.ahi.gecimmo.crm.repository.ProspectRepository;
import ma.ahi.gecimmo.crm.repository.RelanceRepository;
import ma.ahi.gecimmo.crm.repository.RendezVousRepository;
import ma.ahi.gecimmo.crm.security.CurrentUser;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.IndicateursCommercial;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.RelanceRequest;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.RelanceResponse;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.RendezVousRequest;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.RendezVousResponse;

/** CRM > Rendez-vous & visites, Relances, indicateurs du commercial. */
@Service
@Transactional
public class AgendaService {

    private final RendezVousRepository rendezVousRepository;
    private final RelanceRepository relanceRepository;
    private final ProspectRepository prospectRepository;
    private final ProspectService prospectService;
    private final CurrentUser currentUser;

    public AgendaService(RendezVousRepository rendezVousRepository, RelanceRepository relanceRepository,
                         ProspectRepository prospectRepository, ProspectService prospectService,
                         CurrentUser currentUser) {
        this.rendezVousRepository = rendezVousRepository;
        this.relanceRepository = relanceRepository;
        this.prospectRepository = prospectRepository;
        this.prospectService = prospectService;
        this.currentUser = currentUser;
    }

    // ---------- Rendez-vous ----------

    public RendezVousResponse planifier(RendezVousRequest req) {
        Prospect prospect = prospectService.trouverAvecControle(req.prospectId());
        if (prospect.getStatut() == ProspectStatut.NOUVEAU) {
            prospect.setStatut(ProspectStatut.EN_COURS);
        }
        RendezVous rdv = new RendezVous(prospect, req.dateHeure(), req.type(), req.lotId(), currentUser.username());
        return RendezVousResponse.of(rendezVousRepository.save(rdv));
    }

    @Transactional(readOnly = true)
    public List<RendezVousResponse> mesRendezVous(LocalDate du, LocalDate au) {
        return rendezVousRepository.findByCommercialAndDateHeureBetweenOrderByDateHeure(currentUser.username(),
                        du.atStartOfDay(), au.plusDays(1).atStartOfDay()).stream()
                .map(RendezVousResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<RendezVousResponse> historiqueProspect(Long prospectId) {
        prospectService.trouverAvecControle(prospectId);
        return rendezVousRepository.findByProspectIdOrderByDateHeureDesc(prospectId).stream()
                .map(RendezVousResponse::of).toList();
    }

    /** Visites passées depuis plus de 24 h sans compte rendu (règle AHI). */
    @Transactional(readOnly = true)
    public List<RendezVousResponse> comptesRendusEnRetard() {
        LocalDateTime limite = LocalDateTime.now().minus(RendezVous.DELAI_COMPTE_RENDU);
        return rendezVousRepository.findByCommercialAndStatutAndDateHeureBeforeOrderByDateHeure(
                        currentUser.username(), RendezVous.Statut.PLANIFIE, limite).stream()
                .map(RendezVousResponse::of).toList();
    }

    public RendezVousResponse saisirCompteRendu(Long id, String texte) {
        RendezVous rdv = trouverRdv(id);
        if (rdv.getStatut() == RendezVous.Statut.ANNULE) {
            throw new BusinessException("Ce rendez-vous est annulé.");
        }
        if (rdv.getDateHeure().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Le compte rendu se saisit après le rendez-vous.");
        }
        rdv.saisirCompteRendu(texte, LocalDateTime.now());
        return RendezVousResponse.of(rdv);
    }

    public RendezVousResponse annuler(Long id) {
        RendezVous rdv = trouverRdv(id);
        if (rdv.getStatut() == RendezVous.Statut.EFFECTUE) {
            throw new BusinessException("Un rendez-vous effectué ne peut pas être annulé.");
        }
        rdv.annuler();
        return RendezVousResponse.of(rdv);
    }

    private RendezVous trouverRdv(Long id) {
        RendezVous rdv = rendezVousRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Rendez-vous introuvable : " + id));
        prospectService.trouverAvecControle(rdv.getProspect().getId());
        return rdv;
    }

    // ---------- Relances ----------

    public RelanceResponse creerRelance(RelanceRequest req) {
        Prospect prospect = prospectService.trouverAvecControle(req.prospectId());
        Relance relance = new Relance(prospect, req.dateRelance(), req.objet(), currentUser.username());
        return RelanceResponse.of(relanceRepository.save(relance));
    }

    /** CRM > Relances > "Mes relances du jour" (inclut les relances en retard). */
    @Transactional(readOnly = true)
    public List<RelanceResponse> mesRelancesDuJour() {
        return relanceRepository.findByCommercialAndFaiteFalseAndDateRelanceLessThanEqualOrderByDateRelance(
                        currentUser.username(), LocalDate.now()).stream()
                .map(RelanceResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<RelanceResponse> relancesProspect(Long prospectId) {
        prospectService.trouverAvecControle(prospectId);
        return relanceRepository.findByProspectIdOrderByDateRelanceDesc(prospectId).stream()
                .map(RelanceResponse::of).toList();
    }

    public RelanceResponse marquerFaite(Long id, String resultat) {
        Relance relance = relanceRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Relance introuvable : " + id));
        prospectService.trouverAvecControle(relance.getProspect().getId());
        relance.marquerFaite(resultat);
        return RelanceResponse.of(relance);
    }

    // ---------- Indicateurs ----------

    /** Nombre de visites, taux de transformation visite -> réservation (le CA réservé est dans vente-service). */
    @Transactional(readOnly = true)
    public IndicateursCommercial indicateurs(String commercial, YearMonth mois) {
        LocalDateTime debut = mois.atDay(1).atStartOfDay();
        LocalDateTime fin = mois.plusMonths(1).atDay(1).atStartOfDay();
        long visites = rendezVousRepository.countByCommercialAndStatutAndTypeAndDateHeureBetween(commercial,
                RendezVous.Statut.EFFECTUE, RendezVous.Type.VISITE, debut, fin);
        long convertis = prospectRepository.countByCommercialAndStatutAndDateConversionBetween(commercial,
                ProspectStatut.CONVERTI, debut, fin);
        double taux = visites == 0 ? 0.0 : Math.round(convertis * 1000.0 / visites) / 10.0;
        return new IndicateursCommercial(commercial, mois.toString(), visites, convertis, taux);
    }
}
