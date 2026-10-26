package cl.louaraya.backend.service;

import cl.louaraya.backend.dto.*;
import cl.louaraya.backend.exception.ProductoNoEncontrado;
import cl.louaraya.backend.model.entity.Producto;
import cl.louaraya.backend.repository.ProductoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductoService {
    private final ProductoRepository repo;
    public ProductoService(ProductoRepository repo) { this.repo = repo; }
    public List<ProductoResponse> publicados() { return repo.findByPublicadoTrueOrderByIdAsc().stream().map(ProductoResponse::desde).toList(); }
    public ProductoResponse publicado(Long id) { return ProductoResponse.desde(repo.findByIdAndPublicadoTrue(id).orElseThrow(ProductoNoEncontrado::new)); }
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProductoResponse> todos() { return repo.findAllByOrderByIdAsc().stream().map(ProductoResponse::desde).toList(); }
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse crear(ProductoRequest d) { return ProductoResponse.desde(repo.save(new Producto(d))); }
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse editar(Long id, ProductoRequest d) { Producto p = buscar(id); p.actualizar(d); return ProductoResponse.desde(p); }
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse publicar(Long id, boolean publicado) { Producto p = buscar(id); p.publicar(publicado); return ProductoResponse.desde(p); }
    private Producto buscar(Long id) { return repo.findById(id).orElseThrow(ProductoNoEncontrado::new); }
}
