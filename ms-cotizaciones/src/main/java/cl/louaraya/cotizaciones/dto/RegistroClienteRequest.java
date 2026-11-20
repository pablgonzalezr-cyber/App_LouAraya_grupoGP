package cl.louaraya.cotizaciones.dto;

import jakarta.validation.constraints.*;

public record RegistroClienteRequest(
        @NotBlank @Email @Size(max = 254) String correo,
        @NotBlank @Size(min = 12, max = 72) String clave
) {}
