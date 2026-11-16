package ma.ahi.gecimmo.vente.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import ma.ahi.gecimmo.vente.domain.Compromis;
import ma.ahi.gecimmo.vente.domain.Echeance;
import ma.ahi.gecimmo.vente.domain.RegleVente;
import ma.ahi.gecimmo.vente.domain.Reservation;
import ma.ahi.gecimmo.vente.domain.ReservationStatut;
import ma.ahi.gecimmo.vente.exception.BusinessException;
import ma.ahi.gecimmo.vente.repository.CompromisRepository;

/** Documents générés : bon de réservation, compromis + échéancier (HTML imprimable -> PDF via le navigateur). */
@Service
@Transactional(readOnly = true)
public class DocumentVenteService {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String STYLE = """
            <style>body{font-family:Arial,sans-serif;max-width:760px;margin:40px auto;color:#222;line-height:1.5}
            h1{font-size:20px;text-align:center;border-bottom:2px solid #222;padding-bottom:10px}
            table{width:100%;border-collapse:collapse;margin:12px 0}td,th{padding:6px 8px;border:1px solid #ccc;text-align:left}
            th{background:#f2f2f2}.num{text-align:right}.sign{display:flex;justify-content:space-between;margin-top:60px}
            .note{font-size:12px;color:#555}@media print{button{display:none}}</style>""";

    private final ReservationService reservationService;
    private final CompromisRepository compromisRepository;

    public DocumentVenteService(ReservationService reservationService, CompromisRepository compromisRepository) {
        this.reservationService = reservationService;
        this.compromisRepository = compromisRepository;
    }

    public String bonDeReservation(Long reservationId) {
        Reservation r = reservationService.trouverAvecControle(reservationId);
        if (r.getStatut() == ReservationStatut.EN_ATTENTE_VALIDATION_ADV || r.getStatut() == ReservationStatut.REJETEE) {
            throw new BusinessException("Le bon de réservation est généré après la validation ADV.");
        }
        return "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"><title>Bon de réservation "
                + esc(r.getNumero()) + "</title>" + STYLE + "</head><body>"
                + "<h1>Atlas Horizon Immobilier SARL<br>BON DE RÉSERVATION N° " + esc(r.getNumero()) + "</h1>"
                + "<table>"
                + ligne("Client", esc(r.getClientNom()) + " — CIN " + esc(r.getClientCin()))
                + ligne("Téléphone", esc(r.getClientTelephone()))
                + ligne("Projet / lot", esc(r.getProjetCode()) + " — " + esc(r.getLotCode()))
                + ligne("Prix TTC", dh(r.getPrixTTC()))
                + ligne("Remise", r.getRemisePct().toPlainString() + " %")
                + ligne("Prix net TTC", "<b>" + dh(r.getPrixNet()) + "</b>")
                + ligne("Avance versée", dh(r.getTotalEncaisse()))
                + ligne("Date de validation", r.getDateValidation() == null ? "-"
                        : r.getDateValidation().toLocalDate().format(DATE_FR))
                + ligne("Compromis à signer avant le", r.getDateLimiteCompromis() == null ? "-"
                        : r.getDateLimiteCompromis().format(DATE_FR))
                + "</table>"
                + "<p class=\"note\">Réservation valable " + RegleVente.VALIDITE_RESERVATION_JOURS
                + " jours. En cas de désistement avant compromis, l'avance est remboursée déduction faite de "
                + RegleVente.TAUX_FRAIS_DESISTEMENT.toPlainString() + " % de frais de dossier.</p>"
                + signatures() + "</body></html>";
    }

    public String compromisEtEcheancier(Long compromisId) {
        Compromis c = compromisRepository.findById(compromisId)
                .orElseThrow(() -> BusinessException.notFound("Compromis introuvable : " + compromisId));
        Reservation r = reservationService.trouverAvecControle(c.getReservation().getId());
        StringBuilder lignes = new StringBuilder();
        for (Echeance e : c.getEcheances()) {
            lignes.append("<tr><td>").append(e.getOrdre()).append("</td><td>").append(esc(e.getLibelle()))
                    .append("</td><td class=\"num\">").append(e.getPourcentage().stripTrailingZeros().toPlainString())
                    .append(" %</td><td class=\"num\">").append(dh(e.getMontant()))
                    .append("</td><td>").append(e.getDateEcheance() == null ? "à notifier" : e.getDateEcheance().format(DATE_FR))
                    .append("</td><td class=\"num\">").append(dh(e.getMontantPaye())).append("</td></tr>");
        }
        return "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"><title>Compromis "
                + esc(r.getNumero()) + "</title>" + STYLE + "</head><body>"
                + "<h1>Atlas Horizon Immobilier SARL<br>COMPROMIS DE VENTE — " + esc(r.getNumero()) + "</h1>"
                + "<table>"
                + ligne("Acquéreur", esc(r.getClientNom()) + " — CIN " + esc(r.getClientCin()))
                + ligne("Bien", esc(r.getLotCode()) + " (projet " + esc(r.getProjetCode()) + ")")
                + ligne("Prix de vente net TTC", "<b>" + dh(r.getPrixNet()) + "</b>")
                + ligne("Statut", c.getStatut() == Compromis.Statut.SIGNE
                        ? "Signé le " + c.getDateSignature().toLocalDate().format(DATE_FR) : "Projet (non signé)")
                + "</table><h3>Echéancier</h3><table><tr><th>#</th><th>Echéance</th><th>%</th><th>Montant</th>"
                + "<th>Date</th><th>Payé</th></tr>" + lignes + "</table>"
                + "<p class=\"note\">Le solde est payable à la signature de l'acte définitif chez le notaire "
                + "ou par déblocage du crédit bancaire. Document édité le " + LocalDate.now().format(DATE_FR) + ".</p>"
                + signatures() + "</body></html>";
    }

    private static String ligne(String label, String valeur) {
        return "<tr><th style=\"width:35%\">" + label + "</th><td>" + valeur + "</td></tr>";
    }

    private static String signatures() {
        return "<div class=\"sign\"><div>Pour AHI<br><br>________________</div>"
                + "<div>Le client<br><br>________________</div></div>"
                + "<p><button onclick=\"window.print()\">Imprimer / PDF</button></p>";
    }

    private static String dh(BigDecimal montant) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(montant) + " DH";
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }
}
