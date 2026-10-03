package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.model.TicketHeader;
import org.terra.incognita.ticketdigital.mercadona.model.TicketMercadona;
import org.terra.incognita.ticketdigital.mercadona.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        // Se usa parallelStream porque TicketMercadona.parse() está documentado como thread-safe
        // para llamadas concurrentes, y con cientos de tickets el parseo de PDFs uno a uno es el
        // cuello de botella dominante de estos comandos.
        List<ParsedTicket> tickets = files.parallelStream()
                .map(file -> {
                    try {
                        return new ParsedTicket(file, TicketMercadona.parse(file));
                    } catch (UnsupportedOperationException e) {
                        logger.error("Todavía no podemos procesar el archivo " + file);
                        return null;
                    } catch (Exception e) {
                        // Un único ticket corrupto o con un formato no previsto no debe abortar la
                        // generación de CSV/XLSX/análisis para el resto de tickets: se registra y
                        // se omite, igual que el caso de formato no soportado de arriba.
                        logger.error("Error al parsear " + file + ", se omite", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        return distinct(tickets).stream()
                .map(PurchasedItemRecord::fromTicket)
                .flatMap(List::stream)
                .sorted(PurchasedItemRecord.BY_DATE_DESCENDING)
                .toList();
    }

    // El mismo ticket puede estar en el directorio en más de un fichero (p.ej. el downloader de
    // Gmail guardaba una segunda copia con el id del mensaje como sufijo): si no se descarta,
    // todas sus compras salen duplicadas. Un ticket se identifica por su nº de factura
    // simplificada junto con la fecha de compra (la fecha evita colisiones entre tickets
    // anonimizados, que comparten un nº de factura ficticio). Se conserva el primer fichero en
    // orden de ruta, para que el resultado no dependa del orden del parseo en paralelo.
    private static List<TicketMercadona> distinct(List<ParsedTicket> tickets) {
        Map<TicketKey, ParsedTicket> byKey = new LinkedHashMap<>();
        tickets.stream()
                .sorted(Comparator.comparing(ParsedTicket::file))
                .forEach(parsed -> {
                    TicketHeader header = parsed.ticket().header();
                    TicketKey key = new TicketKey(header.simplifiedInvoiceNumber(), header.purchaseDate());
                    ParsedTicket previous = byKey.putIfAbsent(key, parsed);
                    if (previous != null) {
                        logger.warn("{} es el mismo ticket que {} (factura {}, {}): se omite",
                                parsed.file(), previous.file(), key.invoiceNumber(), key.purchaseDate());
                    }
                });
        return byKey.values().stream().map(ParsedTicket::ticket).toList();
    }

    private record ParsedTicket(Path file, TicketMercadona ticket) {
    }

    private record TicketKey(String invoiceNumber, LocalDateTime purchaseDate) {
    }
}
