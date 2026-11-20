package cl.louaraya.backend.controller;

import cl.louaraya.backend.dto.*;
import cl.louaraya.backend.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Productos", description = "Catálogo académico local. Publicación y disponibilidad son independientes.")
@ApiResponses({@ApiResponse(responseCode = "400", description = "Datos, JSON o identificador inválidos"),
    @ApiResponse(responseCode = "404", description = "Producto inexistente, archivado u oculto para consulta pública")})
public class ProductoController {
    private final ProductoService service;
    public ProductoController(ProductoService service) { this.service = service; }
    @GetMapping("/api/productos") @Operation(summary = "Listar únicamente productos publicados")
    public List<ProductoResponse> publicados() { return service.publicados(); }
    @GetMapping("/api/productos/{id}") @Operation(summary = "Consultar un producto publicado")
    public ProductoResponse publicado(@PathVariable Long id) { return service.publicado(id); }
    @GetMapping("/api/admin/productos") @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Listar publicados y ocultos sin incluir archivados (ADMIN)")
    @ApiResponses({@ApiResponse(responseCode = "401", description = "Credenciales ausentes o incorrectas"), @ApiResponse(responseCode = "403", description = "Rol administrativo requerido")})
    public List<ProductoResponse> todos() { return service.todos(); }
    @PostMapping("/api/admin/productos") @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Crear producto (ADMIN)")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Producto creado; cabecera Location con su URI pública"), @ApiResponse(responseCode = "401", description = "Credenciales ausentes o incorrectas"), @ApiResponse(responseCode = "403", description = "Rol administrativo requerido")})
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest d) {
        ProductoResponse nuevo = service.crear(d);
        return ResponseEntity.created(URI.create("/api/productos/" + nuevo.id())).body(nuevo);
    }
    @PutMapping("/api/admin/productos/{id}") @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Editar todos los campos, conservando el ID (ADMIN)")
    @ApiResponses({@ApiResponse(responseCode = "401", description = "Credenciales ausentes o incorrectas"), @ApiResponse(responseCode = "403", description = "Rol administrativo requerido")})
    public ProductoResponse editar(@PathVariable Long id, @Valid @RequestBody ProductoRequest d) { return service.editar(id, d); }
    @PatchMapping("/api/admin/productos/{id}/publicacion") @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Ocultar o republicar sin eliminar ni cambiar disponibilidad (ADMIN)")
    @ApiResponses({@ApiResponse(responseCode = "401", description = "Credenciales ausentes o incorrectas"), @ApiResponse(responseCode = "403", description = "Rol administrativo requerido")})
    public ProductoResponse publicar(@PathVariable Long id, @Valid @RequestBody PublicacionRequest d) { return service.publicar(id, d.publicado()); }

    @DeleteMapping("/api/admin/productos/{id}") @SecurityRequirement(name = "basicAuth")
    @Operation(summary = "Archivar producto (ADMIN)", description = "Conserva el ID y los datos. El producto sale de ambos listados y queda fuera de consulta pública, edición y republicación.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Producto archivado", content = @Content),
        @ApiResponse(responseCode = "401", description = "Credenciales ausentes o incorrectas"),
        @ApiResponse(responseCode = "403", description = "Rol administrativo requerido"),
        @ApiResponse(responseCode = "404", description = "Producto inexistente o ya archivado")
    })
    public ResponseEntity<Void> archivar(@PathVariable Long id) {
        service.archivar(id);
        return ResponseEntity.noContent().build();
    }
}
