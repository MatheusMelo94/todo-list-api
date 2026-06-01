package com.matheusmelo.todolist.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

/**
 * Unit do RateLimitConfig (spec 002 AC1.5; PDR-0001; ADR-0003 — chave, armazenamento
 * in-memory, thread-safety). Buckets sao resolvidos por chave (IP+endpoint):
 * chaves iguais reusam o mesmo bucket; chaves distintas sao independentes.
 */
class RateLimitConfigTest {

    @Test
    void mesmaChaveReusaOMesmoBucket() {
        RateLimitConfig config = new RateLimitConfig();

        Bucket primeiro = config.resolveBucket("1.2.3.4|POST|/tarefas");
        Bucket segundo = config.resolveBucket("1.2.3.4|POST|/tarefas");

        assertThat(primeiro).isSameAs(segundo);
    }

    @Test
    void chavesDeIpsDistintosTemBucketsIndependentes() {
        RateLimitConfig config = new RateLimitConfig();

        Bucket ipA = config.resolveBucket("1.2.3.4|POST|/tarefas");
        Bucket ipB = config.resolveBucket("9.9.9.9|POST|/tarefas");

        assertThat(ipA).isNotSameAs(ipB);
    }

    @Test
    void chavesDeEndpointsDistintosTemBucketsIndependentes() {
        RateLimitConfig config = new RateLimitConfig();

        Bucket post = config.resolveBucket("1.2.3.4|POST|/tarefas");
        Bucket put = config.resolveBucket("1.2.3.4|PUT|/tarefas");

        assertThat(post).isNotSameAs(put);
    }

    @Test
    void bucketNovoComecaCom60TokensDisponiveis() {
        RateLimitConfig config = new RateLimitConfig();

        Bucket bucket = config.resolveBucket("1.2.3.4|POST|/tarefas");

        assertThat(bucket.getAvailableTokens()).isEqualTo(60);
    }
}
