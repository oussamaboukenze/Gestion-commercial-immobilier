package ma.ahi.gecimmo.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Annuaire des microservices : chaque service s'enregistre ici au démarrage,
 * et la gateway / OpenFeign retrouvent les services par leur nom (ex. VENTE-SERVICE).
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
