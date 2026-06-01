package com.matheusmelo.todolist.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * IT de CORS (spec 002 AC4.2–AC4.5 / PDR-0003;
 * security-conventions.md § CORS Default — allowlist explicita, nunca *).
 * Allowlist de teste fixada em src/test/resources/application.yml
 * (CORS_ALLOWED_ORIGINS=https://app.exemplo.test).
 */
class CorsConfigIT extends AbstractMongoIntegrationTest {

    private static final String ORIGEM_PERMITIDA = "https://app.exemplo.test";
    private static final String ORIGEM_NAO_PERMITIDA = "https://evil.exemplo.test";

    @Test
    void preflightDeOrigemNaAllowlistRecebeAllowOriginCorrespondente() throws Exception {
        mockMvc.perform(options("/tarefas")
                        .header("Origin", ORIGEM_PERMITIDA)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_PERMITIDA));
    }

    @Test
    void preflightDeOrigemForaDaAllowlistNaoRecebeAllowOrigin() throws Exception {
        mockMvc.perform(options("/tarefas")
                        .header("Origin", ORIGEM_NAO_PERMITIDA)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void allowOriginNuncaWildcard() throws Exception {
        mockMvc.perform(options("/tarefas")
                        .header("Origin", ORIGEM_PERMITIDA)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().string(
                        "Access-Control-Allow-Origin", Matchers.not("*")));
    }

    @Test
    void preflightExpoeMetodosExplicitosNaoWildcard() throws Exception {
        mockMvc.perform(options("/tarefas")
                        .header("Origin", ORIGEM_PERMITIDA)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().string(
                        "Access-Control-Allow-Methods", Matchers.not("*")))
                .andExpect(header().string(
                        "Access-Control-Allow-Methods", Matchers.allOf(
                                Matchers.containsString("GET"),
                                Matchers.containsString("POST"),
                                Matchers.containsString("PUT"),
                                Matchers.containsString("DELETE"))));
    }

    @Test
    void requisicaoRealDeOrigemPermitidaRecebeAllowOrigin() throws Exception {
        mockMvc.perform(get("/tarefas").header("Origin", ORIGEM_PERMITIDA))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_PERMITIDA));
    }
}
