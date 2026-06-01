package com.matheusmelo.todolist.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.matheusmelo.todolist.model.StatusTarefa;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * T08 (Red): Bean Validation nos request DTOs.
 * Ambos: titulo @NotBlank/@Size(200), descricao @Size(2000).
 * Update: status @NotNull (PUT sem status -> violacao, AC4.8).
 * Create: status opcional (default no service).
 */
class TarefaRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static String tituloLongo() {
        return "a".repeat(201);
    }

    private static String descricaoLonga() {
        return "a".repeat(2001);
    }

    private boolean violou(Set<? extends ConstraintViolation<?>> violacoes, String campo) {
        return violacoes.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(campo));
    }

    // --- Create ---

    @Test
    void createComTituloEmBrancoViola() {
        var req = new TarefaCreateRequest("   ", "desc", StatusTarefa.PENDENTE);
        assertThat(violou(validator.validate(req), "titulo")).isTrue();
    }

    @Test
    void createComTituloAcima200Viola() {
        var req = new TarefaCreateRequest(tituloLongo(), "desc", StatusTarefa.PENDENTE);
        assertThat(violou(validator.validate(req), "titulo")).isTrue();
    }

    @Test
    void createComDescricaoAcima2000Viola() {
        var req = new TarefaCreateRequest("titulo", descricaoLonga(), StatusTarefa.PENDENTE);
        assertThat(violou(validator.validate(req), "descricao")).isTrue();
    }

    @Test
    void createComStatusNuloEhValido() {
        var req = new TarefaCreateRequest("titulo", "desc", null);
        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    void createValidoSemViolacoes() {
        var req = new TarefaCreateRequest("titulo", "desc", StatusTarefa.PENDENTE);
        assertThat(validator.validate(req)).isEmpty();
    }

    // --- Update ---

    @Test
    void updateComTituloEmBrancoViola() {
        var req = new TarefaUpdateRequest("   ", "desc", StatusTarefa.CONCLUIDA);
        assertThat(violou(validator.validate(req), "titulo")).isTrue();
    }

    @Test
    void updateComTituloAcima200Viola() {
        var req = new TarefaUpdateRequest(tituloLongo(), "desc", StatusTarefa.CONCLUIDA);
        assertThat(violou(validator.validate(req), "titulo")).isTrue();
    }

    @Test
    void updateComDescricaoAcima2000Viola() {
        var req = new TarefaUpdateRequest("titulo", descricaoLonga(), StatusTarefa.CONCLUIDA);
        assertThat(violou(validator.validate(req), "descricao")).isTrue();
    }

    @Test
    void updateComStatusNuloViola() {
        var req = new TarefaUpdateRequest("titulo", "desc", null);
        assertThat(violou(validator.validate(req), "status")).isTrue();
    }

    @Test
    void updateValidoSemViolacoes() {
        var req = new TarefaUpdateRequest("titulo", "desc", StatusTarefa.CONCLUIDA);
        assertThat(validator.validate(req)).isEmpty();
    }
}
