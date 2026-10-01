package rent_a_car_bryan.carservice.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import rent_a_car_bryan.carservice.client.PexelsSearchResponse.PexelsPhoto;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba el PexelsClient real contra un servidor HTTP local de juguete (com.sun.net.httpserver,
 * que viene con el JDK): no usa internet ni la clave verdadera, y corre en milisegundos.
 *
 * La regla de diseño que se protege aquí está en el comentario del cliente: searchCarPhoto
 * NUNCA lanza excepción; si Pexels falla, devuelve vacío para que crear o editar un auto no
 * dependa de un servicio externo.
 */
class PexelsClientTest {

    private HttpServer server;
    private boolean stopped;

    private final AtomicInteger hits = new AtomicInteger();
    private volatile String lastQuery;
    private volatile String lastAuthorization;

    // Lo que responderá el servidor en el próximo request
    private volatile int status = 200;
    private volatile String body = "{\"photos\":[]}";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/search", exchange -> {
            hits.incrementAndGet();
            lastQuery = exchange.getRequestURI().getQuery();
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (!stopped) {
            server.stop(0);
        }
    }

    // =====================================================================================
    // Sin clave
    // =====================================================================================

    @ParameterizedTest(name = "clave = \"{0}\"")
    @ValueSource(strings = {"", "   "})
    @DisplayName("sin API key devuelve vacío y no hace ninguna llamada de red")
    void withoutApiKeyDoesNotCallPexels(String apiKey) {
        Optional<PexelsPhoto> result = client(apiKey).searchCarPhoto("Toyota", "Yaris", 2023, "Blanco");

        assertThat(result).isEmpty();
        assertThat(hits).hasValue(0);
    }

    // =====================================================================================
    // Request
    // =====================================================================================

    @Test
    @DisplayName("envía la clave en Authorization (sin 'Bearer') y busca una foto horizontal")
    void sendsKeyAndSearchParameters() {
        client("my-key").searchCarPhoto("Toyota", "Yaris", 2023, "Blanco");

        assertThat(lastAuthorization).isEqualTo("my-key");
        assertThat(lastQuery)
                .contains("query=Toyota Yaris 2023 car Blanco")
                .contains("orientation=landscape")
                .contains("per_page=1");
    }

    // =====================================================================================
    // Respuestas
    // =====================================================================================

    @Test
    @DisplayName("devuelve la foto con su fotógrafo y los enlaces de atribución")
    void returnsPhotoWithAttribution() {
        body = """
                {"photos":[{
                  "url":"https://www.pexels.com/photo/1",
                  "photographer":"Ana",
                  "photographer_url":"https://www.pexels.com/@ana",
                  "src":{"landscape":"https://img.pexels.com/landscape.jpg"}
                }]}""";

        Optional<PexelsPhoto> result = client("my-key").searchCarPhoto("Toyota", "Yaris", 2023, "Blanco");

        assertThat(result).isPresent();
        assertThat(result.get().photographer()).isEqualTo("Ana");
        assertThat(result.get().photographerUrl()).isEqualTo("https://www.pexels.com/@ana");
        assertThat(result.get().url()).isEqualTo("https://www.pexels.com/photo/1");
        assertThat(result.get().src().landscape()).isEqualTo("https://img.pexels.com/landscape.jpg");
    }

    @Test
    @DisplayName("descarta las fotos sin versión horizontal y toma la siguiente que sí la tiene")
    void skipsPhotosWithoutLandscape() {
        body = """
                {"photos":[
                  {"url":"u1","photographer":"Sin landscape","src":{"large":"https://img/large.jpg"}},
                  {"url":"u2","photographer":"Sin src"},
                  {"url":"u3","photographer":"Ana","src":{"landscape":"https://img/ok.jpg"}}
                ]}""";

        Optional<PexelsPhoto> result = client("my-key").searchCarPhoto("Toyota", "Yaris", 2023, "Blanco");

        assertThat(result).get().extracting(PexelsPhoto::photographer).isEqualTo("Ana");
    }

    @ParameterizedTest(name = "respuesta: {0}")
    @ValueSource(strings = {"{\"photos\":[]}", "{}", "{\"photos\":null}"})
    @DisplayName("sin resultados devuelve vacío")
    void noResultsReturnsEmpty(String json) {
        body = json;

        assertThat(client("my-key").searchCarPhoto("Toyota", "Yaris", 2023, "Blanco")).isEmpty();
    }

    // =====================================================================================
    // Fallos: nunca lanza excepción
    // =====================================================================================

    @ParameterizedTest(name = "HTTP {0}")
    @ValueSource(ints = {401, 429, 500, 503})
    @DisplayName("si Pexels responde con error (clave inválida, límite, caída), devuelve vacío sin lanzar")
    void httpErrorsReturnEmpty(int errorStatus) {
        status = errorStatus;
        body = "{\"error\":\"nope\"}";

        assertThat(client("my-key").searchCarPhoto("Toyota", "Yaris", 2023, "Blanco")).isEmpty();
    }

    @Test
    @DisplayName("una respuesta 200 sin cuerpo devuelve vacío")
    void emptyBodyReturnsEmpty() {
        body = "";

        assertThat(client("my-key").searchCarPhoto("Toyota", "Yaris", 2023, "Blanco")).isEmpty();
    }

    @Test
    @DisplayName("un JSON inválido devuelve vacío sin lanzar")
    void invalidJsonReturnsEmpty() {
        body = "<html>esto no es json</html>";

        assertThat(client("my-key").searchCarPhoto("Toyota", "Yaris", 2023, "Blanco")).isEmpty();
    }

    @Test
    @DisplayName("si Pexels no es alcanzable (servidor caído), devuelve vacío sin lanzar")
    void unreachableServerReturnsEmpty() {
        PexelsClient client = client("my-key");
        server.stop(0);
        stopped = true;

        assertThat(client.searchCarPhoto("Toyota", "Yaris", 2023, "Blanco")).isEmpty();
    }

    // =====================================================================================

    private PexelsClient client(String apiKey) {
        return new PexelsClient(apiKey, "http://127.0.0.1:" + server.getAddress().getPort());
    }
}