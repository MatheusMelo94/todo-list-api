package com.matheusmelo.todolist.dto;

import com.matheusmelo.todolist.model.StatusTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de criacao (POST /tarefas). {@code status} opcional: quando nulo,
 * o service aplica o default PENDENTE (OQ-1 Resolved). Nao aceita
 * {@code id}/{@code dataCriacao} (gerados pelo servidor).
 */
public record TarefaCreateRequest(

        @NotBlank(message = "titulo nao pode ser vazio")
        @Size(max = 200, message = "titulo excede 200 caracteres")
        String titulo,

        @Size(max = 2000, message = "descricao excede 2000 caracteres")
        String descricao,

        StatusTarefa status
) {
}
