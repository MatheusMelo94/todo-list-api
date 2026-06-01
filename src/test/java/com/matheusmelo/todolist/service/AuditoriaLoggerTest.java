package com.matheusmelo.todolist.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit do AuditoriaLogger (spec 002 AC3.4/AC3.5; PDR-0002;
 * security-conventions.md § Logging & Monitoring). Captura os eventos de log via
 * ListAppender e verifica os 4 campos canonicos sem vazar titulo/descricao.
 */
class AuditoriaLoggerTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;
    private AuditoriaLogger auditoriaLogger;

    @BeforeEach
    void setUp() {
        auditoriaLogger = new AuditoriaLogger();
        logger = (Logger) LoggerFactory.getLogger(AuditoriaLogger.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void registraEventoCreateEmInfoComOsQuatroCampos() {
        auditoriaLogger.registrar("create", "id-123");

        assertThat(appender.list).hasSize(1);
        ILoggingEvent evento = appender.list.get(0);
        assertThat(evento.getLevel()).isEqualTo(Level.INFO);
        String mensagem = evento.getFormattedMessage();
        assertThat(mensagem)
                .contains("operacao=create")
                .contains("tarefaId=id-123")
                .contains("outcome=sucesso")
                .contains("timestamp=");
    }

    @Test
    void naoVazaTituloNemDescricaoNoEvento() {
        auditoriaLogger.registrar("update", "id-456");

        String mensagem = appender.list.get(0).getFormattedMessage();
        assertThat(mensagem)
                .doesNotContainIgnoringCase("titulo")
                .doesNotContainIgnoringCase("descricao");
    }

    @Test
    void registraOperacaoDeleteComIdAfetado() {
        auditoriaLogger.registrar("delete", "id-789");

        String mensagem = appender.list.get(0).getFormattedMessage();
        assertThat(mensagem)
                .contains("operacao=delete")
                .contains("tarefaId=id-789");
    }
}
