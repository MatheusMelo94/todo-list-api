package com.matheusmelo.todolist.service;

import com.matheusmelo.todolist.dto.PageResponse;
import com.matheusmelo.todolist.dto.TarefaCreateRequest;
import com.matheusmelo.todolist.dto.TarefaResponse;
import com.matheusmelo.todolist.dto.TarefaUpdateRequest;
import com.matheusmelo.todolist.exception.ResourceNotFoundException;
import com.matheusmelo.todolist.mapper.TarefaMapper;
import com.matheusmelo.todolist.model.StatusTarefa;
import com.matheusmelo.todolist.model.Tarefa;
import com.matheusmelo.todolist.repository.TarefaRepository;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio de tarefas (per architecture-conventions.md § Layer Rules —
 * service orquestra a logica; controllers e repositories permanecem thin).
 */
@Service
public class TarefaService {

    /** Defaults e teto de paginacao (spec 002 AC2.1/AC2.3; PDR-0004). */
    static final int PAGE_DEFAULT = 0;
    static final int SIZE_DEFAULT = 20;
    static final int SIZE_MAXIMO = 100;

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

    /**
     * Lista tarefas paginadas (spec 002 AC2.x; PDR-0004; ADR-0004). Aplica
     * defaults (page 0, size 20) e teto de size 100; colecao vazia retorna pagina
     * vazia (nao erro). Retorna o envelope {@link PageResponse} (nunca o Page).
     */
    public PageResponse<TarefaResponse> listar(Pageable pageable) {
        return mapper.toPageResponse(repository.findAll(normalizar(pageable)));
    }

    /** Aplica defaults (page 0, size 20) e clamp de size em {@value #SIZE_MAXIMO}. */
    private Pageable normalizar(Pageable pageable) {
        int page = pageable.isPaged() ? pageable.getPageNumber() : PAGE_DEFAULT;
        int size = pageable.isPaged() ? pageable.getPageSize() : SIZE_DEFAULT;
        int sizeEfetivo = Math.min(Math.max(size, 1), SIZE_MAXIMO);
        return PageRequest.of(page, sizeEfetivo);
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
        Tarefa salva = repository.save(tarefa);
        auditoriaLogger.registrar("update", salva.getId());
        return mapper.toResponse(salva);
    }

    /** Deleta a tarefa por id (AC5.1); id inexistente -> ResourceNotFoundException (AC5.2). */
    public void deletar(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Tarefa nao encontrada");
        }
        repository.deleteById(id);
        auditoriaLogger.registrar("delete", id);
    }

    private Tarefa buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa nao encontrada"));
    }
}
