package com.matheusmelo.todolist.dto;

import com.matheusmelo.todolist.model.StatusTarefa;
import java.time.Instant;

/**
 * Representacao de saida de uma tarefa. Expoe os campos gerados pelo
 * servidor ({@code id}, {@code dataCriacao}) — nunca aceitos como entrada.
 */
public record TarefaResponse(
        String id,
        String titulo,
        String descricao,
        StatusTarefa status,
        Instant dataCriacao
) {
}
