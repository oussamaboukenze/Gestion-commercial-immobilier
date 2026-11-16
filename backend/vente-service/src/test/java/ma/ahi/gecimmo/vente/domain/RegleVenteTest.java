package ma.ahi.gecimmo.vente.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests des règles AHI (sans Spring : rapides et lisibles). */
class RegleVenteTest {

    private static BigDecimal dh(String montant) {
        return new BigDecimal(montant);
    }

    @Test
    void prixNetAvecRemise() {
        assertEquals(dh("980000.00"), RegleVente.prixNet(dh("1000000"), dh("2")));
        assertEquals(dh("1000000.00"), RegleVente.prixNet(dh("1000000"), null));
    }

    @Test
    void avanceMinimaleEstDe5Pourcent() {
        assertEquals(dh("50000.00"), RegleVente.avanceMinimale(dh("1000000")));
    }

    @Test
    void versementCompromisEstDe20Pourcent() {
        assertEquals(dh("200000.00"), RegleVente.versementMinimalCompromis(dh("1000000")));
    }

    @Test
    void matriceDeValidationDesRemises() {
        assertEquals(StatutRemise.NON_REQUISE, RegleVente.statutRemiseInitial(BigDecimal.ZERO));
        assertEquals(StatutRemise.NON_REQUISE, RegleVente.statutRemiseInitial(dh("2")));
        assertEquals(StatutRemise.EN_ATTENTE_RESP_COMMERCIAL, RegleVente.statutRemiseInitial(dh("2.5")));
        assertEquals(StatutRemise.EN_ATTENTE_RESP_COMMERCIAL, RegleVente.statutRemiseInitial(dh("5")));
        assertEquals(StatutRemise.EN_ATTENTE_DIRECTION, RegleVente.statutRemiseInitial(dh("5.01")));
        assertThrows(IllegalArgumentException.class, () -> RegleVente.statutRemiseInitial(dh("-1")));
    }

    @Test
    void echeancierStandard20_30_50() {
        List<BigDecimal> montants = RegleVente.montantsEcheancier(dh("1000000"));
        assertEquals(List.of(dh("200000.00"), dh("300000.00"), dh("500000.00")), montants);
    }

    @Test
    void echeancierAbsorbeLArrondiSurLaDerniereEcheance() {
        List<BigDecimal> montants = RegleVente.montantsEcheancier(dh("999999.99"));
        BigDecimal total = montants.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(dh("999999.99"), total);
    }

    @Test
    void repartitionDesPaiementsDansLOrdre() {
        List<BigDecimal> payes = RegleVente.repartir(dh("250000"),
                List.of(dh("200000"), dh("300000"), dh("500000")));
        assertEquals(List.of(dh("200000.00"), dh("50000.00"), dh("0.00")), payes);
    }

    @Test
    void fraisDeDossierDesistement10Pourcent() {
        assertEquals(dh("5000.00"), RegleVente.fraisDossierDesistement(dh("50000")));
    }
}
