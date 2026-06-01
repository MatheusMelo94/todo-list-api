package com.matheusmelo.todolist.controller;

import com.matheusmelo.todolist.dto.PageResponse;
import com.matheusmelo.todolist.dto.TarefaCreateRequest;
import com.matheusmelo.todolist.dto.TarefaResponse;
import com.matheusmelo.todolist.dto.TarefaUpdateRequest;
import com.matheusmelo.todolist.service.TarefaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Endpoints REST de tarefas (per architecture-conventions.md § Layer Rules —
 * controller thin: recebe/retorna DTO, nunca entidade, sem logica de negocio;
 * § API Contracts — @Valid na entrada).
 */
@RestController
@RequestMapping("/tarefas")
@Validated
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

    /**
     * Lista paginada (PDR-0004 / ADR-0004). page >= 0, size >= 1; o teto de size
     * (100) e aplicado no service. page/size invalidos -> 400 no shape padrao
     * (ConstraintViolation / TypeMismatch tratados no GlobalExceptionHandler).
     */
    @GetMapping
    public PageResponse<TarefaResponse> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page deve ser >= 0")
                    int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size deve ser >= 1")
                    int size) {
        return service.listar(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public TarefaResponse verPorId(@PathVariable String id) {
        return service.verPorId(id);
    }

    @PutMapping("/{id}")
    public TarefaResponse atualizar(
            @PathVariable String id, @Valid @RequestBody TarefaUpdateRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
