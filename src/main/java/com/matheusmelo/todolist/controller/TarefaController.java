package com.matheusmelo.todolist.controller;

import com.matheusmelo.todolist.dto.TarefaCreateRequest;
import com.matheusmelo.todolist.dto.TarefaResponse;
import com.matheusmelo.todolist.service.TarefaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Endpoints REST de tarefas (per architecture-conventions.md § Layer Rules —
 * controller thin: recebe/retorna DTO, nunca entidade, sem logica de negocio;
 * § API Contracts — @Valid na entrada).
 */
@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    private final TarefaService service;

    public TarefaController(TarefaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TarefaResponse> criar(
            @Valid @RequestBody TarefaCreateRequest request, UriComponentsBuilder uriBuilder) {
        TarefaResponse criada = service.criar(request);
        URI location = uriBuilder.path("/tarefas/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    public List<TarefaResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public TarefaResponse verPorId(@PathVariable String id) {
        return service.verPorId(id);
    }
}
