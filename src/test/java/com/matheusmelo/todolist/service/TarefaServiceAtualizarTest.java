package com.matheusmelo.todolist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.matheusmelo.todolist.dto.TarefaUpdateRequest;
import com.matheusmelo.todolist.exception.ResourceNotFoundException;
import com.matheusmelo.todolist.mapper.TarefaMapper;
import com.matheusmelo.todolist.model.StatusTarefa;
import com.matheusmelo.todolist.model.Tarefa;
import com.matheusmelo.todolist.repository.TarefaRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * T14 (Red): atualizar com semantica PUT (substituicao completa).
 */
class TarefaServiceAtualizarTest {

    private TarefaRepository repository;
    private TarefaService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TarefaRepository.class);
        service = new TarefaService(repository, new TarefaMapper());
        when(repository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Tarefa existente() {
        Tarefa t = new Tarefa();
        t.setId("id1");
        t.setTitulo("antigo");
        t.setDescricao("antiga");
        t.setStatus(StatusTarefa.PENDENTE);
        t.setDataCriacao(Instant.parse("2026-06-01T12:00:00Z"));
        return t;
    }

    @Test
    void idInexistenteLancaResourceNotFoundENadaSalva() {
        when(repository.findById("nao-existe")).thenReturn(Optional.empty());
        var req = new TarefaUpdateRequest("novo", "nova", StatusTarefa.CONCLUIDA);

        assertThatThrownBy(() -> service.atualizar("nao-existe", req))
                .isInstanceOf(ResourceNotFoundException.class);
        Mockito.verify(repository, never()).save(any());
    }

    @Test
    void preservaIdEDataCriacaoEAplicaCampos() {
        when(repository.findById("id1")).thenReturn(Optional.of(existente()));
        var req = new TarefaUpdateRequest("novo", "nova", StatusTarefa.CONCLUIDA);

        var resp = service.atualizar("id1", req);

        assertThat(resp.id()).isEqualTo("id1");
        assertThat(resp.dataCriacao()).isEqualTo(Instant.parse("2026-06-01T12:00:00Z"));
        assertThat(resp.titulo()).isEqualTo("novo");
        assertThat(resp.descricao()).isEqualTo("nova");
        assertThat(resp.status()).isEqualTo(StatusTarefa.CONCLUIDA);
    }

    @Test
    void descricaoAusenteResultaEmNull() {
        when(repository.findById("id1")).thenReturn(Optional.of(existente()));
        var req = new TarefaUpdateRequest("novo", null, StatusTarefa.CONCLUIDA);

        service.atualizar("id1", req);

        ArgumentCaptor<Tarefa> captor = ArgumentCaptor.forClass(Tarefa.class);
        Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDescricao()).isNull();
    }
}
