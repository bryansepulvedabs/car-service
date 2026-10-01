package rent_a_car_bryan.carservice.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import rent_a_car_bryan.carservice.config.SecurityConfig;
import rent_a_car_bryan.carservice.dto.CarRequestDTO;
import rent_a_car_bryan.carservice.dto.CarResponseDTO;
import rent_a_car_bryan.carservice.exception.InvalidCarOperationException;
import rent_a_car_bryan.carservice.exception.ResourceNotFoundException;
import rent_a_car_bryan.carservice.security.JwtAuthFilter;
import rent_a_car_bryan.carservice.security.JwtValidator;
import rent_a_car_bryan.carservice.service.CarService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Capa web de car-service: reglas de acceso (SecurityConfig + JwtAuthFilter reales), mapeo de las
 * rutas, validación del cuerpo y traducción de errores (GlobalExceptionHandler real).
 * El servicio y el validador de tokens son mocks: no hay base de datos ni claves reales.
 *
 * Si algún import de Spring Boot 4 no resuelve (los paquetes de test cambiaron con la
 * modularización), IntelliJ lo corrige con Alt+Enter; la lógica no cambia.
 */
@WebMvcTest(CarController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class CarControllerWebTest {

    // Asegura que Spring Security esté activo en el slice de test
    @TestConfiguration
    @EnableWebSecurity
    static class EnableSecurity {
    }

    @Autowired private WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter springSecurityFilterChain;

    @MockitoBean private CarService carService;
    @MockitoBean private JwtValidator jwtValidator;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        // Se arma explícitamente con la cadena de seguridad real, para no depender de
        // la autoconfiguración de MockMvc.
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    // =====================================================================================
    // Reglas de acceso
    // =====================================================================================
    @Nested
    @DisplayName("Reglas de acceso")
    class Access {

        private static final String ANONYMOUS = "ANONIMO";

        // Cada fila es una regla de negocio: quién puede llamar a qué.
        private static final List<Endpoint> ENDPOINTS = List.of(
                // Catálogo: público a propósito
                new Endpoint(HttpMethod.GET, "/api/cars", true, Set.of()),
                new Endpoint(HttpMethod.GET, "/api/cars/1", true, Set.of()),
                new Endpoint(HttpMethod.GET, "/api/cars/plate/ABCD12", true, Set.of()),
                // Autos eliminados y su ficha: ADMIN; la ficha también rental-service (SERVICE)
                new Endpoint(HttpMethod.GET, "/api/cars/deleted", false, Set.of("ADMIN")),
                new Endpoint(HttpMethod.GET, "/api/cars/admin/1", false, Set.of("ADMIN", "SERVICE")),
                new Endpoint(HttpMethod.PATCH, "/api/cars/1/restore", false, Set.of("ADMIN")),
                // Kilometraje: ADMIN o rental-service al registrar la devolución
                new Endpoint(HttpMethod.PATCH, "/api/cars/1/mileage?value=12000", false, Set.of("ADMIN", "SERVICE")),
                // Mantención: solo ADMIN (rental-service ya no la toca)
                new Endpoint(HttpMethod.PATCH, "/api/cars/1/availability?available=false", false, Set.of("ADMIN")),
                // Escrituras: solo ADMIN
                new Endpoint(HttpMethod.POST, "/api/cars", false, Set.of("ADMIN")),
                new Endpoint(HttpMethod.PUT, "/api/cars/1", false, Set.of("ADMIN")),
                new Endpoint(HttpMethod.DELETE, "/api/cars/1", false, Set.of("ADMIN")),
                new Endpoint(HttpMethod.POST, "/api/cars/images/refresh", false, Set.of("ADMIN"))
        );

        private static final List<String> ROLES =
                Arrays.asList(ANONYMOUS, "CLIENT", "EMPLOYEE", "SERVICE", "ADMIN");

        static Stream<Arguments> accessMatrix() {
            return ENDPOINTS.stream().flatMap(endpoint ->
                    ROLES.stream().map(role -> Arguments.of(endpoint, role)));
        }

        @ParameterizedTest(name = "{0} · {1}")
        @MethodSource("accessMatrix")
        @DisplayName("quién puede llamar a cada endpoint")
        void whoCanCallWhat(Endpoint endpoint, String role) throws Exception {
            String authorization = ANONYMOUS.equals(role) ? null : tokenFor(role);

            int result = call(endpoint.method(), endpoint.path(), authorization)
                    .andReturn().getResponse().getStatus();

            if (endpoint.isPublic()) {
                assertThat(result).as("endpoint público").isNotIn(401, 403);
            } else if (ANONYMOUS.equals(role)) {
                assertThat(result).as("sin token").isEqualTo(401);
            } else if (endpoint.allowedRoles().contains(role)) {
                assertThat(result).as("rol permitido").isNotIn(401, 403);
            } else {
                assertThat(result).as("rol sin permiso").isEqualTo(403);
            }
        }

        @Test
        @DisplayName("un token inválido en un endpoint protegido responde 401")
        void invalidTokenOnProtectedEndpoint() throws Exception {
            when(jwtValidator.parseClaims("bad")).thenThrow(new JwtException("firma inválida"));

            call(HttpMethod.POST, "/api/cars", "Bearer bad").andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("un token inválido en un endpoint público no impide ver el catálogo")
        void invalidTokenOnPublicEndpoint() throws Exception {
            when(jwtValidator.parseClaims("bad")).thenThrow(new JwtException("firma inválida"));

            call(HttpMethod.GET, "/api/cars", "Bearer bad").andExpect(status().isOk());
        }

        @Test
        @DisplayName("un Authorization que no es 'Bearer …' se trata como sin token")
        void nonBearerHeaderIsIgnored() throws Exception {
            call(HttpMethod.POST, "/api/cars", "Basic abc123").andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("un rol desconocido en el token no da ningún permiso")
        void unknownRoleHasNoPermissions() throws Exception {
            call(HttpMethod.POST, "/api/cars", tokenFor("HACKER")).andExpect(status().isForbidden());
        }

        record Endpoint(HttpMethod method, String path, boolean isPublic, Set<String> allowedRoles) {
            @Override
            public String toString() {
                return method + " " + path;
            }
        }
    }

    // =====================================================================================
    // Mapeo de rutas (con ADMIN)
    // =====================================================================================
    @Nested
    @DisplayName("Rutas")
    class Routes {

        @Test
        @DisplayName("POST /api/cars con datos válidos responde 201 y devuelve el auto creado")
        void createReturns201() throws Exception {
            CarResponseDTO created = new CarResponseDTO();
            created.setId(1L);
            created.setLicensePlate("ABCD12");
            when(carService.save(any(CarRequestDTO.class))).thenReturn(created);

            callWithBody(HttpMethod.POST, "/api/cars", admin(), validCarJson())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.licensePlate").value("ABCD12"));

            ArgumentCaptor<CarRequestDTO> sent = ArgumentCaptor.forClass(CarRequestDTO.class);
            verify(carService).save(sent.capture());
            assertThat(sent.getValue().getBrand()).isEqualTo("Toyota");
            assertThat(sent.getValue().getDailyRate()).isEqualTo(30_000L);
        }

        @Test
        @DisplayName("PATCH …/mileage pasa el valor al servicio")
        void mileageForwardsValue() throws Exception {
            call(HttpMethod.PATCH, "/api/cars/5/mileage?value=12000", admin()).andExpect(status().isOk());

            verify(carService).updateMileage(eq(5L), eq(12_000));
        }

        @Test
        @DisplayName("PATCH …/availability pasa el estado al servicio")
        void availabilityForwardsFlag() throws Exception {
            call(HttpMethod.PATCH, "/api/cars/5/availability?available=false", admin()).andExpect(status().isOk());

            verify(carService).updateAvailability(5L, false);
        }

        @Test
        @DisplayName("DELETE responde 204 sin cuerpo")
        void deleteReturns204() throws Exception {
            call(HttpMethod.DELETE, "/api/cars/5", admin()).andExpect(status().isNoContent());

            verify(carService).deleteById(5L);
        }

        @Test
        @DisplayName("POST /images/refresh devuelve cuántos autos quedaron con imagen")
        void refreshReturnsCount() throws Exception {
            when(carService.refreshMissingImages()).thenReturn(3);

            call(HttpMethod.POST, "/api/cars/images/refresh", admin())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updated").value(3));
        }

        @Test
        @DisplayName("GET /plate/{patente} y GET /deleted llegan al servicio")
        void plateAndDeletedRoutes() throws Exception {
            call(HttpMethod.GET, "/api/cars/plate/ABCD12", null).andExpect(status().isOk());
            call(HttpMethod.GET, "/api/cars/deleted", admin()).andExpect(status().isOk());

            verify(carService).findByLicensePLate("ABCD12");
            verify(carService).findAllDeleted();
        }
    }

    // =====================================================================================
    // Validación del cuerpo
    // =====================================================================================
    @Nested
    @DisplayName("Validación")
    class Validation {

        @Test
        @DisplayName("un cuerpo vacío responde 400 con los mensajes de los campos obligatorios")
        void emptyBody() throws Exception {
            callWithBody(HttpMethod.POST, "/api/cars", admin(), "{}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("La patente es obligatoria")))
                    .andExpect(jsonPath("$.message", containsString("La marca es obligatoria")));
        }

        @ParameterizedTest(name = "{5}")
        @CsvSource({
                "ABC,2023,5,1000,30000,entre 5 y 10 caracteres",
                "ABCD12,1979,5,1000,30000,anterior a 1980",
                "ABCD12,2023,0,1000,30000,al menos 1 asiento",
                "ABCD12,2023,5,-1,30000,no puede ser negativo",
                "ABCD12,2023,5,1000,0,mayor a 0"
        })
        @DisplayName("rechaza valores fuera de rango")
        void outOfRangeValues(String plate, int year, int seats, int mileage, long rate, String expectedMessage)
                throws Exception {
            callWithBody(HttpMethod.POST, "/api/cars", admin(), carJson(plate, year, seats, mileage, rate))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString(expectedMessage)));
        }

        @Test
        @DisplayName("la edición (PUT) valida igual que la creación")
        void putAlsoValidates() throws Exception {
            callWithBody(HttpMethod.PUT, "/api/cars/5", admin(), carJson("ABC", 2023, 5, 1000, 30_000))
                    .andExpect(status().isBadRequest());
        }
    }

    // =====================================================================================
    // Traducción de errores
    // =====================================================================================
    @Nested
    @DisplayName("Errores")
    class Errors {

        @Test
        @DisplayName("recurso inexistente → 404 con el mensaje")
        void notFound() throws Exception {
            when(carService.findById(99L)).thenThrow(new ResourceNotFoundException("Auto no encontrado con id : 99"));

            call(HttpMethod.GET, "/api/cars/99", null)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Auto no encontrado con id : 99"));
        }

        @Test
        @DisplayName("operación inválida (kilometraje que baja) → 400 con el mensaje")
        void invalidOperation() throws Exception {
            when(carService.updateMileage(eq(5L), eq(100)))
                    .thenThrow(new InvalidCarOperationException("El kilometraje no puede ser menor al actual (500 km)"));

            call(HttpMethod.PATCH, "/api/cars/5/mileage?value=100", admin())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("no puede ser menor al actual")));
        }

        @Test
        @DisplayName("patente duplicada → 409 en vez de 500")
        void duplicatePlate() throws Exception {
            when(carService.restore(5L)).thenThrow(new DataIntegrityViolationException("duplicate key"));

            call(HttpMethod.PATCH, "/api/cars/5/restore", admin())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Ya existe un auto con esa patente"));
        }

        @Test
        @DisplayName("un id que no es número → 400 indicando el parámetro")
        void typeMismatch() throws Exception {
            call(HttpMethod.GET, "/api/cars/abc", null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("'id'")));
        }

        @Test
        @DisplayName("falta un parámetro obligatorio → 400 indicando cuál")
        void missingParameter() throws Exception {
            call(HttpMethod.PATCH, "/api/cars/5/availability", admin())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("'available'")));
        }

        @Test
        @DisplayName("JSON mal formado → 400")
        void malformedJson() throws Exception {
            callWithBody(HttpMethod.POST, "/api/cars", admin(), "{")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("no es válido")));
        }

        @Test
        @DisplayName("un error inesperado → 500 genérico, sin filtrar detalles internos")
        void unexpectedErrorDoesNotLeakDetails() throws Exception {
            when(carService.findAll()).thenThrow(new IllegalStateException("detalle secreto de la base"));

            call(HttpMethod.GET, "/api/cars", null)
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                    .andExpect(content().string(not(containsString("detalle secreto"))));
        }
    }

    // =====================================================================================
    // Utilidades
    // =====================================================================================

    // Simula un JWT válido del rol indicado: el validador (mock) devuelve claims con ese rol
    private String tokenFor(String role) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("role", String.class)).thenReturn(role);
        when(jwtValidator.parseClaims("token-" + role)).thenReturn(claims);
        return "Bearer token-" + role;
    }

    private String admin() {
        return tokenFor("ADMIN");
    }

    private ResultActions call(HttpMethod method, String path, String authorization) throws Exception {
        return callWithBody(method, path, authorization, "{}");
    }

    private ResultActions callWithBody(HttpMethod method, String path, String authorization, String json)
            throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(method, path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return mvc.perform(request);
    }

    private static String validCarJson() {
        return carJson("ABCD12", 2023, 5, 10_000, 30_000);
    }

    private static String carJson(String plate, int year, int seats, int mileage, long dailyRate) {
        return """
                {"licensePlate":"%s","brand":"Toyota","model":"Yaris","year":%d,"color":"Blanco",
                 "category":"SEDAN","fuel":"GASOLINA","seats":%d,"mileage":%d,"dailyRate":%d}
                """.formatted(plate, year, seats, mileage, dailyRate);
    }
}