package com.matheusmelo.todolist.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Fabrica e cache in-memory de buckets de rate limiting (ADR-0003; PDR-0001;
 * security-conventions.md § Rate Limiting). Um bucket por chave (IP+endpoint de
 * escrita): 60 tokens / janela fixa de 60s.
 *
 * <p>Armazenamento {@link ConcurrentHashMap} (thread-safe; estado por instancia —
 * aceitavel em localhost/Prototype, per PDR-0001 § Consequences). O {@link TimeMeter}
 * e injetavel para permitir testar o reset da janela com relogio controlavel
 * (T-RL-04) sem {@code sleep} real.
 */
@Component
public class RateLimitConfig {

    /** Limite da spec 002: 60 requisicoes por janela fixa de 60s, por chave. */
    static final long CAPACIDADE = 60;
    static final Duration JANELA = Duration.ofSeconds(60);

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final TimeMeter timeMeter;

    public RateLimitConfig() {
        this(TimeMeter.SYSTEM_MILLISECONDS);
    }

    public RateLimitConfig(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    /** Resolve (criando sob demanda) o bucket da chave; mesma chave reusa o bucket. */
    public Bucket resolveBucket(String chave) {
        return buckets.computeIfAbsent(chave, k -> novoBucket());
    }

    private Bucket novoBucket() {
        Bandwidth limite = Bandwidth.builder()
                .capacity(CAPACIDADE)
                .refillIntervally(CAPACIDADE, JANELA)
                .build();
        return Bucket.builder()
                .addLimit(limite)
                .withCustomTimePrecision(timeMeter)
                .build();
    }
}
