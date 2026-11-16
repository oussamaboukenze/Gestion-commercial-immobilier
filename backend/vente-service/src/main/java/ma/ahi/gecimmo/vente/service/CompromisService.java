package ma.ahi.gecimmo.vente.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.vente.domain.Compromis;
import ma.ahi.gecimmo.vente.domain.Echeance;
import ma.ahi.gecimmo.vente.domain.LotStatut;
import ma.ahi.gecimmo.vente.domain.RegleVente;
import ma.ahi.gecimmo.vente.domain.Reservation;
import ma.ahi.gecimmo.vente.domain.ReservationStatut;
import ma.ahi.gecimmo.vente.exception.BusinessException;
import ma.ahi.gecimmo.vente.messaging.Events.EcheanceEnRetardEvent;
import ma.ahi.gecimmo.vente.messaging.VenteEventPublisher;
import ma.ahi.gecimmo.vente.repository.CompromisRepository;
import ma.ahi.gecimmo.vente.repository.EcheanceRepository;
import ma.ahi.gecimmo.vente.security.CurrentUser;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.CompromisResponse;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.EcheanceResponse;

/** Ventes > Compromis / Echéanciers. */
@Service
@Transactional
public class CompromisService {

    private final CompromisRepository compromisRepository;
    private final EcheanceRepository echeanceRepository;
    private final ReservationService reservationService;
    private final VenteEventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public CompromisService(CompromisRepository compromisRepository, EcheanceRepository echeanceRepository,
                            ReservationService reservationService, VenteEventPublisher eventPublisher,
                            CurrentUser currentUser) {
        this.compromisRepository = compromisRepository;
        this.echeanceRepository = echeanceRepository;
        this.reservationService = reservationService;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    /** Ventes > Compromis > Nouveau à partir de la réservation -> échéancier généré. */
    public CompromisResponse etablir(Long reservationId) {
        Reservation r = reservationService.trouverAvecControle(reservationId);
        if (r.getStatut() != ReservationStatut.VALIDEE) {
            throw BusinessException.conflict("Le compromis s'établit sur une réservation validée par l'ADV (statut actuel : "
                    + r.getStatut() + ").");
        }
        if (compromisRepository.existsByReservationId(reservationId)) {
            throw BusinessException.conflict("Un compromis existe déjà pour cette réservation.");
        }
        Compromis compromis = new Compromis(r, currentUser.username());
        return CompromisResponse.of(compromisRepository.save(compromis));
    }

    /** Après signature : versement cumulé >= 20 % du prix, lot "Compromis signé". */
    public CompromisResponse signer(Long compromisId) {
        Compromis c = trouverAvecControle(compromisId);
        if (c.getStatut() == Compromis.Statut.SIGNE) {
            throw BusinessException.conflict("Ce compromis est déjà signé.");
        }
        Reservation r = c.getReservation();
        BigDecimal requis = RegleVente.versementMinimalCompromis(r.getPrixNet());
        if (r.getTotalEncaisse().compareTo(requis) < 0) {
            throw new BusinessException("Versement cumulé insuffisant pour signer : " + r.getTotalEncaisse()
                    + " DH encaissés, " + requis + " DH requis (20 % du prix).");
        }
        c.signer(LocalDateTime.now());
        r.setStatut(ReservationStatut.COMPROMIS_SIGNE);
        eventPublisher.lotStatut(r.getLotId(), LotStatut.COMPROMIS_SIGNE, List.of(LotStatut.RESERVE),
                "compromis signé " + r.getNumero());
        return CompromisResponse.of(c);
    }

    @Transactional(readOnly = true)
    public CompromisResponse get(Long id) {
        return CompromisResponse.of(trouverAvecControle(id));
    }

    @Transactional(readOnly = true)
    public CompromisResponse parReservation(Long reservationId) {
        reservationService.trouverAvecControle(reservationId);
        return CompromisResponse.of(compromisRepository.findByReservationId(reservationId)
                .orElseThrow(() -> BusinessException.notFound("Aucun compromis pour cette réservation.")));
    }

    /** L'ADV fixe la date d'exigibilité (ex. constat de fin du gros oeuvre). */
    public EcheanceResponse fixerDate(Long echeanceId, LocalDate date) {
        Echeance e = echeanceRepository.findById(echeanceId)
                .orElseThrow(() -> BusinessException.notFound("Echéance introuvable : " + echeanceId));
        currentUser.verifierAccesProjet(e.getCompromis().getReservation().getProjetCode());
        e.setDateEcheance(date);
        return EcheanceResponse.of(e);
    }

    /** Recalcule la répartition des paiements après un encaissement / impayé. */
    void repartir(Long reservationId) {
        compromisRepository.findByReservationId(reservationId).ifPresent(Compromis::repartirEncaissements);
    }

    /** Planificateur quotidien : signale à la trésorerie les échéances en retard. */
    public int signalerRetards() {
        LocalDate aujourdhui = LocalDate.now();
        int nb = 0;
        for (Compromis c : compromisRepository.findByStatut(Compromis.Statut.SIGNE)) {
            Reservation r = c.getReservation();
            if (r.getStatut() != ReservationStatut.COMPROMIS_SIGNE) {
                continue;
            }
            for (Echeance e : c.getEcheances()) {
                if (e.etat(aujourdhui) == Echeance.Etat.EN_RETARD) {
                    long jours = ChronoUnit.DAYS.between(e.getDateEcheance(), aujourdhui);
                    eventPublisher.echeanceEnRetard(new EcheanceEnRetardEvent(e.getId(), r.getId(), r.getNumero(),
                            r.getClientNom(), r.getProjetCode(), e.getLibelle(), e.getResteAPayer(),
                            e.getDateEcheance(), jours));
                    nb++;
                }
            }
        }
        return nb;
    }

    private Compromis trouverAvecControle(Long id) {
        Compromis c = compromisRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Compromis introuvable : " + id));
        reservationService.trouverAvecControle(c.getReservation().getId());
        return c;
    }
}
