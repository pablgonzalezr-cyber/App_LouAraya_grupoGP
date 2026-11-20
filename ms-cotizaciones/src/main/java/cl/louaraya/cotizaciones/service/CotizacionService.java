package cl.louaraya.cotizaciones.service;

import cl.louaraya.cotizaciones.dto.*;
import cl.louaraya.cotizaciones.exception.*;
import cl.louaraya.cotizaciones.model.entity.Cliente;
import cl.louaraya.cotizaciones.model.entity.Cotizacion;
import cl.louaraya.cotizaciones.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Service
public class CotizacionService {
    public record Resultado(CotizacionResponse cotizacion, boolean creada) {}
    private static final Set<String> CATEGORIAS = Set.of("Interiores", "Matrimonios", "Festivales", "Otro");
    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private final ClienteRepository clientes;
    private final CotizacionRepository solicitudes;
    public CotizacionService(ClienteRepository clientes, CotizacionRepository solicitudes) {
        this.clientes = clientes;
        this.solicitudes = solicitudes;
    }
    @PreAuthorize("hasRole('CLIENTE')")
    @Transactional
    public Resultado guardar(String correo, CotizacionRequest d) {
        // El bloqueo de la fila cliente serializa envios simultaneos del mismo usuario.
        Cliente cliente = clientes.bloquearPorCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontrado("Cliente no encontrado"));
        validar(d);
        var previa = solicitudes.findByClienteIdAndTokenEnvio(cliente.getId(), d.tokenEnvio());
        if (previa.isPresent()) {
            if (!previa.get().mismosDatos(d)) throw new ConflictoCotizacion("Token reutilizado con datos diferentes");
            return new Resultado(CotizacionResponse.desde(previa.get()), false);
        }
        Cotizacion creada = solicitudes.saveAndFlush(new Cotizacion(cliente, d, Instant.now()));
        return new Resultado(CotizacionResponse.desde(creada), true);
    }
    @PreAuthorize("hasRole('CLIENTE')")
    @Transactional(readOnly = true)
    public List<CotizacionResponse> propias(String correo) {
        Cliente c = cliente(correo);
        return solicitudes.findByClienteIdOrderByCreadaEnDescIdDesc(c.getId())
                .stream().map(CotizacionResponse::desde).toList();
    }
    @PreAuthorize("hasRole('CLIENTE')")
    @Transactional(readOnly = true)
    public CotizacionResponse detallePropio(String correo, Long id) {
        if (id == null || id <= 0) throw new SolicitudInvalida("ID invalido");
        Cliente c = cliente(correo);
        return solicitudes.findByIdAndClienteId(id, c.getId())
                .map(CotizacionResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontrado("Cotizacion no encontrada"));
    }
    private Cliente cliente(String correo) {
        return clientes.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontrado("Cliente no encontrado"));
    }
    private void validar(CotizacionRequest d) {
        if (d.descripcion() == null || d.descripcion().trim().length() < 10 || d.descripcion().trim().length() > 1000)
            throw new SolicitudInvalida("Descripcion: 10 a 1000 caracteres");
        if (d.categoria() == null || !CATEGORIAS.contains(d.categoria().trim()))
            throw new SolicitudInvalida("Categoria no permitida");
        if (d.material() == null || d.material().trim().length() < 2 || d.material().trim().length() > 100)
            throw new SolicitudInvalida("Material: 2 a 100 caracteres");
        if (d.fechaRequerida() != null && d.fechaRequerida().isBefore(LocalDate.now(ZONA)))
            throw new SolicitudInvalida("La fecha requerida no puede estar en el pasado");
        if (d.productoReferenciaId() == null && d.productoReferenciaNombre() != null)
            throw new SolicitudInvalida("Nombre de referencia sin producto asociado");
        if (d.productoReferenciaId() != null && (d.productoReferenciaNombre() == null
                || d.productoReferenciaNombre().isBlank()))
            throw new SolicitudInvalida("Falta nombre historico de producto de referencia");
    }
}