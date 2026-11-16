package ma.ahi.gecimmo.tresorerie.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import ma.ahi.gecimmo.tresorerie.config.TresorerieProperties;
import ma.ahi.gecimmo.tresorerie.domain.Encaissement;
import ma.ahi.gecimmo.tresorerie.domain.ModePaiement;
import ma.ahi.gecimmo.tresorerie.exception.BusinessException;
import ma.ahi.gecimmo.tresorerie.repository.EncaissementRepository;
import ma.ahi.gecimmo.tresorerie.security.CurrentUser;

/** Documents : reçu d'encaissement (HTML imprimable) et export comptable Sage 100 (CSV). */
@Service
@Transactional(readOnly = true)
public class DocumentService {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_SAGE = DateTimeFormatter.ofPattern("ddMMyy");

    private final EncaissementRepository encaissementRepository;
    private final TresorerieProperties properties;
    private final CurrentUser currentUser;

    public DocumentService(EncaissementRepository encaissementRepository, TresorerieProperties properties,
                           CurrentUser currentUser) {
        this.encaissementRepository = encaissementRepository;
        this.properties = properties;
        this.currentUser = currentUser;
    }

    public String recuHtml(Long encaissementId) {
        Encaissement e = encaissementRepository.findById(encaissementId)
                .orElseThrow(() -> BusinessException.notFound("Encaissement introuvable : " + encaissementId));
        currentUser.verifierAccesProjet(e.getProjetCode());
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        String detailMode = e.getMode() == ModePaiement.CHEQUE
                ? "Chèque n° " + e.getNumeroCheque() + " - " + e.getBanque()
                : e.getMode().name();
        return """
                <!DOCTYPE html>
                <html lang="fr"><head><meta charset="UTF-8"><title>Reçu %1$s</title>
                <style>body{font-family:Arial,sans-serif;max-width:640px;margin:40px auto;color:#222}
                h1{font-size:20px;border-bottom:2px solid #222;padding-bottom:8px}
                td{padding:6px 8px}td:first-child{color:#666;width:40%%}
                .montant{font-size:22px;font-weight:bold}.statut{color:#b00}</style></head>
                <body>
                <h1>Atlas Horizon Immobilier SARL<br><small>Reçu d'encaissement n° %1$s</small></h1>
                <table>
                <tr><td>Date</td><td>%2$s</td></tr>
                <tr><td>Client</td><td>%3$s</td></tr>
                <tr><td>Dossier</td><td>%4$s (projet %5$s)</td></tr>
                <tr><td>Mode de paiement</td><td>%6$s</td></tr>
                <tr><td>Montant</td><td class="montant">%7$s DH</td></tr>
                <tr><td>Statut</td><td class="statut">%8$s</td></tr>
                <tr><td>Saisi par</td><td>%9$s</td></tr>
                </table>
                </body></html>
                """.formatted(esc(e.getNumeroRecu()), e.getDateEncaissement().format(DATE_FR), esc(e.getClientNom()),
                esc(e.getReservationNumero()), esc(e.getProjetCode()), esc(detailMode), nf.format(e.getMontant()),
                e.getStatut() == Encaissement.Statut.VALIDE ? "" : e.getStatut().name(), esc(e.getSaisiPar()));
    }

    /**
     * Reporting > Exports > "Encaissements de la période" vers Sage 100.
     * Une écriture = 2 lignes (débit banque/caisse, crédit client). Impayés et contre-passations
     * génèrent l'écriture inverse à leur date. Format CSV ";" (import paramétrable dans Sage).
     */
    public byte[] exportSage(LocalDate debut, LocalDate fin) {
        if (fin.isBefore(debut)) {
            throw new BusinessException("La date de fin doit être après la date de début.");
        }
        TresorerieProperties.Sage sage = properties.sage();
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Journal;Date;Compte;Piece;Libelle;Debit;Credit\n");
        for (Encaissement e : encaissementRepository.mouvementsPeriode(debut, fin)) {
            if (!currentUser.peutVoirProjet(e.getProjetCode())) {
                continue;
            }
            boolean especes = e.getMode() == ModePaiement.ESPECES;
            String journal = especes ? sage.journalCaisse() : sage.journalBanque();
            String compteTreso = especes ? sage.compteCaisse() : sage.compteBanque();
            String libelle = (e.getClientNom() + " " + e.getReservationNumero()).replace(";", " ");
            if (!e.getDateEncaissement().isBefore(debut) && !e.getDateEncaissement().isAfter(fin)) {
                ligne(csv, journal, e.getDateEncaissement(), compteTreso, e.getNumeroRecu(), "Enc. " + libelle,
                        e.getMontant(), null);
                ligne(csv, journal, e.getDateEncaissement(), sage.compteClient(), e.getNumeroRecu(), "Enc. " + libelle,
                        null, e.getMontant());
            }
            if (e.getDateAnnulation() != null && !e.getDateAnnulation().isBefore(debut)
                    && !e.getDateAnnulation().isAfter(fin)) {
                String motif = e.getStatut() == Encaissement.Statut.IMPAYE ? "Impayé " : "Contre-passation ";
                ligne(csv, journal, e.getDateAnnulation(), sage.compteClient(), e.getNumeroRecu(), motif + libelle,
                        e.getMontant(), null);
                ligne(csv, journal, e.getDateAnnulation(), compteTreso, e.getNumeroRecu(), motif + libelle,
                        null, e.getMontant());
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void ligne(StringBuilder csv, String journal, LocalDate date, String compte, String piece,
                              String libelle, BigDecimal debit, BigDecimal credit) {
        csv.append(journal).append(';')
                .append(date.format(DATE_SAGE)).append(';')
                .append(compte).append(';')
                .append(piece).append(';')
                .append(libelle).append(';')
                .append(debit == null ? "" : debit.toPlainString().replace('.', ',')).append(';')
                .append(credit == null ? "" : credit.toPlainString().replace('.', ','))
                .append('\n');
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }
}
