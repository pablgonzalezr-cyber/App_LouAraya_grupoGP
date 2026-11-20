package cl.louaraya.backend.exception;

import java.util.Map;

public class ContenidoInvalido extends RuntimeException {
    private final Map<String, String> errores;

    public ContenidoInvalido(Map<String, String> errores) {
        super("Revisa los campos del contenido");
        this.errores = Map.copyOf(errores);
    }

    public Map<String, String> getErrores() { return errores; }
}
