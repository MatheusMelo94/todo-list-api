package com.matheusmelo.todolist.repository;

import com.matheusmelo.todolist.model.Tarefa;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repositorio Spring Data MongoDB para {@link Tarefa}.
 *
 * <p>Thin: apenas o CRUD padrao do {@link MongoRepository} (per
 * architecture-conventions.md § Layer Rules). Sem query customizada.
 */
public interface TarefaRepository extends MongoRepository<Tarefa, String> {
}
