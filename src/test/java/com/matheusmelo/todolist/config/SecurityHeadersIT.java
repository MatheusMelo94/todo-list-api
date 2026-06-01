package com.matheusmelo.todolist.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * IT dos headers de seguranca (spec 002 AC4.1 / PDR-0003;
 * security-conventions.md § Default Headers). CSP e Permissions-Policy ficam
 * FORA (OQ-2 / Non-Goal #6 — API sem frontend).
 */
class SecurityHeadersIT extends AbstractMongoIntegrationTest {

    @Test
    void respostaCarregaHstsNosniffFrameOptionsEReferrerPolicy() throws Exception {
        mockMvc.perform(get("/tarefas"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Strict-Transport-Security",
                        Matchers.containsString("max-age=31536000")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string(
                        "Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void respostaNaoCarregaCspNemPermissionsPolicy() throws Exception {
        mockMvc.perform(get("/tarefas"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Security-Policy"))
                .andExpect(header().doesNotExist("Permissions-Policy"));
    }
}
