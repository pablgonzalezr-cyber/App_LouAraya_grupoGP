package cl.louaraya.backend.dto;

import jakarta.validation.constraints.NotNull;

public record PublicacionContenidoRequest(@NotNull Boolean publicada) {}
