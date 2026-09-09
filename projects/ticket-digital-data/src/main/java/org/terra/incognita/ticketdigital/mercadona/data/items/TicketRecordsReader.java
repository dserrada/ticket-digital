package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.model.TicketMercadona;
import org.terra.incognita.ticketdigital.mercadona.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Punto único de lectura de los tickets de un directorio: busca los PDF, los parsea y
 * los convierte en {@link PurchasedItemRecord}, listos para generar un CSV, un XLSX o
 * cualquier otro análisis (p.ej. el índice de inflación).
 */
public class TicketRecordsReader {

    private static final Logger logger = LoggerFactory.getLogger(TicketRecordsReader.class);

    /**
     * @return los items comprados de todos los tickets de {@code ticketsDir}, ordenados de
     * más reciente a más antiguo, o {@code null} si el directorio no existe.
     */
    public static List<PurchasedItemRecord> readAll(Path ticketsDir) throws IOException {
        List<Path> files = FileUtils.searchInDir(ticketsDir);
        if (files == null) return null;

        return files.stream()
                .map(file -> {
                    try {
                        return TicketMercadona.parse(file);
                    } catch (UnsupportedOperationException e) {
                        logger.error("Todavía no podemos procesar el archivo " + file);
                        return null;
                    } catch (Exception e) {
                        logger.error("Error parsing " + file, e);
                        throw new RuntimeException("Error al procesar el archivo: " + file);
                    }
                })
                .filter(Objects::nonNull)
                .map(PurchasedItemRecord::fromTicket)
                .flatMap(List::stream)
                .sorted(PurchasedItemRecord.BY_DATE_DESCENDING)
                .toList();
    }
}
