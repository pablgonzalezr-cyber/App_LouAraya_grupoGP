package cl.louaraya.backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CampaniaRequest(
    @NotBlank @Size(min = 2, max = 100) String titulo,
    @NotBlank @Size(min = 10, max = 2000) String descripcion,
    @NotNull LocalDate inicio,
    @NotNull LocalDate termino,
    @NotNull Boolean publicada,
    @NotNull Boolean activa
) {
    public CampaniaRequest {
        if (titulo != null) titulo = titulo.strip();
        if (descripcion != null) descripcion = descripcion.strip();
    }
}
