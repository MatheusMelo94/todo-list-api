package com.matheusmelo.todolist.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * IT da paginacao no GET /tarefas (spec 002 AC2.1/AC2.2/AC2.5/AC2.6; PDR-0004;
 * ADR-0004). Contrato paginado (PageResponse) e 400 para page/size invalidos.
 */
class TarefaControllerPaginacaoIT extends AbstractMongoIntegrationTest {

    private void criar(String titulo) throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + titulo + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getSemParamsRetornaPageResponsePrimeiraPagina() throws Exception {
        criar("t1");
        mockMvc.perform(get("/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getComPageESizeRefleteMetadados() throws Exception {
        for (int i = 0; i < 7; i++) {
            criar("t" + i);
        }
        mockMvc.perform(get("/tarefas").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getComPageNegativoRetorna400ShapePadrao() throws Exception {
        mockMvc.perform(get("/tarefas").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("page")))
                .andExpect(jsonPath("$.path").value("/tarefas"));
    }

    @Test
    void getComSizeNaoNumericoRetorna400ShapePadrao() throws Exception {
        mockMvc.perform(get("/tarefas").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("size")))
                .andExpect(jsonPath("$.path").value("/tarefas"));
    }

    @Test
    void getComSizeZeroRetorna400ShapePadrao() throws Exception {
        mockMvc.perform(get("/tarefas").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("size")));
    }

    @Test
    void paginacaoEstavelEntrePaginas() throws Exception {
        for (int i = 0; i < 6; i++) {
            criar("t" + i);
        }
        // Primeira pagina e segunda pagina nao se sobrepoem (ordem estavel).
        String pagina0 = mockMvc.perform(get("/tarefas").param("page", "0").param("size", "3"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String pagina1 = mockMvc.perform(get("/tarefas").param("page", "1").param("size", "3"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        java.util.List<String> ids0 = com.jayway.jsonpath.JsonPath.read(pagina0, "$.content[*].id");
        java.util.List<String> ids1 = com.jayway.jsonpath.JsonPath.read(pagina1, "$.content[*].id");
        org.assertj.core.api.Assertions.assertThat(ids0).doesNotContainAnyElementsOf(ids1);
    }
}
