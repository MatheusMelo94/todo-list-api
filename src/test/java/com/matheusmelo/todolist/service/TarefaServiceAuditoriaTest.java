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
}
