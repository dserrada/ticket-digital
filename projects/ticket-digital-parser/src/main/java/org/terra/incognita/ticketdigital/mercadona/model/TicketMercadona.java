package org.terra.incognita.ticketdigital.mercadona.model;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Representación de un ticket digital de una compra realizada en una tienda de Mercadona.
 *
 * Contiene la información fiscal, detalles de la tienda, fecha de compra, lista de artículos adquiridos,
 * importe pagado, información de la tarjeta bancaria y otros identificadores relacionados con el ticket.
 *
 * Esta clase es un registro inmutable que facilita la gestión de los datos del ticket generado.
 */
public record TicketMercadona(ShopData shopData, TicketHeader header, List<PurchasedItem> items,
                              Parking parking, BigDecimal total, BigDecimal amountPaidByCard,
                              VatBreakdown vatBreakdown, CardPayment cardPayment) {

    private static final Logger logger = LoggerFactory.getLogger(TicketMercadona.class);


    /**
     * Expresión regular para el precio total en el ticket.
     */
    public static final Pattern TOTAL_PRICE_REGEX_PATTERN = Pattern.compile("[\\s]*TOTAL \\(€\\)[\\s]*(?<total>\\d*,\\d{2})");

    /**
     * Expresión regular para el importe pagado con tarjeta bancaria.
     */
    public static final Pattern TARJETA_BANCARIA_REGEX_PATTERN = Pattern.compile("[\\s]*TARJETA BANCARIA[\\s]+(?<amount>\\d*,\\d{2})\\s*");

    /**
     * Expresión regular para el texto legal final del ticket (no se almacena).
     */
    public static final Pattern DISCLAIMER_REGEX_PATTERN = Pattern.compile("\\s*SE ADMITEN DEVOLUCIONES CON TICKET\\s*");

    /**
     * Expresiones regulares para el aviso opcional de donación al banco de alimentos (no se almacena).
     */
    public static final Pattern DONATION_NOTICE_FIRST_LINE_PATTERN = Pattern.compile("^\\s*DONACIÓN A BANCO DE\\s*$");
    public static final Pattern DONATION_NOTICE_SECOND_LINE_PATTERN = Pattern.compile("^\\s*ALIMENTOS NO REEMBOLSABLE\\s+\\d*,\\d{2}\\s*$");


    /**
     * Calcula el precio pagado en este ticket a partir de los artículos comprados.
     *
     * @return Total en euros de los precios de los artículos.
     */
    public BigDecimal itemsTotal() {
        return items.stream()
                .map(PurchasedItem::calculatedPrice) // Obtenemos el precio de cada artículo.
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
            return IndentPreservingTextStripper.extractText(document);
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
        BigDecimal total = BigDecimal.ZERO;
        while (status.hasNext()) {
            String currentLine = status.next();
            Matcher matcher = TOTAL_PRICE_REGEX_PATTERN.matcher(currentLine);
            if (matcher.matches()) {
                logger.info("Total: {}", currentLine);
                total = PurchasedItem.parseUnitPrice(matcher.group("total"));
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

        // --- Importe pagado con tarjeta (línea "TARJETA BANCARIA") ---
        if (!status.hasNext()) {
            throw new ParseException("Expected TARJETA BANCARIA line, got EOF", -1);
        }
        String tarjetaLine = status.next();
        Matcher tarjetaMatcher = TARJETA_BANCARIA_REGEX_PATTERN.matcher(tarjetaLine);
        if (!tarjetaMatcher.matches()) {
            throw new ParseException("Expected TARJETA BANCARIA line, found: " + tarjetaLine, status.previousIndex());
        }
        BigDecimal amountPaidByCard = PurchasedItem.parseUnitPrice(tarjetaMatcher.group("amount"));
        logger.info("Importe pagado con tarjeta: {}", amountPaidByCard);

        // --- Desglose de IVA ---
        skipBlankLines(status);
        VatBreakdown vatBreakdown = VatBreakdown.parse(status);
        logger.info("Desglose de IVA parseado: {}", vatBreakdown);

        // --- Pago con tarjeta (tarjeta enmascarada, N.C/AUT, AID/ARC, Importe) ---
        // Algunos tickets incluyen aquí un aviso de donación (p.ej. "DONACIÓN A BANCO DE
        // ALIMENTOS NO REEMBOLSABLE  5,00"); la donación en sí ya aparece como un artículo
        // normal en la lista de items, así que este aviso se descarta sin almacenarlo.
        skipBlankLines(status);
        skipDonationNotice(status);
        skipBlankLines(status);
        CardPayment cardPayment = CardPayment.parse(status);
        if (cardPayment == null) {
            throw new ParseException("Expected card payment block (TARJ. BANCARIA / N.C / AID)", status.nextIndex());
        }
        logger.info("Pago con tarjeta parseado: {}", cardPayment);

        // --- Disclaimer final (se consume pero no se almacena) ---
        skipBlankLines(status);
        if (!status.hasNext()) {
            throw new ParseException("Expected disclaimer line, got EOF", -1);
        }
        String disclaimerLine = status.next();
        if (!DISCLAIMER_REGEX_PATTERN.matcher(disclaimerLine).matches()) {
            throw new ParseException("Expected disclaimer line, found: " + disclaimerLine, status.previousIndex());
        }

        // No exigimos que el resto del fichero esté vacío: algunos tickets añaden texto
        // adicional tras el disclaimer (p.ej. avisos de parking como "DISPONE DE 20 MINUTOS")
        // que no forma parte de los datos del ticket y no necesitamos validar ni almacenar.

        TicketMercadona ticket = new TicketMercadona(shopData, header, items, parking,
                total, amountPaidByCard, vatBreakdown, cardPayment);
        // Comprobamos que el precio total coincide con el precio pagado, sino ocurre esto, algo ha ido muy mal
        if (ticket.itemsTotal().compareTo(ticket.total()) != 0) {
            throw new ParseException("El precio total del ticket no coincide con el precio pagado", -1);
        }
        // Comprobamos que el total, el importe pagado con tarjeta y el importe verificado son
        // consistentes entre sí. No comparamos con base+cuota de IVA: algunos tickets incluyen
        // artículos no sujetos a IVA (p.ej. una donación) que forman parte del total pero no
        // del desglose de IVA.
        if (ticket.total().compareTo(ticket.amountPaidByCard()) != 0
                || ticket.total().compareTo(ticket.cardPayment().amount()) != 0) {
            throw new ParseException(
                    "El total del ticket no coincide con el importe pagado con tarjeta o el importe verificado", -1);
        }
        return ticket;
    }

    /**
     * Salta líneas en blanco hasta encontrar una línea con contenido, dejando el
     * iterador posicionado justo antes de esa línea.
     *
     * @param status Estado del parseador con el iterador de líneas
     */
    private static void skipBlankLines(ParserStatusInfo status) {
        while (status.hasNext()) {
            String line = status.next();
            if (!line.isBlank()) {
                status.rollback(1);
                return;
            }
        }
    }

    /**
     * Salta el aviso opcional de donación al banco de alimentos (dos líneas), si está
     * presente. La donación en sí ya se ha parseado como un artículo normal de la lista
     * de items, así que este aviso se descarta sin almacenarlo.
     *
     * @param status Estado del parseador con el iterador de líneas
     */
    private static void skipDonationNotice(ParserStatusInfo status) throws ParseException {
        if (!status.hasNext()) return;
        String firstLine = status.next();
        if (!DONATION_NOTICE_FIRST_LINE_PATTERN.matcher(firstLine).matches()) {
            status.rollback(1);
            return;
        }
        if (!status.hasNext()) {
            throw new ParseException("Expected donation notice second line, got EOF", -1);
        }
        String secondLine = status.next();
        if (!DONATION_NOTICE_SECOND_LINE_PATTERN.matcher(secondLine).matches()) {
            throw new ParseException("Expected donation notice second line, found: " + secondLine, status.previousIndex());
        }
    }

}
