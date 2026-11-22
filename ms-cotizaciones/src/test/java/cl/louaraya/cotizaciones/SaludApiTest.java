package cl.louaraya.cotizaciones;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaludApiTest {
    @Autowired MockMvc mvc;
    @Test void saludYDocumentacionPublica() throws Exception {
        mvc.perform(get("/api/salud")).andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("cotizaciones"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
    @Test void noExponeOperacionesNoDefinidas() throws Exception {
        mvc.perform(post("/api/admin/cotizaciones")).andExpect(status().isUnauthorized());
    }
}
