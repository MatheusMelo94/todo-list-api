package com.matheusmelo.todolist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
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
}
