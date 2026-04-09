package org.terra.incognita.ticketdigital.mercadona.model;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Representación de un ticket digital de una compra realizada en una tienda de Mercadona.
 *
 * Contiene la información fiscal, detalles de la tienda, fecha de compra, lista de artículos adquiridos,
 * importe pagado, información de la tarjeta bancaria y otros identificadores relacionados con el ticket.
 *
 * Esta clase es un registro inmutable que facilita la gestión de los datos del ticket generado.
 */
public record TicketMercadona(ShopData shopData, TicketHeader header, List<PurchasedItem> items,
                              BigDecimal pagadoEnEuros, String tarjetaBancaria, String nc, String aut, String aid, String arc) {

    private static final Logger logger = LoggerFactory.getLogger(TicketMercadona.class);

    /**
     * Calcula el precio pagado en este ticket
     *
     * @return Total en euros de los precios de los artículos.
     */
    public BigDecimal precioTotalEnEuros() {
        return items.stream()
                .map(PurchasedItem::precioTotal) // Obtenemos el precio de cada artículo.
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

        logger.debug("Parseando el ticket con data:\\n {}", ticketData);

        // Lo pasamos a un array de strings
        List<String> lines = ticketData.lines()
                // Prodiramos hacer aquí un procesamiento, como un trim(), pero de momento lo dejo tal cual
                .toList();

        // Parseo parte a parte
        int nCurrentLine = 0;  // TODO: esto lo tendrían que mantener los parseadores (o en el arrayList)
        ShopData shopData = ShopData.parse(0, lines);
        nCurrentLine += ShopData.EXPECTED_LINES;
        logger.info("Datos de la tienda parseados: {}", shopData);

        TicketHeader header = TicketHeader.parse(nCurrentLine,lines);
        nCurrentLine += TicketHeader.EXPECTED_LINES;
        logger.info("Datos de  la cabecera parseados: {}", header);

        // Salto lineas en blanco hasta que llego a la cabecera de los items (TODO: esto debería estar embebido en alguno de los parseadores)
        while (nCurrentLine < lines.size() && lines.get(nCurrentLine).isBlank()) {
            nCurrentLine++;
        }
        // Linea de cabecera de los items
        String line = lines.get(nCurrentLine);
        if ( !line.trim().matches("Descripción\\s*P. Unit\\s*Importe") ) {
            throw new ParseException("Invalid ticket format: expected 'Descripción                       P. Unit    Importe' header", nCurrentLine);
        }
        nCurrentLine++;
        logger.info("Cabecera de los items parseada: {}", line);

        List<PurchasedItem> items = new ArrayList<>();
        // Y ahora compruebo la lista de items
        boolean byUnit = false;
        boolean byWeight = false;
        while( !lines.get(nCurrentLine).trim().startsWith("TOTAL (€)")) {
            PurchasedItem result = null;
            // Primero vemos si es un producto vendido por unidades
            if ( (result = ItemByUnit.parse(nCurrentLine, lines)) != null) {
                items.add(result);
                nCurrentLine++;
            // Si no lo era pues probamos con producto al peso
            } else if ( (result = ItemByWeight.parse(nCurrentLine, lines)) != null) {
                items.add(result);
                nCurrentLine+=2;
            } else {
                throw new ParseException("Invalid ticket format: expected item line, found: " + lines.get(nCurrentLine), nCurrentLine);
            }
        }
        // Estamos en la linea del total
        logger.info("Total: {}", lines.get(nCurrentLine));


        return new TicketMercadona(shopData, header,items,
                null,null,null,null,null,null);
    }
    
    


}

