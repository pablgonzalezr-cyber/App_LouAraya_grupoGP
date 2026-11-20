package cl.louaraya.cotizaciones.repository;

import cl.louaraya.cotizaciones.model.entity.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
    Optional<Cotizacion> findByClienteIdAndTokenEnvio(Long clienteId, String tokenEnvio);
    Optional<Cotizacion> findByIdAndClienteId(Long id, Long clienteId);
    List<Cotizacion> findByClienteIdOrderByCreadaEnDescIdDesc(Long clienteId);
    List<Cotizacion> findAllByOrderByCreadaEnDescIdDesc();
}
