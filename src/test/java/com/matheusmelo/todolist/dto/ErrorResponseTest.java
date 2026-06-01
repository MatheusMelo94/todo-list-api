package com.matheusmelo.todolist.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * T09 (Red): shape do corpo de erro padrao da convencao
 * (timestamp, status, error, message, path — nomes em ingles).
 */
class ErrorResponseTest {

    @Test
    void possuiOsCincoCamposComNomesEmIngles() {
        var nomes = Arrays.stream(ErrorResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
        assertThat(nomes).containsExactly("timestamp", "status", "error", "message", "path");
    }

    @Test
    void exponeOsValores() {
        Instant agora = Instant.parse("2026-06-01T12:00:00Z");
        var err = new ErrorResponse(agora, 404, "Not Found", "Tarefa nao encontrada", "/tarefas/x");

        assertThat(err.timestamp()).isEqualTo(agora);
        assertThat(err.status()).isEqualTo(404);
        assertThat(err.error()).isEqualTo("Not Found");
        assertThat(err.message()).isEqualTo("Tarefa nao encontrada");
        assertThat(err.path()).isEqualTo("/tarefas/x");
    }
}
