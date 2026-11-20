package cl.louaraya.cotizaciones.controller;

import cl.louaraya.cotizaciones.dto.*;
import cl.louaraya.cotizaciones.model.EstadoCotizacion;
import cl.louaraya.cotizaciones.service.AdminCotizacionesService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/cotizaciones")
@SecurityRequirement(name = "basicAuth")
@Tag(name = "Cotizaciones ADMIN", description = "Seguimiento y transiciones seguras de estados")
public class AdminCotizacionesController {
    private final AdminCotizacionesService service;
    public AdminCotizacionesController(AdminCotizacionesService service) { this.service = service; }
    @GetMapping
    public List<CotizacionResponse> listar(@RequestParam(required = false) EstadoCotizacion estado) {
        return service.listar(estado);
    }
    @GetMapping("/{id}")
    public CotizacionResponse detalle(@PathVariable Long id) { return service.detalle(id); }
    @PatchMapping("/{id}/estado")
    public CotizacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest datos) {
        return service.cambiarEstado(id, datos);
    }
}
