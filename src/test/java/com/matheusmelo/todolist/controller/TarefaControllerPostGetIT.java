package com.matheusmelo.todolist.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * T16 (Red): POST e GET (lista + por id) — integracao MockMvc.
 */
class TarefaControllerPostGetIT extends AbstractMongoIntegrationTest {

    private String criarTarefa(String body) throws Exception {
        MvcResult res = mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return res.getResponse().getContentAsString();
    }

    @Test
    void postValidoRetorna201ComCorpoELocation() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Comprar leite\",\"descricao\":\"Integral\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.matchesPattern(".*/tarefas/.+")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.titulo").value("Comprar leite"))
                .andExpect(jsonPath("$.descricao").value("Integral"))
                .andExpect(jsonPath("$.status").value("pendente"))
                .andExpect(jsonPath("$.dataCriacao").isNotEmpty());
    }

    @Test
    void postComStatusInformadoPreserva() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"t\",\"status\":\"concluida\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("concluida"));
    }

    @Test
    void postInvalidoRetorna400ComCorpoDeErroPadrao() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("titulo")))
                .andExpect(jsonPath("$.path").value("/tarefas"));
    }

    @Test
    void getListaVaziaRetorna200PageResponseVazio() throws Exception {
        // Contrato paginado (ADR-0004 / spec 002 AC2.4): array -> $.content vazio.
        mockMvc.perform(get("/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getListaRetornaTarefas() throws Exception {
        // Contrato paginado (ADR-0004 / spec 002 AC2.1): $[0] -> $.content[0].
        criarTarefa("{\"titulo\":\"t1\"}");
        mockMvc.perform(get("/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("t1"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getPorIdExistenteRetorna200() throws Exception {
        String criada = criarTarefa("{\"titulo\":\"t1\"}");
        String id = com.jayway.jsonpath.JsonPath.read(criada, "$.id");
        mockMvc.perform(get("/tarefas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("t1"));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/tarefas/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/tarefas/nao-existe"));
    }
}
