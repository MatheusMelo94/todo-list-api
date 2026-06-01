package com.matheusmelo.todolist.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * QA E2E (pos-implementacao): cobertura de LIMITES e EDGE CASES atraves dos
 * endpoints reais {@code /tarefas}, fechando lacunas da suite do Engineer cuja
 * cobertura de tamanho/status invalido vivia apenas na camada de Bean Validation
 * (DTO) ou no stub controller do GlobalExceptionHandler.
 *
 * <p>Cobre via HTTP ponta-a-ponta:
 * <ul>
 *   <li>AC1.3  POST com status invalido -> 400 no endpoint real;</li>
 *   <li>AC1.5  POST com titulo > 200 -> 400 + contrato de erro;</li>
 *   <li>AC1.6  POST com descricao > 2000 -> 400 + contrato de erro;</li>
 *   <li>AC1.7  contrato de erro completo (5 campos) em violacoes de tamanho;</li>
 *   <li>AC4.5  PUT com titulo > 200 -> 400, nada alterado;</li>
 *   <li>AC4.6  PUT com descricao > 2000 -> 400, nada alterado;</li>
 *   <li>Limites EXATOS: titulo=200 e descricao=2000 sao aceitos (201);</li>
 *   <li>Exploratorio: status case-sensitive, campos desconhecidos, JSON malformado,
 *       descricao null explicita.</li>
 * </ul>
 * Mensagens de validacao seguem o wording dos DTOs ("titulo excede 200
 * caracteres", "descricao excede 2000 caracteres", "status ...").
 */
class TarefaControllerBoundaryIT extends AbstractMongoIntegrationTest {

    private static String repeat(int n) {
        return "a".repeat(n);
    }

    private String criar(String body) throws Exception {
        MvcResult res = mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id");
    }

    private long contar() throws Exception {
        // Contrato paginado (ADR-0004): le $.content em vez do array raiz.
        MvcResult res = mockMvc.perform(get("/tarefas"))
                .andExpect(status().isOk())
                .andReturn();
        java.util.List<?> lista = com.jayway.jsonpath.JsonPath.read(
                res.getResponse().getContentAsString(), "$.content");
        return lista.size();
    }

    // --- POST: limites de tamanho (AC1.5, AC1.6, AC1.7) ---

    @Test
    void postTituloAcima200Retorna400ContratoCompletoENadaPersiste() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + repeat(201) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("titulo")))
                .andExpect(jsonPath("$.path").value("/tarefas"));
        org.assertj.core.api.Assertions.assertThat(contar()).isZero();
    }

    @Test
    void postDescricaoAcima2000Retorna400ContratoCompletoENadaPersiste() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"t\",\"descricao\":\"" + repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("descricao")))
                .andExpect(jsonPath("$.path").value("/tarefas"));
        org.assertj.core.api.Assertions.assertThat(contar()).isZero();
    }

    // --- POST: limites EXATOS aceitos (boundary value: 200 e 2000) ---

    @Test
    void postTituloExatamente200Retorna201() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + repeat(200) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value(repeat(200)));
    }

    @Test
    void postDescricaoExatamente2000Retorna201() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"t\",\"descricao\":\"" + repeat(2000) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value(repeat(2000)));
    }

    // --- POST: status invalido no endpoint real (AC1.3) ---

    @Test
    void postStatusInvalidoRetorna400ENadaPersiste() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"t\",\"status\":\"arquivada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/tarefas"));
        org.assertj.core.api.Assertions.assertThat(contar()).isZero();
    }

    // --- PUT: limites de tamanho (AC4.5, AC4.6) — nada alterado ---

    @Test
    void putTituloAcima200Retorna400ENadaAltera() throws Exception {
        String id = criar("{\"titulo\":\"original\"}");
        mockMvc.perform(put("/tarefas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + repeat(201) + "\",\"status\":\"pendente\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("titulo")))
                .andExpect(jsonPath("$.path").value("/tarefas/" + id));
        // nada alterado: titulo permanece "original"
        mockMvc.perform(get("/tarefas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("original"));
    }

    @Test
    void putDescricaoAcima2000Retorna400ENadaAltera() throws Exception {
        String id = criar("{\"titulo\":\"original\",\"descricao\":\"desc-original\"}");
        mockMvc.perform(put("/tarefas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"novo\",\"descricao\":\"" + repeat(2001) + "\",\"status\":\"pendente\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("descricao")))
                .andExpect(jsonPath("$.path").value("/tarefas/" + id));
        // nada alterado: titulo e descricao originais preservados
        mockMvc.perform(get("/tarefas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("original"))
                .andExpect(jsonPath("$.descricao").value("desc-original"));
    }

    // --- Exploratorio (time-boxed): edge cases nao explicitos na spec ---

    @Test
    void postStatusCaseSensitiveRejeitaMaiusculas() throws Exception {
        // "PENDENTE" (maiusculo) nao deve ser aceito como "pendente" — enum case-sensitive no JSON.
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"t\",\"status\":\"PENDENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        org.assertj.core.api.Assertions.assertThat(contar()).isZero();
    }

    @Test
    void postComCampoDesconhecidoEhIgnoradoOuRejeitadoSemPersistirLixo() throws Exception {
        // Campo extra "id" no corpo nao deve sobrescrever o id gerado pelo sistema.
        MvcResult res = mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"t\",\"id\":\"forjado-123\",\"campoExtra\":\"x\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        org.assertj.core.api.Assertions.assertThat(id).isNotEqualTo("forjado-123");
    }

    @Test
    void postJsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/tarefas"));
    }

    @Test
    void postDescricaoNullExplicitaRetorna201ComDescricaoNull() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"t\",\"descricao\":null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.status").value("pendente"));
    }
}
