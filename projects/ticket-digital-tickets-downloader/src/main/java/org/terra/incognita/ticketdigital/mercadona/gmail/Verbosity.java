package org.terra.incognita.ticketdigital.mercadona.gmail;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sube el nivel de logging a DEBUG cuando se pasa {@code -v}/{@code --verbose}, para ver
 * el detalle de qué está haciendo la descarga en cada momento.
 */
final class Verbosity {

    private Verbosity() {
    }

    static void apply(boolean verbose) {
        if (verbose) {
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.DEBUG);
        }
    }
}
