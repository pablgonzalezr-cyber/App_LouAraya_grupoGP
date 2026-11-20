package cl.louaraya.backend.dto;

import java.util.List;

public record ColeccionResponse(Long id, String nombre, String descripcion, boolean activa,
                                boolean publicada, List<Long> productos) {
    public record Publica(Long id, String nombre, String descripcion, boolean activa,
                          boolean publicada, List<ProductoResponse> productos) {}
}
