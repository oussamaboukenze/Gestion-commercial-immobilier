package ma.ahi.gecimmo.tresorerie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Trésorerie : encaissements, remises en banque, impayés, relances clients,
 * crédits bancaires et export comptable vers Sage 100.
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableFeignClients
@ConfigurationPropertiesScan
public class TresorerieServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TresorerieServiceApplication.class, args);
    }
}
