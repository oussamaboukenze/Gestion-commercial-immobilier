package ma.ahi.gecimmo.vente.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ma.ahi.gecimmo.vente.service.CompromisService;
import ma.ahi.gecimmo.vente.service.OptionService;

/** Tâches automatiques du cycle de vente. */
@Component
public class VenteScheduler {

    private static final Logger log = LoggerFactory.getLogger(VenteScheduler.class);

    private final OptionService optionService;
    private final CompromisService compromisService;

    public VenteScheduler(OptionService optionService, CompromisService compromisService) {
        this.optionService = optionService;
        this.compromisService = compromisService;
    }

    /** Toutes les 5 minutes : options de plus de 48 h -> lot "Disponible". */
    @Scheduled(fixedDelayString = "${gecimmo.scheduler.options-ms:300000}", initialDelay = 30000)
    public void expirerOptions() {
        int nb = optionService.expirerOptions();
        if (nb > 0) {
            log.info("{} option(s) expirée(s)", nb);
        }
    }

    /** Chaque jour à 6 h : échéances en retard envoyées à la trésorerie (relances). */
    @Scheduled(cron = "${gecimmo.scheduler.retards-cron:0 0 6 * * *}")
    public void signalerRetards() {
        int nb = compromisService.signalerRetards();
        log.info("{} échéance(s) en retard signalée(s) à la trésorerie", nb);
    }
}
