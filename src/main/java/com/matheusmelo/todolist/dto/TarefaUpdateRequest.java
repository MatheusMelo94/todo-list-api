package com.matheusmelo.todolist.dto;

import com.matheusmelo.todolist.model.StatusTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de substituicao completa (PUT /tarefas/{id}). {@code status}
 * obrigatorio (@NotNull): PUT sem status -> 400 (AC4.8, OQ-1 Resolved).
 * Nao aceita {@code id}/{@code dataCriacao} (imutaveis).
 */
public record TarefaUpdateRequest(

        @NotBlank(message = "titulo nao pode ser vazio")
        @Size(max = 200, message = "titulo excede 200 caracteres")
        String titulo,

        @Size(max = 2000, message = "descricao excede 2000 caracteres")
        String descricao,

        @NotNull(message = "status nao pode ser nulo")
        StatusTarefa status
) {
}
