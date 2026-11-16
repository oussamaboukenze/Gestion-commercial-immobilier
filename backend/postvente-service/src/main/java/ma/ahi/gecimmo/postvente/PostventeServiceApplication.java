package ma.ahi.gecimmo.postvente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Après-vente : Notaire & Actes, Livraison (planning, pré-réception, PV, clés) et SAV (tickets, interventions).
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableFeignClients
public class PostventeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PostventeServiceApplication.class, args);
    }
}
