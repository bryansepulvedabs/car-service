package rent_a_car_bryan.carservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import rent_a_car_bryan.carservice.client.PexelsSearchResponse.PexelsPhoto;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class PexelsClient {

    private final RestClient restClient;
    private final boolean enabled;

    public PexelsClient(@Value("${pexels.api-key:}") String apiKey,
                        @Value("${pexels.base-url:https://api.pexels.com/v1}") String baseUrl) {
        this.enabled = !apiKey.isBlank();

        HttpClient httpClient;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }}, new java.security.SecureRandom());

            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .sslContext(sslContext)
                    .build();
        } catch (Exception e) {
            log.warn("No se pudo configurar SSL permisivo, usando el por defecto: {}", e.getMessage());
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", apiKey)
                .requestFactory(factory)
                .build();

        if (!enabled) {
            log.warn("PEXELS_API_KEY no configurada: los autos se guardarán sin imagen");
        }
    }

    /**
     * Busca una foto horizontal para la marca y modelo indicados.
     * Nunca lanza excepción: si Pexels falla o no hay resultados, devuelve Optional.empty()
     * para que crear o actualizar un auto no dependa de un servicio externo.
     */
    public Optional<PexelsPhoto> searchCarPhoto(String brand, String model, Integer year) {
        if (!enabled) {
            return Optional.empty();
        }
        String query = brand + " " + model + " " + year + " car";
        try {
            PexelsSearchResponse response = restClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("query", query)
                            .queryParam("orientation", "landscape")
                            .queryParam("per_page", 1)
                            .build())
                    .retrieve()
                    .body(PexelsSearchResponse.class);

            Optional<PexelsPhoto> photo = response == null || response.photos() == null
                    ? Optional.empty()
                    : response.photos().stream()
                    .filter(p -> p.src() != null && p.src().landscape() != null)
                    .findFirst();

            photo.ifPresent(p -> log.info("Pexels OK para '{}': {}", query, p.src().landscape()));

            if (photo.isEmpty()) {
                log.info("Pexels no encontró fotos para '{}'", query);
            }
            return photo;
        } catch (RestClientException e) {
            log.warn("Error al consultar Pexels para '{}': {}", query, e.getMessage());
            return Optional.empty();
        }
    }
}