package cl.louaraya.cotizaciones.config;

import cl.louaraya.cotizaciones.repository.ClienteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService usuarios(@Value("${lou.cotizaciones.admin.usuario}") String adminUsuario,
                                @Value("${lou.cotizaciones.admin.clave}") String adminClave,
                                PasswordEncoder encoder, ClienteRepository clientes) {
        if (adminUsuario.isBlank() || adminClave.isBlank() || adminClave.length() < 12
                || adminClave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Configura LOU_COT_ADMIN_USUARIO y LOU_COT_ADMIN_CLAVE");
        }
        final UserDetails admin = User.withUsername(adminUsuario)
                .password(encoder.encode(adminClave)).roles("ADMIN").build();
        return nombre -> {
            if (nombre.equals(adminUsuario)) return User.withUserDetails(admin).build();
            return clientes.findByCorreo(nombre.trim().toLowerCase(Locale.ROOT))
                    .map(c -> User.withUsername(c.getCorreo()).password(c.getClaveHash()).roles("CLIENTE").build())
                    .orElseThrow(() -> new UsernameNotFoundException("Cuenta desconocida"));
        };
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, ObjectMapper mapper) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/api/salud", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/clientes/registro").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/cotizaciones", "/api/cotizaciones/**").hasRole("CLIENTE")
                        .anyRequest().denyAll())
                .httpBasic(b -> b.authenticationEntryPoint((req, res, ex) -> {
                    res.setHeader("WWW-Authenticate", "Basic realm=\"LouAraya cotizaciones local\"");
                    error(res, mapper, 401, "No autorizado", "Credenciales requeridas o incorrectas");
                }))
                .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) ->
                        error(res, mapper, 403, "Prohibido", "Permisos insuficientes")))
                .build();
    }
    private static void error(HttpServletResponse res, ObjectMapper mapper, int status,
                              String titulo, String detalle) throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getOutputStream(), Map.of(
                "type", "about:blank", "status", status, "title", titulo, "detail", detalle));
    }
}
