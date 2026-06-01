package com.matheusmelo.todolist.dto;

import java.util.List;

/**
 * Envelope de resposta paginada (ADR-0004; PDR-0004; spec 002 AC2.1). Record
 * imutavel — contrato publico estavel que substitui o array puro de GET /tarefas.
 * Nao expoe o {@code org.springframework.data.domain.Page} na borda
 * (architecture-conventions.md § Layer Rules).
 *
 * @param content lista de itens da pagina (DTOs)
 * @param page numero da pagina atual (0-based)
 * @param size tamanho efetivo da pagina (apos o teto de 100)
 * @param totalElements total de elementos da colecao
 * @param totalPages total de paginas
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
