package cl.louaraya.backend.repository;

import cl.louaraya.backend.model.entity.Coleccion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface ColeccionRepository extends JpaRepository<Coleccion, Long> {
    @EntityGraph(attributePaths = "productos")
    List<Coleccion> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = "productos")
    List<Coleccion> findByActivaTrueAndPublicadaTrueOrderByIdDesc();

    @EntityGraph(attributePaths = "productos")
    Optional<Coleccion> findByIdAndActivaTrueAndPublicadaTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Coleccion> findColeccionById(Long id);
}
