package com.matheusmelo.todolist.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * IT do RateLimitFilter — escopo de escrita (spec 002 AC1.1/AC1.4; PDR-0001).
 * Ate 60 escritas/IP/60s passam; leitura nunca e bloqueada por este limite.
 */
class RateLimitFilterIT extends AbstractMongoIntegrationTest {

    // IPs distintos por metodo: o RateLimitConfig e singleton e mantem estado
    // in-memory entre testes; IPs separados garantem isolamento.
    private static final String IP_A = "203.0.113.11";
    private static final String IP_B = "203.0.113.12";
    private static final String IP_C = "203.0.113.13";

    @Test
    void ate60EscritasPorIpNaJanelaSaoProcessadas() throws Exception {
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(post("/tarefas")
                            .with(comIp(IP_A))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"titulo\":\"t" + i + "\"}"))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void getListaNuncaEBloqueadoPeloLimiteDeEscrita() throws Exception {
        // Esgota a cota de escrita do IP.
        for (int i = 0; i < 61; i++) {
            mockMvc.perform(post("/tarefas")
                    .with(comIp(IP_B))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"titulo\":\"t" + i + "\"}"));
        }
        // GET continua atendido para o mesmo IP.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/tarefas").with(comIp(IP_B)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void getPorIdNuncaEBloqueadoPeloLimiteDeEscrita() throws Exception {
        for (int i = 0; i < 70; i++) {
            mockMvc.perform(get("/tarefas/qualquer-id").with(comIp(IP_C)))
                    .andExpect(status().isNotFound());
        }
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor comIp(
            String ip) {
        return req -> {
            req.setRemoteAddr(ip);
            return req;
        };
    }
}
