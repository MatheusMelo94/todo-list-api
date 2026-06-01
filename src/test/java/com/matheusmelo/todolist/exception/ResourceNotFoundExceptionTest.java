package com.matheusmelo.todolist.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * T11 (Red): excecao de dominio runtime com mensagem de dominio.
 */
class ResourceNotFoundExceptionTest {

    @Test
    void ehRuntimeException() {
        var ex = new ResourceNotFoundException("Tarefa nao encontrada");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void carregaMensagemDeDominio() {
        var ex = new ResourceNotFoundException("Tarefa nao encontrada");
        assertThat(ex.getMessage()).isEqualTo("Tarefa nao encontrada");
    }
}
