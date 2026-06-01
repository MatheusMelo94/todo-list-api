package com.matheusmelo.todolist.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.matheusmelo.todolist.dto.TarefaCreateRequest;
import com.matheusmelo.todolist.dto.TarefaUpdateRequest;
import com.matheusmelo.todolist.exception.ResourceNotFoundException;
import com.matheusmelo.todolist.mapper.TarefaMapper;
import com.matheusmelo.todolist.model.StatusTarefa;
import com.matheusmelo.todolist.model.Tarefa;
import com.matheusmelo.todolist.repository.TarefaRepository;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Auditoria de mutacoes na camada de service (spec 002 AC3.1/AC3.2/AC3.3/AC3.6;
 * PDR-0002). Verifica que create/update/delete bem-sucedidos emitem exatamente um
 * evento com o id afetado, e que leitura nao audita; mutacao em id inexistente
 * (404) nao emite evento de sucesso.
 */
class TarefaServiceAuditoriaTest {

    private TarefaRepository repository;
    private AuditoriaLogger auditoriaLogger;
    private TarefaService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TarefaRepository.class);
        auditoriaLogger = Mockito.mock(AuditoriaLogger.class);
        service = new TarefaService(repository, new TarefaMapper(), auditoriaLogger);
    }

    @Test
    void criarEmiteUmEventoCreateComIdDaTarefaCriada() {
        when(repository.save(any(Tarefa.class))).thenAnswer(inv -> {
            Tarefa t = inv.getArgument(0);
            t.setId("id-novo");
            return t;
        });

        service.criar(new TarefaCreateRequest("titulo", "desc", null));

        verify(auditoriaLogger).registrar("create", "id-novo");
        Mockito.verifyNoMoreInteractions(auditoriaLogger);
    }

    @Test
    void atualizarEmiteUmEventoUpdateComIdAfetado() {
        Tarefa existente = tarefa("id-1");
        when(repository.findById("id-1")).thenReturn(Optional.of(existente));
        when(repository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

        service.atualizar("id-1", new TarefaUpdateRequest("novo", "desc", StatusTarefa.CONCLUIDA));

        verify(auditoriaLogger).registrar("update", "id-1");
        Mockito.verifyNoMoreInteractions(auditoriaLogger);
    }

    @Test
    void deletarEmiteUmEventoDeleteComIdAfetado() {
        when(repository.existsById("id-2")).thenReturn(true);

        service.deletar("id-2");

        verify(auditoriaLogger).registrar("delete", "id-2");
        Mockito.verifyNoMoreInteractions(auditoriaLogger);
    }

    @Test
    void verPorIdNaoEmiteAuditoriaDeMutacao() {
        when(repository.findById("id-3")).thenReturn(Optional.of(tarefa("id-3")));

        service.verPorId("id-3");

        Mockito.verifyNoInteractions(auditoriaLogger);
    }

    @Test
    void atualizarIdInexistenteNaoEmiteAuditoria() {
        when(repository.findById("nao-existe")).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() ->
                        service.atualizar("nao-existe",
                                new TarefaUpdateRequest("t", null, StatusTarefa.PENDENTE)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(auditoriaLogger, never()).registrar(any(), any());
    }

    @Test
    void deletarIdInexistenteNaoEmiteAuditoria() {
        when(repository.existsById("nao-existe")).thenReturn(false);

        Assertions.assertThatThrownBy(() -> service.deletar("nao-existe"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(auditoriaLogger, never()).registrar(any(), any());
    }

    private Tarefa tarefa(String id) {
        Tarefa t = new Tarefa();
        t.setId(id);
        t.setTitulo("titulo");
        t.setStatus(StatusTarefa.PENDENTE);
        return t;
    }
}
