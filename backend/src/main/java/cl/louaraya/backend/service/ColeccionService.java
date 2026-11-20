package cl.louaraya.backend.service;

import cl.louaraya.backend.dto.*;
import cl.louaraya.backend.exception.ContenidoInvalido;
import cl.louaraya.backend.exception.ContenidoNoEncontrado;
import cl.louaraya.backend.model.entity.Coleccion;
import cl.louaraya.backend.model.entity.Producto;
import cl.louaraya.backend.repository.ColeccionRepository;
import cl.louaraya.backend.repository.ProductoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ColeccionService {
    private final ColeccionRepository colecciones;
    private final ProductoRepository productos;

    public ColeccionService(ColeccionRepository colecciones, ProductoRepository productos) {
        this.colecciones = colecciones;
        this.productos = productos;
    }

    public List<ColeccionResponse.Publica> publicas() {
        return colecciones.findByActivaTrueAndPublicadaTrueOrderByIdDesc().stream().map(this::publica).toList();
    }

    public ColeccionResponse.Publica publica(Long id) {
        return publica(colecciones.findByIdAndActivaTrueAndPublicadaTrue(id).orElseThrow(this::noEncontrada));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<ColeccionResponse> todas() {
        return colecciones.findAllByOrderByIdDesc().stream().map(this::respuesta).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ColeccionResponse detalle(Long id) {
        return respuesta(colecciones.findById(id).orElseThrow(this::noEncontrada));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ColeccionResponse crear(ColeccionRequest datos) {
        return respuesta(colecciones.save(new Coleccion(datos, miembros(datos))));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ColeccionResponse editar(Long id, ColeccionRequest datos) {
        Coleccion coleccion = buscarParaModificar(id);
        coleccion.actualizar(datos, miembros(datos));
        return respuesta(coleccion);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ColeccionResponse publicar(Long id, boolean publicada) {
        Coleccion coleccion = buscarParaModificar(id);
        validarPublicacion(publicada, coleccion.getProductos());
        coleccion.publicar(publicada);
        return respuesta(coleccion);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void desactivar(Long id) {
        buscarParaModificar(id).desactivar();
    }

    private Set<Producto> miembros(ColeccionRequest datos) {
        validarPublicacion(datos.publicada(), datos.productos());
        Set<Producto> encontrados = new HashSet<>(productos.findAllById(datos.productos()));
        if (encontrados.size() != datos.productos().size()) {
            throw new ContenidoInvalido(Map.of("productos", "Uno o más productos no existen"));
        }
        return encontrados;
    }

    private void validarPublicacion(boolean publicada, Set<?> miembros) {
        if (publicada && miembros.isEmpty()) {
            throw new ContenidoInvalido(Map.of("productos", "Asocia al menos un producto para publicar"));
        }
    }

    private Coleccion buscarParaModificar(Long id) {
        return colecciones.findColeccionById(id).orElseThrow(this::noEncontrada);
    }

    private ContenidoNoEncontrado noEncontrada() {
        return new ContenidoNoEncontrado("Colección no encontrada");
    }

    private ColeccionResponse respuesta(Coleccion c) {
        List<Long> ids = c.getProductos().stream().map(Producto::getId).sorted().toList();
        return new ColeccionResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.isActiva(), c.isPublicada(), ids);
    }

    private ColeccionResponse.Publica publica(Coleccion c) {
        List<ProductoResponse> visibles = c.getProductos().stream()
            .filter(p -> p.isPublicado() && !p.isArchivado())
            .sorted(Comparator.comparing(Producto::getId)).map(ProductoResponse::desde).toList();
        return new ColeccionResponse.Publica(c.getId(), c.getNombre(), c.getDescripcion(), c.isActiva(), c.isPublicada(), visibles);
    }
}
