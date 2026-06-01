package com.matheusmelo.todolist.service;

import com.matheusmelo.todolist.dto.TarefaCreateRequest;
import com.matheusmelo.todolist.dto.TarefaResponse;
import com.matheusmelo.todolist.dto.TarefaUpdateRequest;
import com.matheusmelo.todolist.exception.ResourceNotFoundException;
import com.matheusmelo.todolist.mapper.TarefaMapper;
import com.matheusmelo.todolist.model.StatusTarefa;
import com.matheusmelo.todolist.model.Tarefa;
import com.matheusmelo.todolist.repository.TarefaRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio de tarefas (per architecture-conventions.md § Layer Rules —
 * service orquestra a logica; controllers e repositories permanecem thin).
 */
@Service
public class TarefaService {

    private final TarefaRepository repository;
    private final TarefaMapper mapper;
    private final AuditoriaLogger auditoriaLogger;

    public TarefaService(
            TarefaRepository repository, TarefaMapper mapper, AuditoriaLogger auditoriaLogger) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditoriaLogger = auditoriaLogger;
    }

    /** Cria uma tarefa: gera dataCriacao e aplica status default PENDENTE se nulo (AC1.1, AC1.4). */
    public TarefaResponse criar(TarefaCreateRequest request) {
        Tarefa tarefa = mapper.toEntity(request);
        if (tarefa.getStatus() == null) {
            tarefa.setStatus(StatusTarefa.PENDENTE);
        }
        tarefa.setDataCriacao(Instant.now());
        Tarefa salva = repository.save(tarefa);
        auditoriaLogger.registrar("create", salva.getId());
        return mapper.toResponse(salva);
    }

    /** Lista todas as tarefas; lista vazia quando nao ha dados (AC2.2). */
    public List<TarefaResponse> listar() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** Retorna a tarefa por id ou lanca ResourceNotFoundException (AC3.2). */
    public TarefaResponse verPorId(String id) {
        return mapper.toResponse(buscarOuFalhar(id));
    }

    /**
     * Substituicao completa (PUT, AC4.1/AC4.7): aplica titulo/descricao/status
     * preservando id/dataCriacao; descricao ausente -> null. id inexistente ->
     * ResourceNotFoundException (AC4.2), nada salvo. O status chega garantido
     * nao-nulo pela validacao @NotNull (OQ-1 Resolved).
     */
    public TarefaResponse atualizar(String id, TarefaUpdateRequest request) {
        Tarefa tarefa = buscarOuFalhar(id);
        mapper.applyUpdate(tarefa, request);
        return mapper.toResponse(repository.save(tarefa));
    }

    /** Deleta a tarefa por id (AC5.1); id inexistente -> ResourceNotFoundException (AC5.2). */
    public void deletar(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Tarefa nao encontrada");
        }
        repository.deleteById(id);
    }

    private Tarefa buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa nao encontrada"));
    }
}
