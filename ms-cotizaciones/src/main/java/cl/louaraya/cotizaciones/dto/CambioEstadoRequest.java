package cl.louaraya.cotizaciones.dto;

import cl.louaraya.cotizaciones.model.EstadoCotizacion;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(
        @NotNull EstadoCotizacion esperado,
        @NotNull EstadoCotizacion nuevo
) {}
