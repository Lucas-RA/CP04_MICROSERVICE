package br.com.fiap.cp01_api01;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.cp01_api01.model.Financa;
import br.com.fiap.cp01_api01.model.Futebol;
import br.com.fiap.cp01_api01.repository.FinancaRepository;
import br.com.fiap.cp01_api01.repository.FutebolRepository;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ApiCrudIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FinancaRepository financaRepository;

    @Autowired
    private FutebolRepository futebolRepository;

    @BeforeEach
    void cleanDatabase() {
        financaRepository.deleteAll();
        futebolRepository.deleteAll();
    }

    @Test
    void shouldExecuteFinancaCrud() throws Exception {
        mockMvc.perform(post("/api/v1/financas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emissor": "Tesouro Nacional",
                                  "taxa": 12.5,
                                  "risco": "baixo",
                                  "vencimento": "2030-01-01",
                                  "quantidade": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        Financa criada = financaRepository.findAll().get(0);

        mockMvc.perform(get("/api/v1/financas/{id}", criada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emissor").value("Tesouro Nacional"));

        mockMvc.perform(get("/api/v1/financas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(criada.getId()));

        mockMvc.perform(put("/api/v1/financas/{id}", criada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emissor": "Banco Central",
                                  "taxa": 10.0,
                                  "risco": "medio",
                                  "vencimento": "2031-01-01",
                                  "quantidade": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emissor").value("Banco Central"));

        mockMvc.perform(delete("/api/v1/financas/{id}", criada.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/financas/{id}", criada.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldExecuteFutebolCrud() throws Exception {
        mockMvc.perform(post("/api/v1/copa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ano": 2022,
                                  "capeao": "Argentina",
                                  "sede": "Catar",
                                  "vice": "Franca",
                                  "melhorJogador": "Lionel Messi"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        Futebol criada = futebolRepository.findAll().get(0);

        mockMvc.perform(get("/api/v1/copa/{id}", criada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capeao").value("Argentina"));

        mockMvc.perform(get("/api/v1/copa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(criada.getId()));

        mockMvc.perform(put("/api/v1/copa/{id}", criada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ano": 2022,
                                  "capeao": "Argentina",
                                  "sede": "Catar",
                                  "vice": "Franca",
                                  "melhorJogador": "Emiliano Martinez"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.melhorJogador").value("Emiliano Martinez"));

        mockMvc.perform(delete("/api/v1/copa/{id}", criada.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/financas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emissor": "",
                                  "taxa": -1,
                                  "risco": "",
                                  "vencimento": "",
                                  "quantidade": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldExposeOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/v1/financas']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/copa']").exists());
    }
}
