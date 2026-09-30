package rent_a_car_bryan.carservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import rent_a_car_bryan.carservice.security.JwtAuthFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sin token (o vencido) responde 401; con token pero sin permiso, 403
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // Autos eliminados: lista, ficha y reactivar. Estas tres reglas tienen que ir
                        // ANTES de GET /api/cars/**, que es publica: si quedaran despues, esa las
                        // taparia y cualquiera podria listar los autos dados de baja.
                        .requestMatchers(HttpMethod.GET, "/api/cars/deleted").hasRole("ADMIN")
                        // Ficha incluyendo eliminados: ADMIN, o rental-service (SERVICE) al armar el
                        // historial de un arriendo cuyo auto fue dado de baja
                        .requestMatchers(HttpMethod.GET, "/api/cars/admin/*").hasAnyRole("ADMIN", "SERVICE")
                        .requestMatchers(HttpMethod.PATCH, "/api/cars/*/restore").hasRole("ADMIN")
                        // Lecturas públicas a propósito: es el catálogo, se puede ver sin cuenta
                        .requestMatchers(HttpMethod.GET, "/api/cars/**").permitAll()
                        // Cambiar disponibilidad (operativo / en mantencion): solo ADMIN.
                        // rental-service ya no la toca: la disponibilidad por fechas la resuelve el
                        // con sus propios arriendos.
                        .requestMatchers(HttpMethod.PATCH, "/api/cars/*/availability").hasRole("ADMIN")
                        // Crear, editar, eliminar autos y refrescar imágenes: solo ADMIN
                        .anyRequest().hasRole("ADMIN")
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}