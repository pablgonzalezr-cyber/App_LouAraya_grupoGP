package cl.louaraya.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Producto completo. Disponible indica stock/capacidad; publicado controla su visibilidad pública.")
public record ProductoRequest(
    @NotBlank @Size(max = 100) @Schema(example = "Tapiz Bohemio") String nombre,
    @NotBlank @Size(max = 2000) @Schema(example = "Tapiz ornamental tejido a mano.") String descripcion,
    @NotBlank @Size(max = 100) @Schema(example = "Interiores") String categoria,
    @NotBlank @Size(max = 100) @Schema(example = "Algodón natural") String material,
    @NotNull @DecimalMin("0.01") @DecimalMax("10000") @Digits(integer = 5, fraction = 2) @Schema(example = "45", description = "Ancho en centímetros") BigDecimal anchoCm,
    @NotNull @DecimalMin("0.01") @DecimalMax("10000") @Digits(integer = 5, fraction = 2) @Schema(example = "70", description = "Alto/largo en centímetros; equivale a largoCm en Android") BigDecimal altoCm,
    @NotNull @DecimalMin("0.01") @Digits(integer = 9, fraction = 2) @Schema(example = "24990", description = "Precio referencial en CLP, sin descuentos automáticos") BigDecimal precio,
    @NotNull @Schema(example = "true", description = "Puede estar publicado aunque no esté disponible") Boolean disponible,
    @NotNull @Schema(example = "true", description = "false oculta el producto del API público sin eliminarlo") Boolean publicado
) {}
