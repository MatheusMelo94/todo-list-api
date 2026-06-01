package com.matheusmelo.todolist.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import io.github.bucket4j.TimeMeter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * IT do 429 + Retry-After (spec 002 AC1.2/AC1.3; OQ-1; PDR-0001; ADR-0003).
 * Usa um {@link TimeMeter} controlavel (FakeTimeMeter) para testar o reset da
 * janela sem sleep real.
 */
@Import(RateLimit429IT.RelogioControlavelConfig.class)
class RateLimit429IT extends AbstractMongoIntegrationTest {

    // IP distinto por metodo: o RateLimitConfig e singleton no contexto (cacheado
    // entre metodos) e mantem o mapa de buckets; IPs separados isolam as cotas.
    private static final String IP_429 = "198.51.100.21";
    private static final String IP_HEADERS = "198.51.100.22";
    private static final String IP_NO_MUTATION = "198.51.100.23";
    private static final String IP_RESET = "198.51.100.24";

    /** Substitui o RateLimitConfig por um com relogio controlavel (compartilhado). */
    @TestConfiguration
    static class RelogioControlavelConfig {
        static final FakeTimeMeter RELOGIO = new FakeTimeMeter();

        @Bean
        RateLimitConfig rateLimitConfig() {
            RELOGIO.reset();
            return new RateLimitConfig(RELOGIO);
        }
    }

    /** TimeMeter com tempo manualmente avancavel (nanos). */
    static class FakeTimeMeter implements TimeMeter {
        private long nanos = 0L;

        void reset() {
            nanos = 0L;
        }

        void avancarSegundos(long segundos) {
            nanos += segundos * 1_000_000_000L;
        }

        @Override
        public long currentTimeNanos() {
            return nanos;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }
    }

    private static RequestPostProcessor comIp(String ip) {
        return req -> {
            req.setRemoteAddr(ip);
            return req;
        };
    }

    private void esgotarCota(String ip) throws Exception {
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(post("/tarefas")
                            .with(comIp(ip))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"titulo\":\"t" + i + "\"}"))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void requisicao61RetornaShapePadrao429ComRetryAfter() throws Exception {
        esgotarCota(IP_429);

        mockMvc.perform(post("/tarefas")
                        .with(comIp(IP_429))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"excedente\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/tarefas"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void resposta429CarregaHeadersDeSeguranca() throws Exception {
        esgotarCota(IP_HEADERS);

        mockMvc.perform(post("/tarefas")
                        .with(comIp(IP_HEADERS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"excedente\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void requisicaoBarradaNaoCriaTarefa() throws Exception {
        esgotarCota(IP_NO_MUTATION);
        // Barra a 61a.
        mockMvc.perform(post("/tarefas")
                        .with(comIp(IP_NO_MUTATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"excedente\"}"))
                .andExpect(status().isTooManyRequests());

        // Total de tarefas continua 60 (a barrada nao persistiu). Contrato paginado
        // (ADR-0004): le totalElements.
        mockMvc.perform(get("/tarefas").with(comIp(IP_NO_MUTATION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(60));
    }

    @Test
    void aposJanelaExpirarOMesmoIpVoltaAserAtendido() throws Exception {
        esgotarCota(IP_RESET);
        mockMvc.perform(post("/tarefas")
                        .with(comIp(IP_RESET))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"excedente\"}"))
                .andExpect(status().isTooManyRequests());

        // Avanca o relogio alem da janela de 60s.
        RelogioControlavelConfig.RELOGIO.avancarSegundos(61);

        mockMvc.perform(post("/tarefas")
                        .with(comIp(IP_RESET))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"pos-reset\"}"))
                .andExpect(status().isCreated());
    }
}
