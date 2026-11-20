package cl.louaraya.cotizaciones.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Lou Araya - Cotizaciones (academico)", version = "1.0",
        description = "Servicio independiente, H2 propia, puerto 8096. HTTP Basic SOLO para pruebas locales; usar HTTPS y una autenticacion compartida antes de publicar."))
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
public class OpenApiConfig {}
