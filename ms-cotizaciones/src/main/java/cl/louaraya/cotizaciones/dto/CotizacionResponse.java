package cl.louaraya.cotizaciones.dto;

import cl.louaraya.cotizaciones.model.EstadoCotizacion;
import cl.louaraya.cotizaciones.model.entity.Cotizacion;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CotizacionResponse(Long id, Long usuarioId, String tokenEnvio,
                                 String descripcion, String categoria, String material, BigDecimal anchoCm,
                                 BigDecimal largoCm, Integer cantidad, LocalDate fechaRequerida,
                                 Long productoReferenciaId, String productoReferenciaNombre,
                                 Long creadaEn, EstadoCotizacion estado) {
    public static CotizacionResponse desde(Cotizacion c) {
        return new CotizacionResponse(c.getId(), c.getCliente().getId(), c.getTokenEnvio(),
                c.getDescripcion(), c.getCategoria(), c.getMaterial(), c.getAnchoCm(),
                c.getLargoCm(), c.getCantidad(), c.getFechaRequerida(),
                c.getProductoReferenciaId(), c.getProductoReferenciaNombre(),
                c.getCreadaEn().toEpochMilli(), c.getEstado());
    }
}
