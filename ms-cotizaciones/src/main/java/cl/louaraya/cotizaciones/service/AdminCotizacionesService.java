package cl.louaraya.cotizaciones.service;

import cl.louaraya.cotizaciones.dto.*;
import cl.louaraya.cotizaciones.exception.*;
import cl.louaraya.cotizaciones.model.EstadoCotizacion;
import cl.louaraya.cotizaciones.model.entity.Cotizacion;
import cl.louaraya.cotizaciones.repository.CotizacionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminCotizacionesService {
    private final CotizacionRepository repo;
    public AdminCotizacionesService(CotizacionRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<CotizacionResponse> listar(EstadoCotizacion filtro) {
        return repo.findAllByOrderByCreadaEnDescIdDesc().stream()
                .filter(c -> filtro == null || c.getEstado() == filtro)
                .map(CotizacionResponse::desde).toList();
    }
    @Transactional(readOnly = true)
    public CotizacionResponse detalle(Long id) {
        validarId(id);
        return repo.findById(id).map(CotizacionResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontrado("Cotizacion no encontrada"));
    }
    @Transactional
    public CotizacionResponse cambiarEstado(Long id, CambioEstadoRequest d) {
        validarId(id);
        Cotizacion c = repo.bloquearPorId(id)
                .orElseThrow(() -> new RecursoNoEncontrado("Cotizacion no encontrada"));
        EstadoCotizacion actual = c.getEstado();
        if (d.esperado() != actual)
            throw new ConflictoCotizacion("Estado desactualizado; actualiza el detalle antes de continuar");
        boolean valida = (actual == EstadoCotizacion.PENDIENTE && d.nuevo() == EstadoCotizacion.EN_REVISION)
                || (actual == EstadoCotizacion.EN_REVISION && (d.nuevo() == EstadoCotizacion.APROBADA
                || d.nuevo() == EstadoCotizacion.RECHAZADA));
        if (!valida) throw new SolicitudInvalida("Transicion de estado no permitida");
        c.cambiarEstado(d.nuevo());
        return CotizacionResponse.desde(repo.saveAndFlush(c));
    }
    private void validarId(Long id) {
        if (id == null || id <= 0) throw new SolicitudInvalida("ID invalido");
    }
}

