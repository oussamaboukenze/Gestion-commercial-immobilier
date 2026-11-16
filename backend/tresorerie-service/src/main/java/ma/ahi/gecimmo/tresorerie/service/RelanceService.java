package ma.ahi.gecimmo.tresorerie.service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.tresorerie.domain.Encaissement;
import ma.ahi.gecimmo.tresorerie.domain.RelanceClient;
import ma.ahi.gecimmo.tresorerie.exception.BusinessException;
import ma.ahi.gecimmo.tresorerie.messaging.Events.EcheanceEnRetardEvent;
import ma.ahi.gecimmo.tresorerie.repository.RelanceClientRepository;
import ma.ahi.gecimmo.tresorerie.security.CurrentUser;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.RelanceResponse;

/** Trésorerie > Relances : J+7, J+15, mise en demeure J+30 (validée par le DAF). */
@Service
@Transactional
public class RelanceService {

    private static final Logger log = LoggerFactory.getLogger(RelanceService.class);
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RelanceClientRepository relanceRepository;
    private final CurrentUser currentUser;

    public RelanceService(RelanceClientRepository relanceRepository, CurrentUser currentUser) {
        this.relanceRepository = relanceRepository;
        this.currentUser = currentUser;
    }

    /** Evénement quotidien de vente-service : crée la relance du niveau atteint (une seule fois). */
    public void traiterRetard(EcheanceEnRetardEvent ev) {
        RelanceClient.Niveau niveau = RelanceClient.Niveau.pourRetard(ev.joursRetard());
        if (niveau == null || relanceRepository.existsByEcheanceIdAndNiveau(ev.echeanceId(), niveau)) {
            return;
        }
        relanceRepository.save(new RelanceClient(ev.echeanceId(), ev.reservationId(), ev.reservationNumero(),
                ev.clientNom(), ev.projetCode(), ev.libelle(), ev.resteAPayer(), ev.dateEcheance(), niveau));
        log.info("Relance {} créée pour le dossier {} ({} jours de retard)", niveau, ev.reservationNumero(),
                ev.joursRetard());
    }

    void relanceChequeImpaye(Encaissement e, String motif) {
        relanceRepository.save(new RelanceClient(null, e.getReservationId(), e.getReservationNumero(),
                e.getClientNom(), e.getProjetCode(), "Chèque n° " + e.getNumeroCheque() + " impayé : " + motif,
                e.getMontant(), e.getDateEncaissement(), RelanceClient.Niveau.CHEQUE_IMPAYE));
    }

    @Transactional(readOnly = true)
    public List<RelanceResponse> aTraiter() {
        return relanceRepository.findByStatutInOrderByDateCreation(EnumSet.of(RelanceClient.Statut.A_ENVOYER,
                        RelanceClient.Statut.EN_ATTENTE_VALIDATION_DAF)).stream()
                .filter(r -> currentUser.peutVoirProjet(r.getProjetCode()))
                .map(RelanceResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<RelanceResponse> parDossier(Long reservationId) {
        return relanceRepository.findByReservationIdOrderByDateCreationDesc(reservationId).stream()
                .filter(r -> currentUser.peutVoirProjet(r.getProjetCode()))
                .map(RelanceResponse::of).toList();
    }

    /** Mise en demeure : validation du DAF obligatoire avant envoi. */
    public RelanceResponse validerMiseEnDemeure(Long id) {
        RelanceClient r = trouver(id);
        if (r.getStatut() != RelanceClient.Statut.EN_ATTENTE_VALIDATION_DAF) {
            throw BusinessException.conflict("Cette relance n'attend pas la validation du DAF.");
        }
        r.validerParDaf(currentUser.username());
        return RelanceResponse.of(r);
    }

    public RelanceResponse marquerEnvoyee(Long id) {
        RelanceClient r = trouver(id);
        if (r.getStatut() != RelanceClient.Statut.A_ENVOYER) {
            throw BusinessException.conflict(r.getStatut() == RelanceClient.Statut.EN_ATTENTE_VALIDATION_DAF
                    ? "Mise en demeure non validée par le DAF." : "Cette relance a déjà été traitée.");
        }
        r.marquerEnvoyee(currentUser.username());
        return RelanceResponse.of(r);
    }

    public RelanceResponse annuler(Long id) {
        RelanceClient r = trouver(id);
        r.annuler();
        return RelanceResponse.of(r);
    }

    /** Lettre de relance prête à imprimer (texte). */
    @Transactional(readOnly = true)
    public String lettre(Long id) {
        RelanceClient r = trouver(id);
        if (r.getStatut() == RelanceClient.Statut.EN_ATTENTE_VALIDATION_DAF) {
            throw BusinessException.conflict("Mise en demeure non validée par le DAF.");
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        String titre = switch (r.getNiveau()) {
            case CHEQUE_IMPAYE -> "Objet : chèque impayé";
            case RELANCE_1 -> "Objet : rappel d'échéance";
            case RELANCE_2 -> "Objet : deuxième rappel d'échéance";
            case MISE_EN_DEMEURE -> "Objet : MISE EN DEMEURE";
        };
        return """
                Atlas Horizon Immobilier SARL - Casablanca
                Casablanca, le %s

                %s
                Dossier : %s

                Madame, Monsieur %s,

                Sauf erreur de notre part, la somme de %s DH (%s) restait due au %s.
                Nous vous remercions de bien vouloir régulariser votre situation dans les meilleurs délais
                auprès de notre service trésorerie.

                Si votre règlement a été effectué entre-temps, nous vous prions de ne pas tenir compte de ce courrier.

                Le service Trésorerie
                """.formatted(LocalDate.now().format(DATE_FR), titre, r.getReservationNumero(), r.getClientNom(),
                nf.format(r.getMontantDu()), r.getObjet(),
                r.getDateEcheance() == null ? "-" : r.getDateEcheance().format(DATE_FR));
    }

    private RelanceClient trouver(Long id) {
        RelanceClient r = relanceRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Relance introuvable : " + id));
        currentUser.verifierAccesProjet(r.getProjetCode());
        return r;
    }
}
