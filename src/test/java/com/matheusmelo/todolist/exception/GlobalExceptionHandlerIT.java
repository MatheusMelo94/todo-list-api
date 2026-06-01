package com.matheusmelo.todolist.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import com.matheusmelo.todolist.dto.TarefaCreateRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * T12 (Red): o GlobalExceptionHandler retorna o corpo de erro completo
 * (5 campos) para 400 (validacao + body ilegivel) e 404 (recurso ausente).
 *
 * <p>Usa um controller-stub apenas de teste para exercitar o advice sem
 * depender do TarefaController (T16). A mensagem de validacao identifica
 * o campo; nenhuma resposta vaza stack trace/DB/path de classe.
 */
@Import(GlobalExceptionHandlerIT.StubController.class)
class GlobalExceptionHandlerIT extends AbstractMongoIntegrationTest {

    @TestConfiguration
    @RestController
    @RequestMapping("/stub")
    static class StubController {

        @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
        public String criar(@Valid @RequestBody TarefaCreateRequest req) {
            return "ok";
        }

        @GetMapping("/ausente")
        public String ausente() {
            throw new ResourceNotFoundException("Tarefa nao encontrada");
        }
    }

    @Test
    void validacaoRetornaCorpoCompletoComCampoNaMensagem() throws Exception {
        mockMvc.perform(post("/stub")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"descricao\":\"d\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("titulo")))
                .andExpect(jsonPath("$.path").value("/stub"));
    }

    @Test
    void statusInvalidoNoBodyRetorna400() throws Exception {
        mockMvc.perform(post("/stub")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"t\",\"status\":\"arquivada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/stub"));
    }

    @Test
    void recursoAusenteRetornaCorpoCompleto404() throws Exception {
        mockMvc.perform(get("/stub/ausente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Tarefa nao encontrada"))
                .andExpect(jsonPath("$.path").value("/stub/ausente"));
    }

    @Test
    void respostaDeErroNaoVazaInternals() throws Exception {
        var resultado = mockMvc.perform(post("/stub")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        String body = resultado.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("Exception")
                .doesNotContain("at com.")
                .doesNotContain("mongo");
    }
}
