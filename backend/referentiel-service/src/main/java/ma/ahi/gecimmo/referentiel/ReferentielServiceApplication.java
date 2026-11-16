package ma.ahi.gecimmo.referentiel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/** Référentiel : projets, tranches, immeubles, lots, grilles de prix et statuts des lots. */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ReferentielServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReferentielServiceApplication.class, args);
    }
}
