package cl.louaraya.cotizaciones.controller;

import cl.louaraya.cotizaciones.dto.*;
import cl.louaraya.cotizaciones.service.ClienteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Registro de cliente propio del servicio de cotizaciones")
public class ClienteController {
    private final ClienteService service;
    public ClienteController(ClienteService service) { this.service = service; }
    @PostMapping("/registro")
    public ResponseEntity<RegistroClienteResponse> registrar(@Valid @RequestBody RegistroClienteRequest d) {
        RegistroClienteResponse r = service.registrar(d);
        return ResponseEntity.created(URI.create("/api/clientes/" + r.id())).body(r);
    }
}
