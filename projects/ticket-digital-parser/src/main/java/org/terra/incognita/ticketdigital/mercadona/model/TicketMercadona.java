package org.terra.incognita.ticketdigital.mercadona.model;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Representación de un ticket digital de una compra realizada en una tienda de Mercadona.
 *
 * Contiene la información fiscal, detalles de la tienda, fecha de compra, lista de artículos adquiridos,
 * importe pagado, información de la tarjeta bancaria y otros identificadores relacionados con el ticket.
 *
 * Esta clase es un registro inmutable que facilita la gestión de los datos del ticket generado.
 */
public record TicketMercadona(ShopData shopData, TicketHeader header, List<PurchasedItem> items,
                              Parking parking, BigDecimal pagadoEnEuros, String tarjetaBancaria, String nc, String aut, String aid, String arc) {

    private static final Logger logger = LoggerFactory.getLogger(TicketMercadona.class);

    /**
     * Calcula el precio pagado en este ticket
     *
     * @return Total en euros de los precios de los artículos.
     */
    public BigDecimal precioTotalEnEuros() {
        return items.stream()
                .map(PurchasedItem::precioCalculado) // Obtenemos el precio de cada artículo.
                .reduce(BigDecimal.ZERO, BigDecimal::add)  // Sumamos todos los precios de los artículos comprados
                .setScale(2, RoundingMode.UNNECESSARY);
    }


    /**
     * Parsea un ticket digital de Mercadona desde un archivo de texto o PDF.
     *
     * @param filePath Ruta al archivo que contiene los datos del ticket
     * @return Un objeto TicketMercadona con los datos parseados
     * @throws IOException    Si ocurre un error al leer el archivo
     * @throws ParseException Si el formato del ticket no es válido
     */
    public static TicketMercadona parse(Path filePath) throws IOException, ParseException {
        Objects.requireNonNull(filePath, "filePath");

        String fileName = filePath.getFileName().toString();
        String fileNameLower = fileName.toLowerCase();

        String ticketData;
        if (fileNameLower.endsWith(".pdf")) {
            ticketData = extractTextFromPdf(filePath);
        } else if (fileNameLower.endsWith(".txt")) {
            ticketData = Files.readString(filePath);
        } else {
            throw new ParseException("Invalid file extension: expected .txt or .pdf file, got " + fileName, -1);
        }
        return parse(ticketData);
    }

    /**
     * Extrae el contenido de texto de un archivo PDF.
     *
     * @param pdfPath Ruta al archivo PDF
     * @return Contenido de texto extraído del PDF
     * @throws IOException Si ocurre un error al leer el archivo PDF
     */
    private static String extractTextFromPdf(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public static TicketMercadona parse(String ticketData) throws IOException, ParseException {
        Objects.requireNonNull(ticketData, "ticketData");

        logger.debug("Parseando el ticket con data:\n {}", ticketData);

        // Parseo parte a parte
        ParserStatusInfo status = new ParserStatusInfo(ticketData);

        ShopData shopData = ShopData.parse(status);
        logger.info("Datos de la tienda parseados: {}", shopData);

        TicketHeader header = TicketHeader.parse(status);
        logger.info("Datos de  la cabecera parseados: {}", header);

        // Salto lineas en blanco hasta que llego a la cabecera de los items (TODO: esto debería estar embebido en alguno de los parseadores)
        while (status.hasNext()) {
            String l = status.next();
            if (l.isBlank()) {
                continue;
            }
            // Linea de cabecera de los items
            if (!l.trim().matches("Descripción\\s*P. Unit\\s*Importe")) {
                throw new ParseException("Invalid ticket format: expected 'Descripción                       P. Unit    Importe' header", status.previousIndex());
            }
            logger.info("Cabecera de los items parseada: {}", l);
            break;
        }

        List<PurchasedItem> items = new ArrayList<>();
        // Y ahora compruebo la lista de items
        Parking parking = null;
        while (status.hasNext()) {
            String currentLine = status.next();
            if (currentLine.trim().startsWith("TOTAL (€)")) {
                logger.info("Total: {}", currentLine);
                break;
            }
            status.rollback(1); // Volver atrás para que los parsers lean la línea

            PurchasedItem result = null;
            // Primero vemos si es info del parking por que se puede confundir con una itemByUnit
            if ((parking = Parking.parse(status)) != null) {
                // El parking ya avanza el iterador
                // Luego vemos si es un producto vendido por unidades
            } else if ((result = OneItemByUnit.parse(status)) != null) {
                items.add(result);
            } else if ((result = NItemsByUnit.parse(status)) != null) {
                items.add(result);
                // Si no lo era pues probamos con producto al peso
            } else if ((result = ItemByWeight.parse(status)) != null) {
                items.add(result);
            } else if ((result = FreshItemByWeight.parse(status)) != null) {
                items.add(result);
            } else {
                String errorLine = status.next();
                logger.error("Invalid ticket format: expected item line, found: [{}], fileNumber: {}", errorLine, status.previousIndex());
                throw new ParseException("Invalid ticket format: expected item line, found: " + errorLine, status.previousIndex());
            }
        }


        return new TicketMercadona(shopData, header,items, parking,
                null,null,null,null,null,null);
    }

}

