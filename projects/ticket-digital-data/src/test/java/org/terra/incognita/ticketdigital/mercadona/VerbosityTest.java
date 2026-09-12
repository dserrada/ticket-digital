package org.terra.incognita.ticketdigital.mercadona;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerbosityTest {

    private static Logger root() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    @AfterEach
    void restoreDefaultLevel() {
        root().setLevel(Level.DEBUG);
    }

    @Test
    void conVerboseSubeElNivelDelLoggerRaizADebug() {
        root().setLevel(Level.INFO);

        Verbosity.apply(true);

        assertEquals(Level.DEBUG, root().getLevel());
    }

    @Test
    void sinVerboseNoCambiaElNivelDelLoggerRaiz() {
        root().setLevel(Level.INFO);

        Verbosity.apply(false);

        assertEquals(Level.INFO, root().getLevel());
    }
}
