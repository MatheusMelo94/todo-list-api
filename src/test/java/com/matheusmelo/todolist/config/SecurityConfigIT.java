package com.matheusmelo.todolist.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * IT da cadeia de seguranca (PDR-0003 / spec 002 AC4.6): o Spring Security entra
 * APENAS para headers + CORS — Non-Goal #1 (sem auth) permanece intacto. Toda
 * requisicao continua anonima (permitAll), nunca 401/403.
 */
class SecurityConfigIT extends AbstractMongoIntegrationTest {

    @Test
    void getTarefasAnonimoRetorna200NaoExigeAuth() throws Exception {
        mockMvc.perform(get("/tarefas"))
                .andExpect(status().isOk());
    }

    @Test
    void postTarefasAnonimoNaoRetorna401Nem403() throws Exception {
        // CSRF desabilitado: POST anonimo nao e bloqueado por token CSRF ausente.
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"t1\"}"))
                .andExpect(status().isCreated());
    }
}
