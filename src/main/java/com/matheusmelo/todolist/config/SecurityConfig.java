package com.matheusmelo.todolist.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuracao do Spring Security restrita a headers de seguranca + CORS
 * (PDR-0003; per architecture-conventions.md § Package Layout — config/).
 *
 * <p><b>Non-Goal #1 intacto:</b> NAO ha autenticacao/autorizacao. A cadeia faz
 * {@code anyRequest().permitAll()}, desabilita CSRF (API stateless, sem sessao/
 * cookie de auth) e nao registra {@code formLogin}/{@code httpBasic}. Todo
 * endpoint permanece anonimo (spec 002 AC4.6).
 */
@Configuration
public class SecurityConfig {

    /**
     * Allowlist de origens de CORS via env (CORS_ALLOWED_ORIGINS), separada por
     * virgula. NUNCA '*' (per security-conventions.md § CORS Default). O default
     * aponta para localhost de dev — em prod a env e obrigatoria.
     */
    private final List<String> allowedOrigins;

    public SecurityConfig(
            @Value("${cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    @Bean
    public RateLimitFilter rateLimitFilter(
            RateLimitConfig rateLimitConfig, RateLimitResponseWriter responseWriter) {
        return new RateLimitFilter(rateLimitConfig, responseWriter);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, RateLimitFilter rateLimitFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                // RateLimitFilter apos o HeaderWriterFilter: respostas 429 carregam
                // os headers de seguranca (AC4.1 — toda resposta).
                .addFilterAfter(rateLimitFilter, HeaderWriterFilter.class)
                .headers(headers -> headers
                        // HSTS aplicado a toda resposta (AC4.1 — "toda resposta").
                        // requestMatcher(any) forca o header mesmo em requisicoes nao-HTTPS
                        // (localhost/testes); em producao o TLS termina no edge.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000)
                                .requestMatcher(request -> true))
                        // X-Content-Type-Options: nosniff e X-Frame-Options: DENY sao
                        // defaults do Spring Security; mantidos explicitos por clareza.
                        .contentTypeOptions(opts -> {})
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy
                                        .STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Fonte de configuracao de CORS: allowlist explicita de origens (via env),
     * metodos e headers explicitos — nunca wildcard (AC4.2–AC4.5; PDR-0003;
     * security-conventions.md § CORS Default e architecture-conventions.md § CORS).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "Origin"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
