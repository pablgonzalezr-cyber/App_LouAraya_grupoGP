package cl.louaraya.backend;

import cl.louaraya.backend.dto.CampaniaRequest;
import cl.louaraya.backend.dto.ColeccionRequest;
import cl.louaraya.backend.service.CampaniaService;
import cl.louaraya.backend.service.ColeccionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContenidoApiTest {
    private static final String COLECCIONES = "/api/colecciones";
    private static final String CAMPANIAS = "/api/campanias";
    private static final String ADMIN_COLECCIONES = "/api/admin/colecciones";
    private static final String ADMIN_CAMPANIAS = "/api/admin/campanias";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired ColeccionService colecciones;
    @Autowired CampaniaService campanias;

    @Test void administraColeccionYReemplazaAsociacionesSinBorrarProductos() throws Exception {
        long id = crearColeccion(new ColeccionRequest(" Tapices ", "Tejidos de algodón natural", true, false, Set.of(1L, 2L)));
        String admin = ADMIN_COLECCIONES + "/" + id;
        String publica = COLECCIONES + "/" + id;
        mvc.perform(get(admin).with(admin())).andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Tapices")).andExpect(jsonPath("$.productos").value(contains(1, 2)));
        mvc.perform(get(ADMIN_COLECCIONES).with(admin())).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get(publica)).andExpect(status().isNotFound());

        var actualizada = new ColeccionRequest("Tapices de pared", "Selección de tejidos de algodón", true, true, Set.of(2L, 6L));
        mvc.perform(put(admin).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json(actualizada)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.productos").value(contains(2, 6)));
        mvc.perform(get(publica)).andExpect(status().isOk())
            .andExpect(jsonPath("$.productos[*].id").value(contains(2, 6)))
            .andExpect(jsonPath("$.productos[1].disponible").value(false));
        mvc.perform(get(COLECCIONES)).andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(delete(admin).with(admin())).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get(publica)).andExpect(status().isNotFound());
        mvc.perform(get(COLECCIONES)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(admin).with(admin())).andExpect(status().isOk())
            .andExpect(jsonPath("$.activa").value(false)).andExpect(jsonPath("$.productos").value(contains(2, 6)));
        mvc.perform(get("/api/productos/1")).andExpect(status().isOk());
        mvc.perform(get("/api/productos/2")).andExpect(status().isOk());

        mvc.perform(put(admin).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json(actualizada)))
            .andExpect(status().isOk());
        mvc.perform(patch(admin + "/publicacion").with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"publicada\":false}")).andExpect(status().isOk());
        mvc.perform(get(publica)).andExpect(status().isNotFound());
    }

    @Test void conservaVinculosConProductosArchivadosYOcultosSinExponerlos() throws Exception {
        long id = crearColeccion(new ColeccionRequest("Tapices", "Selección para la casa", true, true, Set.of(1L, 2L, 3L)));
        mvc.perform(delete("/api/admin/productos/1").with(admin())).andExpect(status().isNoContent());
        mvc.perform(patch("/api/admin/productos/2/publicacion").with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"publicado\":false}")).andExpect(status().isOk());

        mvc.perform(get(COLECCIONES + "/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.productos[*].id").value(contains(3)));
        mvc.perform(get(ADMIN_COLECCIONES + "/" + id).with(admin()))
            .andExpect(jsonPath("$.productos").value(contains(1, 2, 3)));
    }

    @Test void validaDatosAsociacionesEIdentificadoresDeColeccion() throws Exception {
        mvc.perform(post(ADMIN_COLECCIONES).with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content(json(new ColeccionRequest(" A ", " Corto ", true, false, Set.of(0L)))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.nombre").exists())
            .andExpect(jsonPath("$.errores.descripcion").exists());
        mvc.perform(post(ADMIN_COLECCIONES).with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Tapices\",\"descripcion\":\"Selección para la casa\",\"activa\":true,\"publicada\":false,\"productos\":[0]}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(ADMIN_COLECCIONES).with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Tapices\",\"descripcion\":\"Selección para la casa\",\"activa\":true,\"publicada\":false,\"productos\":[1.9]}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(ADMIN_COLECCIONES).with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content(json(new ColeccionRequest("Tapices", "Selección para la casa", true, true, Set.of(999999L)))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.productos").exists());
        long id = crearColeccion(new ColeccionRequest("Tapices", "Selección para la casa", true, false, Set.of()));
        mvc.perform(patch(ADMIN_COLECCIONES + "/" + id + "/publicacion").with(admin())
            .contentType(MediaType.APPLICATION_JSON).content("{\"publicada\":true}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.productos").exists());

        mvc.perform(put(ADMIN_COLECCIONES + "/" + id).with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content(json(new ColeccionRequest("Tapices", "Selección para la casa", true, false, Set.of(1L, 999999L)))))
            .andExpect(status().isBadRequest());
        mvc.perform(get(ADMIN_COLECCIONES + "/" + id).with(admin()))
            .andExpect(jsonPath("$.productos.length()").value(0));
        assertEquals(0, jdbc.queryForObject("select count(*) from coleccion_productos", Integer.class));
        mvc.perform(get(COLECCIONES + "/no-numero")).andExpect(status().isBadRequest());
        mvc.perform(get(ADMIN_COLECCIONES + "/0").with(admin())).andExpect(status().isBadRequest());
        mvc.perform(delete(ADMIN_COLECCIONES + "/999999").with(admin())).andExpect(status().isNotFound());
    }

    @Test void administraCampaniaYAplicaVigenciaInclusivaYEstados() throws Exception {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Santiago"));
        long id = crearCampania(campania(hoy, hoy, false, true));
        String admin = ADMIN_CAMPANIAS + "/" + id;
        String publica = CAMPANIAS + "/" + id;
        mvc.perform(get(admin).with(admin())).andExpect(status().isOk())
            .andExpect(jsonPath("$.inicio").value(hoy.toString())).andExpect(jsonPath("$.publicada").value(false));
        mvc.perform(get(publica)).andExpect(status().isNotFound());

        mvc.perform(patch(admin + "/publicacion").with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"publicada\":true}")).andExpect(status().isOk());
        mvc.perform(get(publica)).andExpect(status().isOk()).andExpect(jsonPath("$.termino").value(hoy.toString()));

        long futura = crearCampania(campania(hoy.plusDays(1), hoy.plusDays(2), true, true));
        long vencida = crearCampania(campania(hoy.minusDays(2), hoy.minusDays(1), true, true));
        long inactiva = crearCampania(campania(hoy, hoy, true, false));
        mvc.perform(get(CAMPANIAS)).andExpect(jsonPath("$[*].id").value(contains((int) id)));
        for (long oculta : new long[]{futura, vencida, inactiva}) {
            mvc.perform(get(CAMPANIAS + "/" + oculta)).andExpect(status().isNotFound());
        }
        mvc.perform(get(ADMIN_CAMPANIAS).with(admin())).andExpect(jsonPath("$.length()").value(4));

        mvc.perform(put(admin).with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content(json(new CampaniaRequest("Edición de temporada", "Piezas tejidas a mano", hoy.minusDays(1), hoy, true, true))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Edición de temporada"));
        mvc.perform(patch(admin + "/publicacion").with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"publicada\":false}")).andExpect(status().isOk());
        mvc.perform(get(publica)).andExpect(status().isNotFound());
        mvc.perform(patch(admin + "/publicacion").with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"publicada\":true}")).andExpect(status().isOk());

        mvc.perform(delete(admin).with(admin())).andExpect(status().isNoContent());
        mvc.perform(get(publica)).andExpect(status().isNotFound());
        mvc.perform(get(admin).with(admin())).andExpect(status().isOk())
            .andExpect(jsonPath("$.activa").value(false)).andExpect(jsonPath("$.publicada").value(true));
    }

    @Test void validaFechasYRechazaIdsDeCampaniaIncorrectos() throws Exception {
        var inicio = LocalDate.of(2026, 11, 20);
        mvc.perform(post(ADMIN_CAMPANIAS).with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content(json(campania(inicio, inicio.minusDays(1), true, true))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.termino").exists());
        mvc.perform(post(ADMIN_CAMPANIAS).with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content(json(campania(inicio, inicio, true, true)).replace("2026-11-20", "2026-02-30")))
            .andExpect(status().isBadRequest());
        mvc.perform(post(ADMIN_CAMPANIAS).with(admin()).contentType(MediaType.APPLICATION_JSON)
            .content(json(campania(LocalDate.of(0, 1, 1), inicio, true, true))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.inicio").exists());
        mvc.perform(get(CAMPANIAS + "/999999")).andExpect(status().isNotFound());
        mvc.perform(get(ADMIN_CAMPANIAS + "/-1").with(admin())).andExpect(status().isBadRequest());
        mvc.perform(delete(ADMIN_CAMPANIAS + "/999999").with(admin())).andExpect(status().isNotFound());
    }

    @Test void exigeAutenticacionEnTodasLasOperacionesAdministrativas() throws Exception {
        for (String ruta : new String[]{ADMIN_COLECCIONES, ADMIN_CAMPANIAS}) {
            mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            mvc.perform(get(ruta + "/1")).andExpect(status().isUnauthorized());
            mvc.perform(post(ruta)).andExpect(status().isUnauthorized());
            mvc.perform(put(ruta + "/1")).andExpect(status().isUnauthorized());
            mvc.perform(patch(ruta + "/1/publicacion")).andExpect(status().isUnauthorized());
            mvc.perform(delete(ruta + "/1")).andExpect(status().isUnauthorized());
        }
    }

    @Test @WithMockUser(roles = "CLIENTE")
    void rechazaRolClienteEnHttpYServicio() throws Exception {
        assertThrows(AccessDeniedException.class, () -> colecciones.desactivar(1L));
        assertThrows(AccessDeniedException.class, () -> campanias.desactivar(1L));
        mvc.perform(delete(ADMIN_COLECCIONES + "/1")).andExpect(status().isForbidden());
        mvc.perform(delete(ADMIN_CAMPANIAS + "/1")).andExpect(status().isForbidden());
    }

    @Test void openApiExponeOperacionesNuevasYAutenticacion() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/colecciones/{id}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/campanias/{id}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/admin/colecciones'].post.responses['201']").exists())
            .andExpect(jsonPath("$.paths['/api/admin/campanias/{id}'].delete.responses['204']").exists())
            .andExpect(jsonPath("$.paths['/api/admin/campanias/{id}/publicacion'].patch.security[0].basicAuth").isArray());
    }

    private long crearColeccion(ColeccionRequest datos) throws Exception {
        return crear(ADMIN_COLECCIONES, datos);
    }

    private long crearCampania(CampaniaRequest datos) throws Exception {
        return crear(ADMIN_CAMPANIAS, datos);
    }

    private long crear(String ruta, Object datos) throws Exception {
        var respuesta = mvc.perform(post(ruta).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json(datos)))
            .andExpect(status().isCreated()).andReturn().getResponse();
        long id = mapper.readTree(respuesta.getContentAsString()).get("id").asLong();
        assertEquals(ruta + "/" + id, respuesta.getHeader("Location"));
        return id;
    }

    private CampaniaRequest campania(LocalDate inicio, LocalDate termino, boolean publicada, boolean activa) {
        return new CampaniaRequest("Temporada", "Piezas tejidas a mano", inicio, termino, publicada, activa);
    }

    private String json(Object datos) throws Exception { return mapper.writeValueAsString(datos); }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return httpBasic("admin-test", "SoloPruebas123!");
    }
}
