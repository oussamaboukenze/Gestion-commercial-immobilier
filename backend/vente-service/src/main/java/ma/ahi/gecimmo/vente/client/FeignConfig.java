package ma.ahi.gecimmo.vente.client;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Response;
import feign.codec.ErrorDecoder;
import ma.ahi.gecimmo.vente.exception.BusinessException;

/**
 * Transforme les erreurs métier des autres services (404, 409, 422) en BusinessException,
 * en gardant leur message (ex. "Le lot ... n'est plus disponible").
 */
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder feignErrorDecoder(ObjectMapper objectMapper) {
        ErrorDecoder defaut = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            int status = response.status();
            if (status == 404 || status == 409 || status == 422 || status == 400) {
                return new BusinessException(HttpStatus.valueOf(status), lireDetail(objectMapper, response));
            }
            return defaut.decode(methodKey, response);
        };
    }

    private static String lireDetail(ObjectMapper objectMapper, Response response) {
        if (response.body() == null) {
            return "Erreur du service distant (" + response.status() + ")";
        }
        try (InputStream in = response.body().asInputStream()) {
            JsonNode json = objectMapper.readTree(in);
            if (json != null && json.hasNonNull("detail")) {
                return json.get("detail").asText();
            }
        } catch (IOException e) {
            // corps illisible : message générique
        }
        return "Erreur du service distant (" + response.status() + ")";
    }
}
