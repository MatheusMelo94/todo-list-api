package com.matheusmelo.todolist.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Emite um evento de auditoria estruturado (INFO) por mutacao bem-sucedida
 * (spec 002 AC3.x; PDR-0002). Componente fino injetavel no service — o service e
 * a unica camada que orquestra e detem o id pos-save (architecture-conventions.md
 * § Layer Rules).
 *
 * <p><b>Sem dados sensiveis:</b> registra apenas metadados (operacao, id, outcome,
 * timestamp). NUNCA recebe ou loga titulo/descricao nem corpo de requisicao
 * (security-conventions.md § Logging & Monitoring — never log full request bodies;
 * architecture-conventions.md § Logging — never log PII at INFO).
 */
@Component
public class AuditoriaLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaLogger.class);

    /**
     * Registra uma mutacao bem-sucedida.
     *
     * @param operacao create | update | delete
     * @param tarefaId id da tarefa afetada (existe pos-persistencia)
     */
    public void registrar(String operacao, String tarefaId) {
        log.info(
                "auditoria operacao={} tarefaId={} outcome=sucesso timestamp={}",
                operacao,
                tarefaId,
                Instant.now());
    }
}
