package cl.louaraya.backend.dto;

import java.time.LocalDate;

public record CampaniaResponse(Long id, String titulo, String descripcion, LocalDate inicio,
                               LocalDate termino, boolean publicada, boolean activa) {}
