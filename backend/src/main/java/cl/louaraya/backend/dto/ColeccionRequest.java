package cl.louaraya.backend.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.util.Set;

public record ColeccionRequest(
    @NotBlank @Size(min = 2, max = 100) String nombre,
    @NotBlank @Size(min = 10, max = 2000) String descripcion,
    @NotNull Boolean activa,
    @NotNull Boolean publicada,
    @NotNull @JsonDeserialize(contentUsing = IdProductoDeserializer.class) Set<@NotNull @Positive Long> productos
) {
    public ColeccionRequest {
        if (nombre != null) nombre = nombre.strip();
        if (descripcion != null) descripcion = descripcion.strip();
    }

    public static class IdProductoDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext contexto) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return contexto.reportInputMismatch(Long.class, "Los IDs de productos deben ser números enteros");
            }
            return parser.getLongValue();
        }
    }
}
