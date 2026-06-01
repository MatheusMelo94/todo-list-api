package com.matheusmelo.todolist.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.matheusmelo.todolist.AbstractMongoIntegrationTest;
import com.matheusmelo.todolist.model.StatusTarefa;
import com.matheusmelo.todolist.model.Tarefa;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * T07 (Red): round-trip save + findById contra o Mongo embedded (base T04).
 */
class TarefaRepositoryIT extends AbstractMongoIntegrationTest {

    @Autowired
    private TarefaRepository tarefaRepository;

    @Test
    void salvaERecuperaTarefa() {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Comprar leite");
        tarefa.setDescricao("Integral");
        tarefa.setStatus(StatusTarefa.PENDENTE);
        tarefa.setDataCriacao(Instant.parse("2026-06-01T12:00:00Z"));

        Tarefa salva = tarefaRepository.save(tarefa);
        assertThat(salva.getId()).isNotBlank();

        Optional<Tarefa> recuperada = tarefaRepository.findById(salva.getId());
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getTitulo()).isEqualTo("Comprar leite");
        assertThat(recuperada.get().getDescricao()).isEqualTo("Integral");
        assertThat(recuperada.get().getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(recuperada.get().getDataCriacao())
                .isEqualTo(Instant.parse("2026-06-01T12:00:00Z"));
    }
}
