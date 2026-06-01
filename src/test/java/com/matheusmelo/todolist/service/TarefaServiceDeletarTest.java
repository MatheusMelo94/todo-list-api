package com.matheusmelo.todolist.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.matheusmelo.todolist.exception.ResourceNotFoundException;
import com.matheusmelo.todolist.mapper.TarefaMapper;
import com.matheusmelo.todolist.repository.TarefaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * T15 (Red): deletar id existente vs. inexistente.
 */
class TarefaServiceDeletarTest {

    private TarefaRepository repository;
    private TarefaService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TarefaRepository.class);
        service = new TarefaService(
                repository, new TarefaMapper(), Mockito.mock(AuditoriaLogger.class));
    }

    @Test
    void idExistenteDeleta() {
        when(repository.existsById("id1")).thenReturn(true);

        service.deletar("id1");

        verify(repository).deleteById("id1");
    }

    @Test
    void idInexistenteLancaResourceNotFoundENaoDeleta() {
        when(repository.existsById("nao-existe")).thenReturn(false);

        assertThatThrownBy(() -> service.deletar("nao-existe"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).deleteById("nao-existe");
    }
}
