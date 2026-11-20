package cl.louaraya.cotizaciones.repository;

import cl.louaraya.cotizaciones.model.entity.Cotizacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
    Optional<Cotizacion> findByClienteIdAndTokenEnvio(Long clienteId, String tokenEnvio);
    Optional<Cotizacion> findByIdAndClienteId(Long id, Long clienteId);
    List<Cotizacion> findByClienteIdOrderByCreadaEnDescIdDesc(Long clienteId);
    List<Cotizacion> findAllByOrderByCreadaEnDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cotizacion c where c.id = :id")
    Optional<Cotizacion> bloquearPorId(@Param("id") Long id);
}
