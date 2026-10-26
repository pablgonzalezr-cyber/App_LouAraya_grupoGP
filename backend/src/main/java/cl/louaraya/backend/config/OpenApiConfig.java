package cl.louaraya.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Lou Araya — Backend académico provisional", version = "1.0",
    description = "Servidor propio independiente de Android/Room y de la cátedra. Precio referencial CLP; medidas cm. Errores application/problem+json con status/title/detail y errores por campo para validación. Basic solo para demostración en localhost; no publicar sin HTTPS."))
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic", description = "Cuenta ADMIN definida mediante LOU_ADMIN_USUARIO / LOU_ADMIN_CLAVE en el entorno local. No usar credenciales Android.")
public class OpenApiConfig {}
