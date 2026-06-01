package com.matheusmelo.todolist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.matheusmelo.todolist.dto.PageResponse;
import com.matheusmelo.todolist.dto.TarefaResponse;
import com.matheusmelo.todolist.mapper.TarefaMapper;
import com.matheusmelo.todolist.model.StatusTarefa;
import com.matheusmelo.todolist.model.Tarefa;
import com.matheusmelo.todolist.repository.TarefaRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Service listar(Pageable) com teto de size e defaults (spec 002
 * AC2.1/AC2.2/AC2.3/AC2.4; PDR-0004; ADR-0004).
 */
class TarefaServiceListarPaginadoTest {

    private TarefaRepository repository;
    private TarefaService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TarefaRepository.class);
        service = new TarefaService(
                repository, new TarefaMapper(), Mockito.mock(AuditoriaLogger.class));
    }

    private void retornaPaginaVazia() {
        when(repository.findAll(Mockito.<Pageable>any()))
                .thenAnswer(inv -> {
                    Pageable p = inv.getArgument(0);
                    return new PageImpl<>(List.of(), p, 0);
                });
    }

    @Test
    void aplicaDefaultsPage0Size20QuandoPageableNaoDefinido() {
        retornaPaginaVazia();

        service.listar(Pageable.unpaged());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void aplicaTetoDeSize100QuandoSizeAcimaDoLimite() {
        retornaPaginaVazia();

        service.listar(PageRequest.of(0, 500));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void preservaPageESizeValidosAbaixoDoTeto() {
        retornaPaginaVazia();

        service.listar(PageRequest.of(2, 5));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void colecaoVaziaRetornaPaginaVaziaSemErro() {
        retornaPaginaVazia();

        PageResponse<TarefaResponse> resp = service.listar(PageRequest.of(0, 20));

        assertThat(resp.content()).isEmpty();
        assertThat(resp.totalElements()).isEqualTo(0L);
    }

    @Test
    void mapeiaConteudoParaPageResponse() {
        Tarefa t = new Tarefa();
        t.setId("id1");
        t.setTitulo("t1");
        t.setStatus(StatusTarefa.PENDENTE);
        Page<Tarefa> page = new PageImpl<>(List.of(t), PageRequest.of(0, 20), 1);
        when(repository.findAll(Mockito.<Pageable>any())).thenReturn(page);

        PageResponse<TarefaResponse> resp = service.listar(PageRequest.of(0, 20));

        assertThat(resp.content()).hasSize(1);
        assertThat(resp.content().get(0).id()).isEqualTo("id1");
        assertThat(resp.totalElements()).isEqualTo(1L);
    }
}
