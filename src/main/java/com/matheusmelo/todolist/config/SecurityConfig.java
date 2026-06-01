package com.matheusmelo.todolist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
