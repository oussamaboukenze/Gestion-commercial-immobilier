package ma.ahi.gecimmo.vente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ventes : options, réservations, compromis, échéanciers, désistements, changements de lot.
 * C'est le coeur du cycle de vente GECIMMO.
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableFeignClients
@EnableScheduling
public class VenteServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VenteServiceApplication.class, args);
    }
}
