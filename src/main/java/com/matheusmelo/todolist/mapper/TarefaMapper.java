package com.matheusmelo.todolist.mapper;

import com.matheusmelo.todolist.dto.PageResponse;
import com.matheusmelo.todolist.dto.TarefaCreateRequest;
import com.matheusmelo.todolist.dto.TarefaResponse;
import com.matheusmelo.todolist.dto.TarefaUpdateRequest;
import com.matheusmelo.todolist.model.Tarefa;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Conversao entidade <-> DTO (per architecture-conventions.md § Layer Rules —
 * mapper em todo boundary entidade<->DTO, nunca bypass).
 *
 * <p>{@code id}/{@code dataCriacao} sao definidos pelo service na criacao e
 * preservados no update; o mapper nunca os define a partir de entrada do cliente.
 */
@Component
public class TarefaMapper {

    /** Cria a entidade a partir do request de criacao; nao define id nem dataCriacao. */
    public Tarefa toEntity(TarefaCreateRequest request) {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(request.titulo());
        tarefa.setDescricao(request.descricao());
        tarefa.setStatus(request.status());
        return tarefa;
    }

    /**
     * Substituicao completa (PUT): aplica titulo/descricao/status do request
     * preservando id/dataCriacao da entidade existente. descricao ausente -> null.
     */
    public void applyUpdate(Tarefa tarefa, TarefaUpdateRequest request) {
        tarefa.setTitulo(request.titulo());
        tarefa.setDescricao(request.descricao());
        tarefa.setStatus(request.status());
    }

    public TarefaResponse toResponse(Tarefa tarefa) {
        return new TarefaResponse(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getStatus(),
                tarefa.getDataCriacao());
    }

    /**
     * Mapeia o {@link Page} do repositorio para o envelope {@link PageResponse}
     * (ADR-0004): content via {@link #toResponse}, metadados do Page. Nunca expoe
     * o Page na borda da API (§ Layer Rules).
     */
    public PageResponse<TarefaResponse> toPageResponse(Page<Tarefa> page) {
        List<TarefaResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
