package ma.ahi.gecimmo.vente.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.vente.domain.ReservationStatut;
import ma.ahi.gecimmo.vente.repository.ReservationRepository;
import ma.ahi.gecimmo.vente.security.CurrentUser;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.ChiffresCommercialResponse;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.EtatVentesResponse;
import ma.ahi.gecimmo.vente.web.dto.VenteDtos.LigneEtatVentes;

/** Reporting > Etats de vente / Performance commerciale. */
@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final ReservationRepository reservationRepository;
    private final CurrentUser currentUser;

    public ReportingService(ReservationRepository reservationRepository, CurrentUser currentUser) {
        this.reservationRepository = reservationRepository;
        this.currentUser = currentUser;
    }

    public EtatVentesResponse etatVentes(String projetCode) {
        String code = projetCode.toUpperCase();
        currentUser.verifierAccesProjet(code);
        Map<ReservationStatut, LigneEtatVentes> parStatut = new EnumMap<>(ReservationStatut.class);
        for (ReservationStatut s : ReservationStatut.values()) {
            parStatut.put(s, new LigneEtatVentes(0, BigDecimal.ZERO));
        }
        BigDecimal caActif = BigDecimal.ZERO;
        for (Object[] row : reservationRepository.statistiquesParStatut(code)) {
            ReservationStatut statut = (ReservationStatut) row[0];
            long nombre = ((Number) row[1]).longValue();
            BigDecimal montant = row[2] == null ? BigDecimal.ZERO : new BigDecimal(row[2].toString());
            parStatut.put(statut, new LigneEtatVentes(nombre, montant));
            if (statut == ReservationStatut.VALIDEE || statut == ReservationStatut.COMPROMIS_SIGNE) {
                caActif = caActif.add(montant);
            }
        }
        return new EtatVentesResponse(code, parStatut, caActif);
    }

    /** "CA réservé du mois" d'un commercial (réservations validées par l'ADV et toujours actives). */
    public ChiffresCommercialResponse chiffres(String commercial, YearMonth mois) {
        BigDecimal ca = reservationRepository.caReserve(commercial, mois.atDay(1).atStartOfDay(),
                mois.plusMonths(1).atDay(1).atStartOfDay(),
                EnumSet.of(ReservationStatut.VALIDEE, ReservationStatut.COMPROMIS_SIGNE));
        return new ChiffresCommercialResponse(commercial, mois.toString(), ca);
    }
}
