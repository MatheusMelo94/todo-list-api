package com.matheusmelo.todolist.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status de uma tarefa. Contrato JSON externo em minuscula:
 * PENDENTE <-> "pendente", CONCLUIDA <-> "concluida"
 * (per architecture-conventions.md § API Contracts — contrato externo).
 */
public enum StatusTarefa {

    PENDENTE,
    CONCLUIDA;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
