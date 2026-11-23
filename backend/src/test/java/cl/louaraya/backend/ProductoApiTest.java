package cl.louaraya.backend;

import cl.louaraya.backend.dto.ProductoRequest;
import cl.louaraya.backend.repository.ProductoRepository;
import cl.louaraya.backend.service.ProductoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductoApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ProductoRepository repo;
    @Autowired ProductoService service;
    @Autowired DataSource datasource;
    private static final String ADMIN = "/api/admin/productos";
    private ProductoRequest valido() {
        return new ProductoRequest("Tapiz de prueba", "Diseño ficticio para verificar la API", "Interiores", "Algodón",
            new BigDecimal("45"), new BigDecimal("70"), new BigDecimal("20000"), false, true);
    }
    private String json(Object valor) throws Exception { return mapper.writeValueAsString(valor); }
    private long crear() throws Exception {
        String respuesta = mvc.perform(post(ADMIN).with(httpBasic("admin-test", "SoloPruebas123!"))
            .contentType(MediaType.APPLICATION_JSON).content(json(valido())))
            .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn().getResponse().getContentAsString();
        return mapper.readTree(respuesta).get("id").asLong();
    }
    @Test void semillasConservanOchoIdsYDisponibilidadIndependiente() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(8))
            .andExpect(jsonPath("$[0].id").value(1)).andExpect(jsonPath("$[7].id").value(8));
        mvc.perform(get("/api/productos/6")).andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Cortina Macramé")).andExpect(jsonPath("$.disponible").value(false))
            .andExpect(jsonPath("$.publicado").value(true));
    }
    @Test void crearDevuelve201YProductoConsultableConIdNuevo() throws Exception {
        long id = crear(); assertTrue(id >= 9);
        mvc.perform(get("/api/productos/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Tapiz de prueba")).andExpect(jsonPath("$.precio").value(20000));
    }
    @Test void editarPrecioYSemillaRepetidaNoSobrescribe() throws Exception {
        mvc.perform(put(ADMIN + "/1").with(httpBasic("admin-test", "SoloPruebas123!"))
            .contentType(MediaType.APPLICATION_JSON).content(json(valido())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.precio").value(20000));
        try (var conexion = datasource.getConnection()) {
            ScriptUtils.executeSqlScript(conexion, new ClassPathResource("data.sql"));
        }
        assertEquals(8, repo.count());
        assertEquals(0, new BigDecimal("20000").compareTo(repo.findById(1L).orElseThrow().getPrecio()));
    }
    @Test void ocultarYRepublicarSinEliminarNiCambiarDisponibilidad() throws Exception {
        long id = crear();
        mvc.perform(patch(ADMIN + "/" + id + "/publicacion").with(httpBasic("admin-test", "SoloPruebas123!"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"publicado\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.publicado").value(false));
        mvc.perform(get("/api/productos/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/productos")).andExpect(jsonPath("$.length()").value(8));
        mvc.perform(get(ADMIN).with(httpBasic("admin-test", "SoloPruebas123!"))).andExpect(jsonPath("$.length()").value(9));
        assertTrue(repo.existsById(id));
        mvc.perform(patch(ADMIN + "/" + id + "/publicacion").with(httpBasic("admin-test", "SoloPruebas123!"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"publicado\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.disponible").value(false));
        mvc.perform(get("/api/productos/" + id)).andExpect(status().isOk());
    }
    @Test void operacionesAdministrativasExigenCredenciales() throws Exception {
        mvc.perform(get(ADMIN)).andExpect(status().isUnauthorized());
        mvc.perform(post(ADMIN).contentType(MediaType.APPLICATION_JSON).content(json(valido()))).andExpect(status().isUnauthorized());
        mvc.perform(put(ADMIN + "/1").contentType(MediaType.APPLICATION_JSON).content(json(valido()))).andExpect(status().isUnauthorized());
        mvc.perform(patch(ADMIN + "/1/publicacion").contentType(MediaType.APPLICATION_JSON).content("{\"publicado\":false}")).andExpect(status().isUnauthorized());
        mvc.perform(get(ADMIN).with(httpBasic("admin-test", "incorrecta"))).andExpect(status().isUnauthorized());
        mvc.perform(delete(ADMIN + "/1")).andExpect(status().isUnauthorized());
        mvc.perform(delete(ADMIN + "/1").with(httpBasic("admin-test", "incorrecta")))
            .andExpect(status().isUnauthorized());
    }
    @Test @WithMockUser(roles = "CLIENTE") void rolClienteEsRechazadoEnHttpYServicio() throws Exception {
        assertThrows(AccessDeniedException.class, () -> service.crear(valido()));
        assertThrows(AccessDeniedException.class, () -> service.archivar(1L));
        mvc.perform(get(ADMIN)).andExpect(status().isForbidden());
        mvc.perform(delete(ADMIN + "/1")).andExpect(status().isForbidden());
    }
    @Test void rechazaCamposInvalidosYJsonInesperado() throws Exception {
        mvc.perform(post(ADMIN).with(httpBasic("admin-test", "SoloPruebas123!")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\" \",\"precio\":-1}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.nombre").exists()).andExpect(jsonPath("$.errores.precio").exists());
        mvc.perform(patch(ADMIN + "/1/publicacion").with(httpBasic("admin-test", "SoloPruebas123!"))
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.publicado").exists());
        mvc.perform(post(ADMIN).with(httpBasic("admin-test", "SoloPruebas123!")).contentType(MediaType.APPLICATION_JSON)
            .content(json(valido()).replace("\"nombre\"", "\"campoAjeno\""))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/productos/no-numero")).andExpect(status().isBadRequest());
        mvc.perform(delete(ADMIN + "/no-numero").with(httpBasic("admin-test", "SoloPruebas123!")))
            .andExpect(status().isBadRequest());
    }
    @Test void inexistentesDevuelven404() throws Exception {
        mvc.perform(get("/api/productos/999999")).andExpect(status().isNotFound());
        mvc.perform(put(ADMIN + "/999999").with(httpBasic("admin-test", "SoloPruebas123!"))
            .contentType(MediaType.APPLICATION_JSON).content(json(valido()))).andExpect(status().isNotFound());
        mvc.perform(patch(ADMIN + "/999999/publicacion").with(httpBasic("admin-test", "SoloPruebas123!"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"publicado\":false}")).andExpect(status().isNotFound());
        mvc.perform(delete(ADMIN + "/999999").with(httpBasic("admin-test", "SoloPruebas123!")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }
    @Test void archivarConservaLaFilaYExcluyeElProductoDelCrud() throws Exception {
        var admin = httpBasic("admin-test", "SoloPruebas123!");
        String ruta = ADMIN + "/1";
        mvc.perform(delete(ruta).with(admin)).andExpect(status().isNoContent())
            .andExpect(content().string(""));

        mvc.perform(get("/api/productos/1")).andExpect(status().isNotFound());
        mvc.perform(get("/api/productos/2")).andExpect(status().isOk())
            .andExpect(jsonPath("$.disponible").value(true));
        mvc.perform(get("/api/productos")).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains(2, 3, 4, 5, 6, 7, 8)));
        mvc.perform(get(ADMIN).with(admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains(2, 3, 4, 5, 6, 7, 8)));

        mvc.perform(put(ruta).with(admin).contentType(MediaType.APPLICATION_JSON).content(json(valido())))
            .andExpect(status().isNotFound());
        mvc.perform(patch(ruta + "/publicacion").with(admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"publicado\":true}")).andExpect(status().isNotFound());
        mvc.perform(delete(ruta).with(admin)).andExpect(status().isNotFound());
        assertEquals(8, repo.count());
        assertEquals("Tapiz Bohemio", repo.findById(1L).orElseThrow().getNombre());
    }
    @Test void swaggerYOpenApiDocumentanSeguridadYEndpoints() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"))
            .andExpect(jsonPath("$.paths['/api/admin/productos'].post.security[0].basicAuth").isArray())
            .andExpect(jsonPath("$.paths['/api/admin/productos/{id}'].delete.security[0].basicAuth").isArray())
            .andExpect(jsonPath("$.paths['/api/admin/productos/{id}'].delete.responses['204']").exists())
            .andExpect(jsonPath("$.paths['/api/productos'].get").exists());
    }
    @Test void pruebasSoloUsanMemoriaYJava17() throws Exception {
        try (var c = datasource.getConnection()) { assertTrue(c.getMetaData().getURL().startsWith("jdbc:h2:mem:")); }
        assertEquals(17, Runtime.version().feature());
    }
}
