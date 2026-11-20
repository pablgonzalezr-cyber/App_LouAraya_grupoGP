package cl.louaraya.backend.repository;

import cl.louaraya.backend.model.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByPublicadoTrueAndArchivadoFalseOrderByIdAsc();
    List<Producto> findByArchivadoFalseOrderByIdAsc();
    Optional<Producto> findByIdAndPublicadoTrueAndArchivadoFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Producto> findByIdAndArchivadoFalse(Long id);
}
