package com.matheusmelo.todolist.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * IT da F-0009 (security-findings-001) — a chave do rate limit deve ser derivada da
 * ROTA LOGICA (template do endpoint), nao da URI concreta. Spec 002 AC1.1: limite
 * "por endpoint de escrita" (por metodo/rota logica), nao por instancia de recurso.
 *
 * <p>Antes da correcao, {@code getRequestURI()} (com o id de path) dava a cada id um
 * bucket proprio de 60 — um cliente contornava o limite de PUT/DELETE variando o id.
 * Estes testes esgotam a cota de DELETE/PUT usando ids DISTINTOS e esperam 429 ao
 * exceder o limite agregado da rota (IP + verbo + /tarefas/{id}).
 */
class RateLimitRotaLogicaIT extends AbstractMongoIntegrationTest {

    private static final String IP_DELETE = "203.0.113.41";
    private static final String IP_PUT = "203.0.113.42";

    @Test
    void deleteEmIdsDistintosCompartilhaACotaDaRotaLogica() throws Exception {
        // 60 DELETEs com ids DISTINTOS (cada um 404 — nao persiste nada) consomem a
        // cota agregada da rota DELETE /tarefas/{id}.
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(delete("/tarefas/id-inexistente-" + i).with(comIp(IP_DELETE)))
                    .andExpect(status().isNotFound());
        }

        // A 61a, com OUTRO id distinto, deve ser barrada: a cota e por rota logica,
        // nao por id. Antes da correcao isto retornava 404 (bucket proprio por id).
        mockMvc.perform(delete("/tarefas/id-inexistente-60").with(comIp(IP_DELETE)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void putEmIdsDistintosCompartilhaACotaDaRotaLogica() throws Exception {
        // 60 PUTs com ids DISTINTOS (cada um 404 — alvo inexistente) consomem a cota
        // agregada da rota PUT /tarefas/{id}.
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(put("/tarefas/id-inexistente-" + i)
                            .with(comIp(IP_PUT))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"titulo\":\"t" + i + "\",\"status\":\"pendente\"}"))
                    .andExpect(status().isNotFound());
        }

        // A 61a, com OUTRO id distinto, deve ser barrada (429), nao 404.
        mockMvc.perform(put("/tarefas/id-inexistente-60")
                        .with(comIp(IP_PUT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"excedente\",\"status\":\"pendente\"}"))
                .andExpect(status().isTooManyRequests());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder put(
            String uri) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(uri);
    }

    private static RequestPostProcessor comIp(String ip) {
        return req -> {
            req.setRemoteAddr(ip);
            return req;
        };
    }
}
