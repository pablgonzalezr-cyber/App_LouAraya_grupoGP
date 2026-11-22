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
class RegistroYCotizacionesTest {
    private static final String CLAVE = "ClaveClientePruebas2026!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String registrar() throws Exception {
        String correo = "cliente-" + UUID.randomUUID() + "@ejemplo.test";
        mvc.perform(post("/api/clientes/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegistroClienteRequest(correo, CLAVE))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.correo").value(correo));
        return correo;
    }
    private CotizacionRequest datos(String token) {
        return new CotizacionRequest(token, "Quiero un tapiz tejido a mano para mi casa", "Interiores",
                "Algodon natural", new BigDecimal("40.5"), new BigDecimal("70"), 2,
                LocalDate.now(ZoneId.of("America/Santiago")).plusDays(5), 1L, "Tapiz mural");
    }
    @Test void clienteRegistraYConsultaSuCotizacion() throws Exception {
        String email = registrar();
        CotizacionRequest d = datos(UUID.randomUUID().toString());
        var creado = mvc.perform(post("/api/cotizaciones").with(httpBasic(email, CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(d)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.productoReferenciaId").value(1))
                .andReturn();
        long id = json.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/cotizaciones/mias").with(httpBasic(email, CLAVE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/cotizaciones/mias/" + id).with(httpBasic(email, CLAVE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
    }
    @Test void tokenRepetidoDevuelveMismoIdSinDuplicarYConflictoSiCambia() throws Exception {
        String email = registrar();
        String token = UUID.randomUUID().toString();
        String cuerpo = json.writeValueAsString(datos(token));
        var primera = mvc.perform(post("/api/cotizaciones").with(httpBasic(email, CLAVE))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isCreated()).andReturn();
        long id = json.readTree(primera.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/cotizaciones").with(httpBasic(email, CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/cotizaciones/mias").with(httpBasic(email, CLAVE)))
                .andExpect(jsonPath("$", hasSize(1)));
        CotizacionRequest d = datos(token);
        CotizacionRequest cambiada = new CotizacionRequest(d.tokenEnvio(), "Esta es otra descripcion muy distinta", d.categoria(),
                d.material(), d.anchoCm(), d.largoCm(), d.cantidad(), d.fechaRequerida(), d.productoReferenciaId(), d.productoReferenciaNombre());
        mvc.perform(post("/api/cotizaciones").with(httpBasic(email, CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cambiada)))
                .andExpect(status().isConflict());
    }
    @Test void aislamientoEntreClientes() throws Exception {
        String uno = registrar();
        String otro = registrar();
        var creado = mvc.perform(post("/api/cotizaciones").with(httpBasic(uno, CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos(UUID.randomUUID().toString()))))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/cotizaciones/mias/" + id).with(httpBasic(otro, CLAVE)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/cotizaciones/mias").with(httpBasic(otro, CLAVE)))
                .andExpect(jsonPath("$", hasSize(0)));
    }
    @Test void proteccionYValidacion() throws Exception {
        String email = registrar();
        mvc.perform(get("/api/cotizaciones/mias")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cotizaciones/mias").with(httpBasic("admin-test", "ClaveAdminPruebas2026!")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/cotizaciones").with(httpBasic(email, CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"descripcion\":\"corta\"}"))
                .andExpect(status().isBadRequest());
        CotizacionRequest d = datos(UUID.randomUUID().toString());
        CotizacionRequest vencida = new CotizacionRequest(d.tokenEnvio(), d.descripcion(), d.categoria(),
                d.material(), d.anchoCm(), d.largoCm(), d.cantidad(), LocalDate.now(ZoneId.of("America/Santiago")).minusDays(1), d.productoReferenciaId(), d.productoReferenciaNombre());
        mvc.perform(post("/api/cotizaciones").with(httpBasic(email, CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(vencida)))
                .andExpect(status().isBadRequest());
    }
}
