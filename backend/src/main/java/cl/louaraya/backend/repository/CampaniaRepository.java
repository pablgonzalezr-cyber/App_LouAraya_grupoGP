package cl.louaraya.backend.repository;

import cl.louaraya.backend.model.entity.Campania;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CampaniaRepository extends JpaRepository<Campania, Long> {
    List<Campania> findAllByOrderByIdDesc();

    @Query("""
        select c from Campania c where c.activa = true and c.publicada = true
        and :hoy between c.inicio and c.termino order by c.inicio desc, c.id desc
        """)
    List<Campania> vigentes(@Param("hoy") LocalDate hoy);

    @Query("""
        select c from Campania c where c.id = :id and c.activa = true and c.publicada = true
        and :hoy between c.inicio and c.termino
        """)
    Optional<Campania> vigente(@Param("id") Long id, @Param("hoy") LocalDate hoy);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Campania> findCampaniaById(Long id);
}
