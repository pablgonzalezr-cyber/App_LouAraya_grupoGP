package cl.louaraya.cotizaciones.service;

import cl.louaraya.cotizaciones.dto.*;
import cl.louaraya.cotizaciones.exception.*;
import cl.louaraya.cotizaciones.model.entity.Cliente;
import cl.louaraya.cotizaciones.repository.ClienteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class ClienteService {
    private final ClienteRepository repo;
    private final PasswordEncoder encoder;
    public ClienteService(ClienteRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }
    @Transactional
    public RegistroClienteResponse registrar(RegistroClienteRequest d) {
        String correo = d.correo().trim().toLowerCase(Locale.ROOT);
        if (correo.isBlank() || d.clave().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new SolicitudInvalida("Correo o longitud de clave invalida");
        if (repo.existsByCorreo(correo)) throw new ConflictoCotizacion("Correo ya registrado");
        try {
            Cliente nuevo = repo.saveAndFlush(new Cliente(correo, encoder.encode(d.clave())));
            return new RegistroClienteResponse(nuevo.getId(), nuevo.getCorreo());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoCotizacion("Correo ya registrado");
        }
    }
}