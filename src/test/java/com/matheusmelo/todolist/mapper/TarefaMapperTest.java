package com.matheusmelo.todolist.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.matheusmelo.todolist.dto.TarefaCreateRequest;
import com.matheusmelo.todolist.dto.TarefaResponse;
import com.matheusmelo.todolist.dto.TarefaUpdateRequest;
import com.matheusmelo.todolist.model.StatusTarefa;
import com.matheusmelo.todolist.model.Tarefa;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * T10 (Red): conversoes entidade <-> DTO nos dois sentidos.
 */
class TarefaMapperTest {

    private final TarefaMapper mapper = new TarefaMapper();

    @Test
    void toEntityMapeiaCamposSemIdNemDataCriacao() {
        var req = new TarefaCreateRequest("Comprar leite", "Integral", StatusTarefa.PENDENTE);

        Tarefa entidade = mapper.toEntity(req);

        assertThat(entidade.getTitulo()).isEqualTo("Comprar leite");
        assertThat(entidade.getDescricao()).isEqualTo("Integral");
        assertThat(entidade.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(entidade.getId()).isNull();
        assertThat(entidade.getDataCriacao()).isNull();
    }

    @Test
    void applyUpdatePreservaIdEDataCriacao() {
        Instant criacao = Instant.parse("2026-06-01T12:00:00Z");
        Tarefa existente = new Tarefa();
        existente.setId("id1");
        existente.setTitulo("antigo");
        existente.setDescricao("antiga");
        existente.setStatus(StatusTarefa.PENDENTE);
        existente.setDataCriacao(criacao);

        var req = new TarefaUpdateRequest("novo", "nova", StatusTarefa.CONCLUIDA);
        mapper.applyUpdate(existente, req);

        assertThat(existente.getId()).isEqualTo("id1");
        assertThat(existente.getDataCriacao()).isEqualTo(criacao);
        assertThat(existente.getTitulo()).isEqualTo("novo");
        assertThat(existente.getDescricao()).isEqualTo("nova");
        assertThat(existente.getStatus()).isEqualTo(StatusTarefa.CONCLUIDA);
    }

    @Test
    void applyUpdateComDescricaoAusenteDefineNull() {
        Tarefa existente = new Tarefa();
        existente.setId("id1");
        existente.setDescricao("antiga");
        existente.setStatus(StatusTarefa.PENDENTE);

        var req = new TarefaUpdateRequest("novo", null, StatusTarefa.CONCLUIDA);
        mapper.applyUpdate(existente, req);

        assertThat(existente.getDescricao()).isNull();
    }

    @Test
    void toResponseMapeiaTodosOsCampos() {
        Instant criacao = Instant.parse("2026-06-01T12:00:00Z");
        Tarefa entidade = new Tarefa();
        entidade.setId("id1");
        entidade.setTitulo("titulo");
        entidade.setDescricao("desc");
        entidade.setStatus(StatusTarefa.CONCLUIDA);
        entidade.setDataCriacao(criacao);

        TarefaResponse resp = mapper.toResponse(entidade);

        assertThat(resp.id()).isEqualTo("id1");
        assertThat(resp.titulo()).isEqualTo("titulo");
        assertThat(resp.descricao()).isEqualTo("desc");
        assertThat(resp.status()).isEqualTo(StatusTarefa.CONCLUIDA);
        assertThat(resp.dataCriacao()).isEqualTo(criacao);
    }
}
