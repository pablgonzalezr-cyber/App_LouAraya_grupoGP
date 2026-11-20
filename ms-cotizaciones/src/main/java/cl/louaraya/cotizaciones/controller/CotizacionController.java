package cl.louaraya.cotizaciones.controller;

import cl.louaraya.cotizaciones.dto.*;
import cl.louaraya.cotizaciones.service.CotizacionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/cotizaciones")
@SecurityRequirement(name = "basicAuth")
@Tag(name = "Cotizaciones cliente", description = "El propietario se deduce de las credenciales, nunca de usuarioId enviado por Android")
public class CotizacionController {
    private final CotizacionService service;
    public CotizacionController(CotizacionService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<CotizacionResponse> crear(@Valid @RequestBody CotizacionRequest datos, Authentication auth) {
        CotizacionService.Resultado resultado = service.guardar(auth.getName(), datos);
        return ResponseEntity.status(resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK)
                .location(URI.create("/api/cotizaciones/mias/" + resultado.cotizacion().id()))
                .body(resultado.cotizacion());
    }
    @GetMapping("/mias")
    public List<CotizacionResponse> propias(Authentication auth) { return service.propias(auth.getName()); }
    @GetMapping("/mias/{id}")
    public CotizacionResponse detalle(@PathVariable Long id, Authentication auth) {
        return service.detallePropio(auth.getName(), id);
    }
}

