package cl.louaraya.backend.repository;

import cl.louaraya.backend.model.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByPublicadoTrueOrderByIdAsc();
    List<Producto> findAllByOrderByIdAsc();
    Optional<Producto> findByIdAndPublicadoTrue(Long id);
}
