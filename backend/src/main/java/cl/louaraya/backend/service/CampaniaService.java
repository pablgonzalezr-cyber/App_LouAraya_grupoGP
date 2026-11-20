package cl.louaraya.backend.service;

import cl.louaraya.backend.dto.CampaniaRequest;
import cl.louaraya.backend.dto.CampaniaResponse;
import cl.louaraya.backend.exception.ContenidoInvalido;
import cl.louaraya.backend.exception.ContenidoNoEncontrado;
import cl.louaraya.backend.model.entity.Campania;
import cl.louaraya.backend.repository.CampaniaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CampaniaService {
    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");
    private final CampaniaRepository campanias;

    public CampaniaService(CampaniaRepository campanias) {
        this.campanias = campanias;
    }

    public List<CampaniaResponse> vigentes() {
        return campanias.vigentes(LocalDate.now(SANTIAGO)).stream().map(this::respuesta).toList();
    }

    public CampaniaResponse vigente(Long id) {
        return respuesta(campanias.vigente(id, LocalDate.now(SANTIAGO)).orElseThrow(this::noEncontrada));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<CampaniaResponse> todas() {
        return campanias.findAllByOrderByIdDesc().stream().map(this::respuesta).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public CampaniaResponse detalle(Long id) {
        return respuesta(campanias.findById(id).orElseThrow(this::noEncontrada));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CampaniaResponse crear(CampaniaRequest datos) {
        validarFechas(datos);
        return respuesta(campanias.save(new Campania(datos)));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CampaniaResponse editar(Long id, CampaniaRequest datos) {
        Campania campania = buscarParaModificar(id);
        validarFechas(datos);
        campania.actualizar(datos);
        return respuesta(campania);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CampaniaResponse publicar(Long id, boolean publicada) {
        Campania campania = buscarParaModificar(id);
        campania.publicar(publicada);
        return respuesta(campania);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void desactivar(Long id) {
        buscarParaModificar(id).desactivar();
    }

    private void validarFechas(CampaniaRequest datos) {
        Map<String, String> errores = new HashMap<>();
        if (!anioValido(datos.inicio())) errores.put("inicio", "Usa un año entre 1 y 9999");
        if (!anioValido(datos.termino())) errores.put("termino", "Usa un año entre 1 y 9999");
        if (errores.isEmpty() && datos.termino().isBefore(datos.inicio())) {
            errores.put("termino", "El término debe ser igual o posterior al inicio");
        }
        if (!errores.isEmpty()) throw new ContenidoInvalido(errores);
    }

    private boolean anioValido(LocalDate fecha) {
        return fecha.getYear() >= 1 && fecha.getYear() <= 9999;
    }

    private Campania buscarParaModificar(Long id) {
        return campanias.findCampaniaById(id).orElseThrow(this::noEncontrada);
    }

    private ContenidoNoEncontrado noEncontrada() {
        return new ContenidoNoEncontrado("Campaña no encontrada");
    }

    private CampaniaResponse respuesta(Campania c) {
        return new CampaniaResponse(c.getId(), c.getTitulo(), c.getDescripcion(), c.getInicio(), c.getTermino(),
            c.isPublicada(), c.isActiva());
    }
}
