package com.matheusmelo.todolist.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit do envelope PageResponse<T> (ADR-0004; PDR-0004; spec 002 AC2.1).
 * Record imutavel com content + metadados; serializa com as chaves esperadas.
 */
class PageResponseTest {

    @Test
    void exponeContentEMetadados() {
        PageResponse<String> page =
                new PageResponse<>(List.of("a", "b"), 0, 20, 2L, 1);

        assertThat(page.content()).containsExactly("a", "b");
        assertThat(page.page()).isEqualTo(0);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(2L);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void serializaComAsChavesDoContrato() throws Exception {
        PageResponse<String> page =
                new PageResponse<>(List.of("x"), 1, 5, 6L, 2);

        String json = new ObjectMapper().writeValueAsString(page);

        assertThat(json)
                .contains("\"content\":[\"x\"]")
                .contains("\"page\":1")
                .contains("\"size\":5")
                .contains("\"totalElements\":6")
                .contains("\"totalPages\":2");
    }
}
