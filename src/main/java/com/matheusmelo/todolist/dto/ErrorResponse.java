package com.matheusmelo.todolist.dto;

import java.time.Instant;

/**
 * Corpo de erro padrao da convencao
 * (per architecture-conventions.md § API Contracts — shape de erro padrao).
 *
 * <p>ADR-0001 (shape enxuto) foi REJEITADO: usa-se o shape completo, sem campo
 * dedicado de "campo que falhou" (a identificacao do campo vai em {@code message}).
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
