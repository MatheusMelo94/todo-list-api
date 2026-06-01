package com.matheusmelo.todolist.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;

/**
 * T05 (Red): contrato JSON do enum StatusTarefa em minuscula.
 * PENDENTE/CONCLUIDA <-> "pendente"/"concluida"; valor desconhecido falha.
 */
class StatusTarefaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializaPendenteComoMinuscula() throws Exception {
        assertThat(objectMapper.writeValueAsString(StatusTarefa.PENDENTE))
                .isEqualTo("\"pendente\"");
    }

    @Test
    void serializaConcluidaComoMinuscula() throws Exception {
        assertThat(objectMapper.writeValueAsString(StatusTarefa.CONCLUIDA))
                .isEqualTo("\"concluida\"");
    }

    @Test
    void deserializaPendente() throws Exception {
        assertThat(objectMapper.readValue("\"pendente\"", StatusTarefa.class))
                .isEqualTo(StatusTarefa.PENDENTE);
    }

    @Test
    void deserializaConcluida() throws Exception {
        assertThat(objectMapper.readValue("\"concluida\"", StatusTarefa.class))
                .isEqualTo(StatusTarefa.CONCLUIDA);
    }

    @Test
    void valorDesconhecidoNaoMapeiaParaEnumValido() {
        assertThatThrownBy(() -> objectMapper.readValue("\"arquivada\"", StatusTarefa.class))
                .isInstanceOf(InvalidFormatException.class);
    }
}
