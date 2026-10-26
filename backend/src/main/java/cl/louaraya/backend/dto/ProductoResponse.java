package cl.louaraya.backend.dto;

import cl.louaraya.backend.model.entity.Producto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Producto persistido; su ID no cambia al ocultarlo o republicarlo")
public record ProductoResponse(Long id, String nombre, String descripcion, String categoria, String material,
    BigDecimal anchoCm, BigDecimal altoCm, BigDecimal precio, boolean disponible, boolean publicado) {
    public static ProductoResponse desde(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getCategoria(), p.getMaterial(),
            p.getAnchoCm(), p.getAltoCm(), p.getPrecio(), p.isDisponible(), p.isPublicado());
    }
}
