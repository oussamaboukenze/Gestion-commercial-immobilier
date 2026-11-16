package ma.ahi.gecimmo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée unique de GECIMMO : React et Flutter appellent uniquement
 * http://localhost:8080/api/... et la gateway redirige vers le bon microservice.
 */
@SpringBootApplication
public class GatewayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
