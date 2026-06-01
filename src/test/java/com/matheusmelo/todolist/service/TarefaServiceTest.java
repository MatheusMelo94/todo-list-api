package com.matheusmelo.todolist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.matheusmelo.todolist.exception.ResourceNotFoundException;
import com.matheusmelo.todolist.mapper.TarefaMapper;
import com.matheusmelo.todolist.model.StatusTarefa;
import com.matheusmelo.todolist.model.Tarefa;
import com.matheusmelo.todolist.repository.TarefaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * T13 (Red): criar (default status, dataCriacao gerada), listar, verPorId.
 */
class TarefaServiceTest {

    private TarefaRepository repository;
    private TarefaService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TarefaRepository.class);
        service = new TarefaService(
                repository, new TarefaMapper(), Mockito.mock(AuditoriaLogger.class));
        when(repository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void criarGeraDataCriacaoEDefaultPendenteQuandoStatusNulo() {
        var req = new com.matheusmelo.todolist.dto.TarefaCreateRequest("titulo", "desc", null);

        var resp = service.criar(req);

        assertThat(resp.status()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(resp.dataCriacao()).isNotNull();
    }

    @Test
    void criarPreservaStatusInformado() {
        var req = new com.matheusmelo.todolist.dto.TarefaCreateRequest(
                "titulo", "desc", StatusTarefa.CONCLUIDA);

        var resp = service.criar(req);

        assertThat(resp.status()).isEqualTo(StatusTarefa.CONCLUIDA);
        ArgumentCaptor<Tarefa> captor = ArgumentCaptor.forClass(Tarefa.class);
        Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDataCriacao()).isNotNull();
    }

    @Test
    void listarSemDadosRetornaListaVazia() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.listar()).isEmpty();
    }

    @Test
    void listarRetornaTodos() {
        Tarefa t = new Tarefa();
        t.setId("id1");
        t.setTitulo("titulo");
        t.setStatus(StatusTarefa.PENDENTE);
        when(repository.findAll()).thenReturn(List.of(t));

        var resultado = service.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo("id1");
    }

    @Test
    void verPorIdRetornaTarefa() {
        Tarefa t = new Tarefa();
        t.setId("id1");
        t.setTitulo("titulo");
        t.setStatus(StatusTarefa.PENDENTE);
        when(repository.findById("id1")).thenReturn(Optional.of(t));

        assertThat(service.verPorId("id1").id()).isEqualTo("id1");
    }

    @Test
    void verPorIdInexistenteLancaResourceNotFound() {
        when(repository.findById("nao-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verPorId("nao-existe"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
