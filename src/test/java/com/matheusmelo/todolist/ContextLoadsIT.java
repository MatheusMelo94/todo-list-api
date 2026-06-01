package com.matheusmelo.todolist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Teste-sentinela (T04): confirma que o contexto Spring sobe com o Mongo
 * embedded (flapdoodle) e que o {@link org.springframework.data.mongodb.core.MongoTemplate}
 * esta disponivel, sem Docker.
 */
class ContextLoadsIT extends AbstractMongoIntegrationTest {

    @Test
    void contextoSobeComMongoEmbedded() {
        assertThat(mongoTemplate).isNotNull();
        assertThat(mongoTemplate.getDb()).isNotNull();
    }
}
