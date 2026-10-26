package cl.louaraya.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PublicacionRequest(@NotNull @Schema(example = "false", description = "Publicar u ocultar, sin eliminar ni cambiar disponibilidad") Boolean publicado) {}
