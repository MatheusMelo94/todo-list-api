package com.matheusmelo.todolist;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base para testes de integracao (web + Mongo).
 *
 * <p>Usa o Mongo embedded auto-configurado pelo flapdoodle (OQ-2 Resolved):
 * sem Testcontainers, sem URI dinamica, sem Docker. O contexto Spring sobe com
 * uma instancia Mongo embedded e {@code ./mvnw test} roda em qualquer ambiente.
 *
 * <p>Garante estado limpo entre testes via {@code dropCollection} no
 * {@link BeforeEach} (per plan § Estrategia de testes — Integration).
 */
@SpringBootTest
public abstract class AbstractMongoIntegrationTest {

    @Autowired
    protected MongoTemplate mongoTemplate;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    /**
     * Cadeia de filtros do Spring Security (headers, CORS) + demais filtros da app
     * (ex.: RateLimitFilter). Aplicada ao MockMvc para que os ITs exercitem a cadeia
     * de seguranca real (per plan § Architecture Overview — "a cadeia de seguranca e
     * exercida nos ITs"). Sem isto, o webAppContextSetup ignora os filtros.
     */
    @Autowired
    protected Filter springSecurityFilterChain;

    protected MockMvc mockMvc;

    @BeforeEach
    void resetState() {
        mongoTemplate.getDb().drop();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }
}
