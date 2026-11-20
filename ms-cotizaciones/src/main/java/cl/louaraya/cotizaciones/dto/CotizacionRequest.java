package cl.louaraya.cotizaciones.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CotizacionRequest(
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") String tokenEnvio,
        @NotBlank @Size(max = 1000) String descripcion,
        @NotBlank String categoria,
        @NotBlank @Size(max = 100) String material,
        @NotNull @Digits(integer = 5, fraction = 6) @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("10000") BigDecimal anchoCm,
        @NotNull @Digits(integer = 5, fraction = 6) @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("10000") BigDecimal largoCm,
        @NotNull @Min(1) @Max(1000) Integer cantidad,
        LocalDate fechaRequerida,
        @Positive Long productoReferenciaId,
        @Size(max = 150) String productoReferenciaNombre
) {}