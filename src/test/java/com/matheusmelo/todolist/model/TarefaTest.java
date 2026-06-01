package com.matheusmelo.todolist.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * T06 (Red): documento Tarefa mapeado na colecao "tarefas" com os campos
 * id, titulo, descricao, status (StatusTarefa), dataCriacao (Instant).
 */
class TarefaTest {

    @Test
    void documentoMapeadoNaColecaoTarefas() {
        Document document = Tarefa.class.getAnnotation(Document.class);
        assertThat(document).isNotNull();
        assertThat(document.collection()).isEqualTo("tarefas");
    }

    @Test
    void idAnotadoComoId() throws Exception {
        Field id = Tarefa.class.getDeclaredField("id");
        assertThat(id.getType()).isEqualTo(String.class);
        assertThat(id.getAnnotation(Id.class)).isNotNull();
    }

    @Test
    void camposComTiposCorretos() throws Exception {
        assertThat(Tarefa.class.getDeclaredField("titulo").getType()).isEqualTo(String.class);
        assertThat(Tarefa.class.getDeclaredField("descricao").getType()).isEqualTo(String.class);
        assertThat(Tarefa.class.getDeclaredField("status").getType()).isEqualTo(StatusTarefa.class);
        assertThat(Tarefa.class.getDeclaredField("dataCriacao").getType()).isEqualTo(Instant.class);
    }

    @Test
    void acessoresPreservamValores() {
        Instant agora = Instant.now();
        Tarefa tarefa = new Tarefa();
        tarefa.setId("abc");
        tarefa.setTitulo("Comprar leite");
        tarefa.setDescricao("Integral");
        tarefa.setStatus(StatusTarefa.PENDENTE);
        tarefa.setDataCriacao(agora);

        assertThat(tarefa.getId()).isEqualTo("abc");
        assertThat(tarefa.getTitulo()).isEqualTo("Comprar leite");
        assertThat(tarefa.getDescricao()).isEqualTo("Integral");
        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(tarefa.getDataCriacao()).isEqualTo(agora);
    }
}
