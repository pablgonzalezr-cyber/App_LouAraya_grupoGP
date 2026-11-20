package cl.louaraya.cotizaciones;

import cl.louaraya.cotizaciones.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminCotizacionesTest {
    private static final String ADMIN = "admin-test";
    private static final String ADMIN_CLAVE = "ClaveAdminPruebas2026!";
    private static final String CLAVE = "ClaveClientePruebas2026!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String registrar() throws Exception {
        String correo = "cot-" + UUID.randomUUID() + "@ejemplo.test";
        mvc.perform(post("/api/clientes/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegistroClienteRequest(correo, CLAVE))))
                .andExpect(status().isCreated());
        return correo;
    }
    private long crear() throws Exception {
        String correo = registrar();
        var d = new CotizacionRequest(UUID.randomUUID().toString(),
                "Necesito un macrame especial para una celebracion", "Matrimonios", "Hilo de algodon",
                new BigDecimal("60"), new BigDecimal("120"), 1,
                LocalDate.now(ZoneId.of("America/Santiago")).plusDays(10), null, null);
        var respuesta = mvc.perform(post("/api/cotizaciones").with(httpBasic(correo, CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(d)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(respuesta.getResponse().getContentAsString()).get("id").asLong();
    }
    private String cambio(String esperado, String nuevo) throws Exception {
        return json.writeValueAsString(new CambioEstadoRequest(
                cl.louaraya.cotizaciones.model.EstadoCotizacion.valueOf(esperado),
                cl.louaraya.cotizaciones.model.EstadoCotizacion.valueOf(nuevo)));
    }
    @Test void administradorListaFiltraYConsulta() throws Exception {
        long id = crear();
        mvc.perform(get("/api/admin/cotizaciones").with(httpBasic(ADMIN, ADMIN_CLAVE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/admin/cotizaciones?estado=PENDIENTE").with(httpBasic(ADMIN, ADMIN_CLAVE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/admin/cotizaciones/" + id).with(httpBasic(ADMIN, ADMIN_CLAVE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
    }
    @Test void transicionValidaYSinSaltosNiRetrocesos() throws Exception {
        long id = crear();
        String ruta = "/api/admin/cotizaciones/" + id + "/estado";
        mvc.perform(patch(ruta).with(httpBasic(ADMIN, ADMIN_CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(cambio("PENDIENTE", "APROBADA")))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(ruta).with(httpBasic(ADMIN, ADMIN_CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(cambio("PENDIENTE", "EN_REVISION")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_REVISION"));
        mvc.perform(patch(ruta).with(httpBasic(ADMIN, ADMIN_CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(cambio("PENDIENTE", "EN_REVISION")))
                .andExpect(status().isConflict());
        mvc.perform(patch(ruta).with(httpBasic(ADMIN, ADMIN_CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(cambio("EN_REVISION", "RECHAZADA")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RECHAZADA"));
        mvc.perform(patch(ruta).with(httpBasic(ADMIN, ADMIN_CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(cambio("RECHAZADA", "EN_REVISION")))
                .andExpect(status().isBadRequest());
    }
    @Test void accesoLimitadoAAdministradorYErroresDeId() throws Exception {
        long id = crear();
        mvc.perform(get("/api/admin/cotizaciones")).andExpect(status().isUnauthorized());
        String clienteReal = registrar();
        mvc.perform(get("/api/admin/cotizaciones").with(httpBasic(clienteReal, CLAVE)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/cotizaciones").with(httpBasic("cliente-falso", CLAVE)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/cotizaciones/999999").with(httpBasic(ADMIN, ADMIN_CLAVE)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/cotizaciones/no-numero").with(httpBasic(ADMIN, ADMIN_CLAVE)))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/cotizaciones/" + id + "/estado").with(httpBasic(ADMIN, ADMIN_CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"esperado\":\"PENDIENTE\",\"nuevo\":\"INVENTADO\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void openApiDocumentaEndpointsAdministrativos() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/cotizaciones'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/cotizaciones/{id}/estado'].patch").exists());
    }
}
