package cl.louaraya.backend.controller;

import cl.louaraya.backend.dto.ColeccionRequest;
import cl.louaraya.backend.dto.ColeccionResponse;
import cl.louaraya.backend.dto.PublicacionContenidoRequest;
import cl.louaraya.backend.service.ColeccionService;
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
@Tag(name = "Colecciones")
public class ColeccionController {
    private static final String PUBLICA = "/api/colecciones";
    private static final String ADMIN = "/api/admin/colecciones";
    private final ColeccionService service;

    public ColeccionController(ColeccionService service) {
        this.service = service;
    }

    @GetMapping(PUBLICA)
    @Operation(summary = "Listar colecciones activas y publicadas")
    public List<ColeccionResponse.Publica> publicas() {
        return service.publicas();
    }

    @GetMapping(PUBLICA + "/{id}")
    @Operation(summary = "Consultar una colección visible para clientes")
    public ColeccionResponse.Publica publica(@PathVariable @Positive Long id) {
        return service.publica(id);
    }

    @GetMapping(ADMIN)
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Listar colecciones en todos sus estados")
    public List<ColeccionResponse> todas() {
        return service.todas();
    }

    @GetMapping(ADMIN + "/{id}")
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Consultar una colección en cualquier estado")
    public ColeccionResponse detalle(@PathVariable @Positive Long id) {
        return service.detalle(id);
    }

    @PostMapping(ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Crear una colección")
    public ResponseEntity<ColeccionResponse> crear(@Valid @RequestBody ColeccionRequest datos) {
        ColeccionResponse creada = service.crear(datos);
        return ResponseEntity.created(URI.create(ADMIN + "/" + creada.id())).body(creada);
    }

    @PutMapping(ADMIN + "/{id}")
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Actualizar una colección")
    public ColeccionResponse editar(@PathVariable @Positive Long id, @Valid @RequestBody ColeccionRequest datos) {
        return service.editar(id, datos);
    }

    @PatchMapping(ADMIN + "/{id}/publicacion")
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Publicar o despublicar una colección", description = "Conserva el estado activa.")
    public ColeccionResponse publicar(@PathVariable @Positive Long id, @Valid @RequestBody PublicacionContenidoRequest datos) {
        return service.publicar(id, datos.publicada());
    }

    @DeleteMapping(ADMIN + "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Desactivar una colección", description = "Conserva el registro y sus productos asociados. Se puede reactivar con PUT.")
    public void desactivar(@PathVariable @Positive Long id) {
        service.desactivar(id);
    }
}
