package com.matheusmelo.todolist.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.matheusmelo.todolist.model.StatusTarefa;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * T08 (Red): TarefaResponse expoe id/dataCriacao (saida do servidor).
 */
class TarefaResponseTest {

    @Test
    void exponeTodosOsCampos() {
        Instant agora = Instant.parse("2026-06-01T12:00:00Z");
        var resp = new TarefaResponse("id1", "titulo", "desc", StatusTarefa.PENDENTE, agora);

        assertThat(resp.id()).isEqualTo("id1");
        assertThat(resp.titulo()).isEqualTo("titulo");
        assertThat(resp.descricao()).isEqualTo("desc");
        assertThat(resp.status()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(resp.dataCriacao()).isEqualTo(agora);
    }
}
