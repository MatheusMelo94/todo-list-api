package com.matheusmelo.todolist.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusmelo.todolist.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Escreve a resposta 429 (Too Many Requests) no shape padrao {@link ErrorResponse}
 * + header {@code Retry-After} (spec 002 AC1.2; OQ-1; security-conventions.md
 * § Error Responses, § Rate Limiting). Sem information disclosure.
 *
 * <p>Componente proprio (nao via GlobalExceptionHandler) porque o filtro roda fora
 * da cadeia do DispatcherServlet/@ControllerAdvice; o shape e mantido consistente
 * com o {@code GlobalExceptionHandler}.
 */
@Component
public class RateLimitResponseWriter {

    private final ObjectMapper objectMapper;

    public RateLimitResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeTooManyRequests(
            HttpServletRequest request, HttpServletResponse response, long retryAfterSegundos)
            throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Limite de requisicoes excedido",
                request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSegundos));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
