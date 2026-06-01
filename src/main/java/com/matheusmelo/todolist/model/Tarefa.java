package com.matheusmelo.todolist.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento de dominio de uma tarefa (colecao MongoDB "tarefas").
 *
 * <p>{@code id} e {@code dataCriacao} sao gerados/imutaveis do ponto de vista
 * do cliente (per plan § Modelo de dados; nunca aceitos como entrada — ver DTOs).
 */
@Document(collection = "tarefas")
public class Tarefa {

    @Id
    private String id;

    private String titulo;

    private String descricao;

    private StatusTarefa status;

    private Instant dataCriacao;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public StatusTarefa getStatus() {
        return status;
    }

    public void setStatus(StatusTarefa status) {
        this.status = status;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Instant dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
