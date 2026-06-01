package com.matheusmelo.todolist.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de rate limiting restrito aos endpoints de ESCRITA de /tarefas
 * (POST/PUT/DELETE) — PDR-0001; spec 002 AC1.x. Leitura (GET) passa sem consumir
 * token (AC1.4). Consome 1 token por requisicao de escrita; ao esgotar, responde
 * 429 + Retry-After (T-RL-04).
 *
 * <p>Registrado na cadeia do Spring Security (ver SecurityConfig) APOS os header
 * writers, para que respostas 429 carreguem os headers de seguranca (AC4.1 vale
 * para toda resposta — per plan § Architecture Overview).
 *
 * <p>NAO e {@code @Component} (evita auto-registro como servlet filter global, que
 * o aplicaria fora da cadeia de seguranca e duplicaria a execucao). E instanciado
 * como bean e adicionado a cadeia em {@link SecurityConfig}.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> METODOS_DE_ESCRITA = Set.of("POST", "PUT", "DELETE");

    private final RateLimitConfig rateLimitConfig;
    private final RateLimitResponseWriter responseWriter;

    public RateLimitFilter(
            RateLimitConfig rateLimitConfig, RateLimitResponseWriter responseWriter) {
        this.rateLimitConfig = rateLimitConfig;
        this.responseWriter = responseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!isEscritaDeTarefas(request)) {
            chain.doFilter(request, response);
            return;
        }

        Bucket bucket = rateLimitConfig.resolveBucket(chave(request));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }

        // Esgotado: 429 + Retry-After; a requisicao barrada NAO segue a cadeia
        // (nenhuma mutacao ocorre).
        long retryAfterSegundos = nanosParaSegundos(probe.getNanosToWaitForRefill());
        responseWriter.writeTooManyRequests(request, response, retryAfterSegundos);
    }

    private boolean isEscritaDeTarefas(HttpServletRequest request) {
        return METODOS_DE_ESCRITA.contains(request.getMethod())
                && request.getRequestURI().startsWith("/tarefas");
    }

    /** Chave do bucket: IP + metodo + URI (contadores por IP e por endpoint). */
    private String chave(HttpServletRequest request) {
        return request.getRemoteAddr() + "|" + request.getMethod() + "|" + request.getRequestURI();
    }

    private long nanosParaSegundos(long nanos) {
        long segundos = nanos / 1_000_000_000L;
        // Arredonda para cima: nunca sugerir 0s quando ainda ha espera.
        return (nanos % 1_000_000_000L == 0) ? segundos : segundos + 1;
    }
}
