package cl.louaraya.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import java.io.IOException;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean
    UserDetailsService usuarios(@Value("${lou.security.usuario}") String usuario, @Value("${lou.security.clave}") String clave, PasswordEncoder encoder) {
        if(usuario.isBlank() || clave.isBlank() || clave.length() < 12 || clave.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Configura LOU_ADMIN_USUARIO y LOU_ADMIN_CLAVE (12 a 72 caracteres)");
        return new InMemoryUserDetailsManager(User.withUsername(usuario).password(encoder.encode(clave)).roles("ADMIN").build());
    }
    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, ObjectMapper mapper) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/*", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/colecciones", "/api/colecciones/*", "/api/campanias", "/api/campanias/*").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/error").permitAll().anyRequest().denyAll())
            .httpBasic(b -> b.authenticationEntryPoint((req, res, ex) -> {
                res.setHeader("WWW-Authenticate", "Basic realm=\"LouAraya local\"");
                error(res, mapper, 401, "No autorizado", "Credenciales administrativas requeridas o incorrectas");
            }))
            .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) -> error(res, mapper, 403, "Prohibido", "Rol administrativo requerido")))
            .build();
    }
    private static void error(HttpServletResponse res, ObjectMapper mapper, int estado, String titulo, String detalle) throws IOException {
        res.setStatus(estado); res.setContentType("application/problem+json"); res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getOutputStream(), Map.of("type", "about:blank", "status", estado, "title", titulo, "detail", detalle));
    }
}
