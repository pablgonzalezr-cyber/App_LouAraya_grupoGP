package cl.louaraya.backend.exception;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.TreeMap;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ContenidoNoEncontrado.class)
    public ProblemDetail contenidoNoEncontrado(ContenidoNoEncontrado error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(ContenidoInvalido.class)
    public ProblemDetail contenidoInvalido(ContenidoInvalido error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
        problema.setProperty("errores", error.getErrores());
        return problema;
    }

    @ExceptionHandler(ProductoNoEncontrado.class)
    public ProblemDetail noEncontrado(ProductoNoEncontrado error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
        HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los campos del producto");
        var campos = new TreeMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        problema.setProperty("errores", campos);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }
}
