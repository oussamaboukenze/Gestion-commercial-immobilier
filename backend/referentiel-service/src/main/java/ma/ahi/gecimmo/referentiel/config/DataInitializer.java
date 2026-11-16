package ma.ahi.gecimmo.referentiel.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.referentiel.domain.Immeuble;
import ma.ahi.gecimmo.referentiel.domain.Lot;
import ma.ahi.gecimmo.referentiel.domain.Projet;
import ma.ahi.gecimmo.referentiel.domain.ProjetStatut;
import ma.ahi.gecimmo.referentiel.domain.Tranche;
import ma.ahi.gecimmo.referentiel.domain.TypeBatiment;
import ma.ahi.gecimmo.referentiel.domain.Typologie;
import ma.ahi.gecimmo.referentiel.repository.ImmeubleRepository;
import ma.ahi.gecimmo.referentiel.repository.LotRepository;
import ma.ahi.gecimmo.referentiel.repository.ProjetRepository;
import ma.ahi.gecimmo.referentiel.repository.TrancheRepository;

/**
 * Données de démonstration (gecimmo.seed=true) : les 4 projets AHI avec quelques lots.
 * Les prix sont des valeurs d'exemple pour tester, pas des prix réels.
 */
@Component
@ConditionalOnProperty(name = "gecimmo.seed", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ProjetRepository projetRepository;
    private final TrancheRepository trancheRepository;
    private final ImmeubleRepository immeubleRepository;
    private final LotRepository lotRepository;

    public DataInitializer(ProjetRepository projetRepository, TrancheRepository trancheRepository,
                           ImmeubleRepository immeubleRepository, LotRepository lotRepository) {
        this.projetRepository = projetRepository;
        this.trancheRepository = trancheRepository;
        this.immeubleRepository = immeubleRepository;
        this.lotRepository = lotRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (projetRepository.count() > 0) {
            return;
        }
        Projet rna = projetRepository.save(new Projet("RNA", "Résidence Nakhil", "Casablanca",
                "Moyen standing", ProjetStatut.COMMERCIALISATION));
        Projet jbk = projetRepository.save(new Projet("JBK", "Les Jardins de Bouskoura", "Bouskoura",
                "Villas + appartements", ProjetStatut.COMMERCIALISATION));
        Projet oce = projetRepository.save(new Projet("OCE", "Océane Business", "Casablanca",
                "Bureaux + commerces", ProjetStatut.LIVRAISON));
        projetRepository.save(new Projet("SAL", "Al Amal (social)", "Had Soualem",
                "Logement social", ProjetStatut.PRE_COMMERCIALISATION));

        // RNA : tranche T1, immeuble IMM03, 3 étages x 4 lots
        Tranche rnaT1 = trancheRepository.save(new Tranche("T1", "Tranche 1", rna));
        Immeuble imm03 = immeubleRepository.save(new Immeuble("IMM03", TypeBatiment.IMMEUBLE, 5, rnaT1));
        Typologie[] typos = {Typologie.F2, Typologie.F3, Typologie.F3, Typologie.F4};
        int[] surfaces = {65, 85, 90, 110};
        for (int etage = 1; etage <= 3; etage++) {
            for (int i = 0; i < 4; i++) {
                BigDecimal prix = BigDecimal.valueOf(surfaces[i] * 12_000L + etage * 10_000L);
                lotRepository.save(new Lot(imm03, "A" + etage + (i + 1), etage, typos[i],
                        BigDecimal.valueOf(surfaces[i]), prix));
            }
        }
        lotRepository.save(new Lot(imm03, "C01", 0, Typologie.COMMERCE, BigDecimal.valueOf(70),
                BigDecimal.valueOf(1_400_000)));

        // JBK : îlot de villas
        Tranche jbkT1 = trancheRepository.save(new Tranche("T1", "Tranche 1", jbk));
        Immeuble ilot = immeubleRepository.save(new Immeuble("IL01", TypeBatiment.ILOT, 0, jbkT1));
        for (int v = 1; v <= 4; v++) {
            lotRepository.save(new Lot(ilot, "V" + v, 0, Typologie.VILLA, BigDecimal.valueOf(250),
                    BigDecimal.valueOf(2_800_000 + v * 50_000L)));
        }

        // OCE : bureaux
        Tranche oceT1 = trancheRepository.save(new Tranche("T1", "Tranche unique", oce));
        Immeuble ocb = immeubleRepository.save(new Immeuble("B01", TypeBatiment.IMMEUBLE, 6, oceT1));
        for (int b = 1; b <= 3; b++) {
            lotRepository.save(new Lot(ocb, "BU" + b, b, Typologie.BUREAU, BigDecimal.valueOf(120),
                    BigDecimal.valueOf(1_900_000)));
        }
        log.info("Données de démonstration chargées : {} lots", lotRepository.count());
    }
}
