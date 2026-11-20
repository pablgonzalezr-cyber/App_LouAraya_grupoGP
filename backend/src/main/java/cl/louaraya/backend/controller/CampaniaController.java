package cl.louaraya.backend.controller;

import cl.louaraya.backend.dto.CampaniaRequest;
import cl.louaraya.backend.dto.CampaniaResponse;
import cl.louaraya.backend.dto.PublicacionContenidoRequest;
import cl.louaraya.backend.service.CampaniaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Campañas")
public class CampaniaController {
    private static final String PUBLICA = "/api/campanias";
    private static final String ADMIN = "/api/admin/campanias";
    private final CampaniaService service;

    public CampaniaController(CampaniaService service) {
        this.service = service;
    }

    @GetMapping(PUBLICA)
    @Operation(summary = "Listar campañas activas, publicadas y vigentes")
    public List<CampaniaResponse> vigentes() {
        return service.vigentes();
    }

    @GetMapping(PUBLICA + "/{id}")
    @Operation(summary = "Consultar una campaña visible para clientes")
    public CampaniaResponse vigente(@PathVariable @Positive Long id) {
        return service.vigente(id);
    }

    @GetMapping(ADMIN)
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Listar campañas en todos sus estados")
    public List<CampaniaResponse> todas() {
        return service.todas();
    }

    @GetMapping(ADMIN + "/{id}")
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Consultar una campaña en cualquier estado")
    public CampaniaResponse detalle(@PathVariable @Positive Long id) {
        return service.detalle(id);
    }

    @PostMapping(ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Crear una campaña")
    public ResponseEntity<CampaniaResponse> crear(@Valid @RequestBody CampaniaRequest datos) {
        CampaniaResponse creada = service.crear(datos);
        return ResponseEntity.created(URI.create(ADMIN + "/" + creada.id())).body(creada);
    }

    @PutMapping(ADMIN + "/{id}")
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Actualizar una campaña")
    public CampaniaResponse editar(@PathVariable @Positive Long id, @Valid @RequestBody CampaniaRequest datos) {
        return service.editar(id, datos);
    }

    @PatchMapping(ADMIN + "/{id}/publicacion")
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Publicar o despublicar una campaña", description = "Conserva el estado activa.")
    public CampaniaResponse publicar(@PathVariable @Positive Long id, @Valid @RequestBody PublicacionContenidoRequest datos) {
        return service.publicar(id, datos.publicada());
    }

    @DeleteMapping(ADMIN + "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Desactivar una campaña", description = "Conserva el registro. Se puede reactivar con PUT.")
    public void desactivar(@PathVariable @Positive Long id) {
        service.desactivar(id);
    }
}
