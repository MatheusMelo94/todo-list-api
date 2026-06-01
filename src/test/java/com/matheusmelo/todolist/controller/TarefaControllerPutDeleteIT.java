package com.matheusmelo.todolist.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * T17 (Red): PUT (substituicao completa) e DELETE — integracao MockMvc.
 */
class TarefaControllerPutDeleteIT extends AbstractMongoIntegrationTest {

    private record Criada(String id, String dataCriacao) {
    }

    private Criada criar(String body) throws Exception {
        MvcResult res = mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        // Le o valor persistido (precisao de ms do Mongo) para comparacoes
        // consistentes de dataCriacao apos PUT (AC4.1), evitando comparar contra
        // a precisao de nanossegundos do Instant.now em memoria.
        MvcResult get = mockMvc.perform(get("/tarefas/" + id))
                .andExpect(status().isOk())
                .andReturn();
        return new Criada(
                id,
                com.jayway.jsonpath.JsonPath.read(get.getResponse().getContentAsString(), "$.dataCriacao"));
    }

    @Test
    void putValidoRetorna200ComIdEDataCriacaoIntactos() throws Exception {
        Criada c = criar("{\"titulo\":\"antigo\",\"descricao\":\"antiga\"}");

        mockMvc.perform(put("/tarefas/" + c.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"novo\",\"descricao\":\"nova\",\"status\":\"concluida\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.id()))
                .andExpect(jsonPath("$.dataCriacao").value(c.dataCriacao()))
                .andExpect(jsonPath("$.titulo").value("novo"))
                .andExpect(jsonPath("$.descricao").value("nova"))
                .andExpect(jsonPath("$.status").value("concluida"));
    }

    @Test
    void putComDescricaoAusenteRetorna200ComDescricaoNull() throws Exception {
        Criada c = criar("{\"titulo\":\"antigo\",\"descricao\":\"antiga\"}");

        mockMvc.perform(put("/tarefas/" + c.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"novo\",\"status\":\"pendente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value(Matchers.nullValue()));
    }

    @Test
    void putSemStatusRetorna400() throws Exception {
        Criada c = criar("{\"titulo\":\"antigo\"}");

        mockMvc.perform(put("/tarefas/" + c.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"novo\",\"descricao\":\"nova\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("status")))
                .andExpect(jsonPath("$.path").value("/tarefas/" + c.id()));
    }

    @Test
    void putComTituloInvalidoRetorna400() throws Exception {
        Criada c = criar("{\"titulo\":\"antigo\"}");

        mockMvc.perform(put("/tarefas/" + c.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"status\":\"pendente\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("titulo")));
    }

    @Test
    void putComStatusInvalidoRetorna400() throws Exception {
        Criada c = criar("{\"titulo\":\"antigo\"}");

        mockMvc.perform(put("/tarefas/" + c.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"novo\",\"status\":\"arquivada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/tarefas/nao-existe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"novo\",\"status\":\"pendente\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteExistenteRetorna204SemCorpo() throws Exception {
        Criada c = criar("{\"titulo\":\"t\"}");

        mockMvc.perform(delete("/tarefas/" + c.id()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void deleteSeguidoDeGetRetorna404() throws Exception {
        Criada c = criar("{\"titulo\":\"t\"}");
        mockMvc.perform(delete("/tarefas/" + c.id())).andExpect(status().isNoContent());
        mockMvc.perform(get("/tarefas/" + c.id())).andExpect(status().isNotFound());
    }

    @Test
    void deleteInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/tarefas/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
