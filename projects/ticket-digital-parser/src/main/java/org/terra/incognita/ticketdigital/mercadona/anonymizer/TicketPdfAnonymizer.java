package org.terra.incognita.ticketdigital.mercadona.anonymizer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.model.CardPayment;
import org.terra.incognita.ticketdigital.mercadona.model.ShopData;
import org.terra.incognita.ticketdigital.mercadona.model.TicketHeader;
import org.terra.incognita.ticketdigital.mercadona.model.TicketMercadona;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Anonimiza los datos identificativos de un ticket de Mercadona en PDF: últimos 4 dígitos de
 * la tarjeta, dirección/localidad/teléfono de la tienda, fecha y hora de compra, código de
 * operación y número de factura simplificada; además elimina todas las imágenes incrustadas
 * (logo, icono de "atención al cliente" y código de barras).
 *
 * A diferencia de tapar visualmente esos datos, esta clase reescribe el propio texto dentro
 * del PDF, así que tampoco queda recuperable copiando el texto o volviendo a extraerlo con
 * herramientas como {@code ticket-digital-parser}.
 *
 * Las imágenes se eliminan en vez de anonimizarse por dos motivos distintos: el logotipo es
 * una marca registrada de Mercadona y no aporta nada al parser (que solo lee texto); y el
 * código de barras, al ser una imagen, sigue codificando en sus propios píxeles el valor
 * original de la factura simplificada aunque el texto ya se haya anonimizado, así que hay
 * que quitarlo para que no siga filtrando ese dato.
 *
 * Se dejan intactos los datos de los productos, importes, IVA, y AUT/ARC/AID/N.C. de la
 * tarjeta: no identifican al comprador y conservarlos permite seguir usando el PDF anonimizado
 * como fixture de test.
 *
 * El valor real de cada campo se obtiene parseando el ticket con {@link TicketMercadona#parse}
 * (el mismo parser de producción), en vez de adivinarlo con expresiones regulares sobre el PDF:
 * así la búsqueda-y-sustitución en el texto es literal, no una heurística nueva que mantener.
 */
public final class TicketPdfAnonymizer {

    private static final Logger logger = LoggerFactory.getLogger(TicketPdfAnonymizer.class);

    private static final String FAKE_ADDRESS = "CL EJEMPLO, 1";
    private static final String FAKE_LOCALITY = "CIUDAD FICTICIA";
    private static final String FAKE_DATE_TIME = "01/01/2000 00:00";

    /**
     * Anonimiza un PDF de ticket y escribe el resultado en otro fichero.
     *
     * @param input  Ruta del PDF original (no se modifica)
     * @param output Ruta donde se escribe el PDF anonimizado
     * @return Cuántos de los campos objetivo (tarjeta, tienda, fecha, OP, factura) se
     *         localizaron y sustituyeron realmente en el contenido del PDF
     */
    public int anonymize(Path input, Path output) throws IOException, ParseException {
        TicketMercadona ticket = TicketMercadona.parse(input);
        List<Redaction> fixedLengthRedactions = buildFixedLengthRedactions(ticket);
        List<Redaction> variableLengthRedactions = buildVariableLengthRedactions(ticket);

        try (PDDocument document = Loader.loadPDF(input.toFile())) {
            int applied = 0;
            for (PDPage page : document.getPages()) {
                applied += anonymizePage(document, page, fixedLengthRedactions, variableLengthRedactions);
            }
            int expected = fixedLengthRedactions.size() + variableLengthRedactions.size();
            if (applied < expected) {
                logger.warn("Solo se han podido anonimizar {} de {} campos objetivo en {}", applied, expected, input);
            }
            document.save(output.toFile());
            return applied;
        }
    }

    private static List<Redaction> buildFixedLengthRedactions(TicketMercadona ticket) {
        ShopData shop = ticket.shopData();
        TicketHeader header = ticket.header();
        CardPayment card = ticket.cardPayment();

        List<Redaction> redactions = new ArrayList<>();
        redactions.add(sameLengthDigits(shop.postalCode()));
        redactions.add(sameLengthDigits(shop.phoneNumber()));
        redactions.add(sameLengthDigits(header.operationCode()));
        redactions.add(sameLengthDigits(card.lastFourDigits()));
        redactions.add(new Redaction(
                header.simplifiedInvoiceNumber(), header.simplifiedInvoiceNumber().replaceAll("\\d", "0")));
        redactions.add(new Redaction(
                header.purchaseDate().format(TicketHeader.MERCADONA_DATE_TIME_FORMAT), FAKE_DATE_TIME));
        return redactions;
    }

    private static List<Redaction> buildVariableLengthRedactions(TicketMercadona ticket) {
        ShopData shop = ticket.shopData();
        List<Redaction> redactions = new ArrayList<>();
        redactions.add(new Redaction(shop.address(), FAKE_ADDRESS));
        redactions.add(new Redaction(shop.state(), FAKE_LOCALITY));
        return redactions;
    }

    private static Redaction sameLengthDigits(String original) {
        return new Redaction(original, "0".repeat(original.length()));
    }

    private int anonymizePage(PDDocument document, PDPage page, List<Redaction> fixedLengthRedactions,
                               List<Redaction> variableLengthRedactions) throws IOException {
        PDResources resources = page.getResources();
        Set<COSName> imagesToRemove = collectImageXObjectNames(resources);

        List<Object> originalTokens = new PDFStreamParser(page).parse();
        List<TextRun> runs = new ArrayList<>();
        StringBuilder globalText = new StringBuilder();
        List<Object> filteredTokens = new ArrayList<>();
        decodeAndFilterTokens(resources, originalTokens, imagesToRemove, runs, globalText, filteredTokens);

        int applied = 0;
        for (Redaction redaction : fixedLengthRedactions) {
            applied += applySameLengthRedaction(runs, globalText.toString(), redaction);
        }
        for (Redaction redaction : variableLengthRedactions) {
            applied += applyVariableLengthRedaction(runs, redaction);
        }

        for (TextRun run : runs) {
            if (run.modified) {
                setValue(run.cosString, run.font.encode(run.text.toString()));
            }
        }

        removeImageResources(resources, imagesToRemove);
        writeContent(document, page, filteredTokens);
        return applied;
    }

    private static Set<COSName> collectImageXObjectNames(PDResources resources) throws IOException {
        Set<COSName> names = new HashSet<>();
        if (resources == null) {
            return names;
        }
        for (COSName name : resources.getXObjectNames()) {
            if (resources.isImageXObject(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private static void removeImageResources(PDResources resources, Set<COSName> imagesToRemove) {
        if (resources == null || imagesToRemove.isEmpty()) {
            return;
        }
        COSDictionary xObjectDictionary = resources.getCOSObject().getCOSDictionary(COSName.XOBJECT);
        if (xObjectDictionary == null) {
            return;
        }
        for (COSName name : imagesToRemove) {
            xObjectDictionary.removeItem(name);
        }
    }

    /**
     * Recorre los tokens del content stream una sola vez: decodifica los operandos de texto de
     * Tj/TJ para poder buscar y sustituir después (igual que antes), y de paso construye una
     * copia filtrada de los tokens en la que se han eliminado las llamadas "Do" a las imágenes
     * de {@code imagesToRemove} (el resto de tokens, incl. los COSString que luego se mutan in
     * situ para las redacciones de texto, se copian tal cual).
     */
    private static void decodeAndFilterTokens(PDResources resources, List<Object> tokens, Set<COSName> imagesToRemove,
                                               List<TextRun> runs, StringBuilder globalText, List<Object> filteredTokens) throws IOException {
        PDFont currentFont = null;
        List<Object> operands = new ArrayList<>();
        for (Object token : tokens) {
            if (!(token instanceof Operator operator)) {
                operands.add(token);
                continue;
            }
            boolean drop = false;
            switch (operator.getName()) {
                case "Tf" -> currentFont = resolveFont(resources, operands);
                case "Tj" -> registerOperand(operands.isEmpty() ? null : operands.get(0), currentFont, runs, globalText);
                case "TJ" -> registerArrayOperand(operands.isEmpty() ? null : operands.get(0), currentFont, runs, globalText);
                case "Do" -> drop = !operands.isEmpty() && operands.get(0) instanceof COSName xObjectName
                        && imagesToRemove.contains(xObjectName);
                default -> { }
            }
            if (!drop) {
                filteredTokens.addAll(operands);
                filteredTokens.add(operator);
            }
            operands.clear();
        }
    }

    /**
     * Sustituye, en todo el documento, cada aparición literal de {@code redaction.original()}
     * carácter a carácter por {@code redaction.replacement()} (misma longitud). Al ser una
     * sustitución en el sitio, funciona igual si el texto original está en un único operando de
     * texto o repartido entre varios (habitual con ajustes de kerning en un TJ).
     */
    private static int applySameLengthRedaction(List<TextRun> runs, String globalText, Redaction redaction) {
        if (redaction.original().isEmpty() || redaction.original().length() != redaction.replacement().length()) {
            throw new IllegalArgumentException("Redacción de longitud fija con longitudes distintas: " + redaction);
        }
        int matches = 0;
        int fromIndex = 0;
        int index;
        while ((index = globalText.indexOf(redaction.original(), fromIndex)) >= 0) {
            markSameLengthReplacement(runs, index, index + redaction.original().length(), redaction.replacement());
            fromIndex = index + redaction.original().length();
            matches++;
        }
        return matches > 0 ? 1 : 0;
    }

    private static void markSameLengthReplacement(List<TextRun> runs, int globalStart, int globalEnd, String replacement) {
        for (TextRun run : runs) {
            int runEnd = run.globalStart + run.text.length();
            int overlapStart = Math.max(globalStart, run.globalStart);
            int overlapEnd = Math.min(globalEnd, runEnd);
            if (overlapStart >= overlapEnd) {
                continue;
            }
            for (int global = overlapStart; global < overlapEnd; global++) {
                int localIndex = global - run.globalStart;
                run.text.setCharAt(localIndex, replacement.charAt(global - globalStart));
            }
            run.modified = true;
        }
    }

    /**
     * Sustituye {@code redaction.original()} por {@code redaction.replacement()} (pueden tener
     * longitudes distintas), buscándolo dentro de un único {@link TextRun}: no se reparte entre
     * varios porque, a diferencia de una columna de precios, estas líneas (dirección, localidad)
     * no necesitan alineación y se dibujan normalmente como una sola cadena de texto.
     * Si no se encuentra en ningún TextRun completo, se deja sin anonimizar y se avisa por log
     * en vez de arriesgarse a corromper el contenido.
     */
    private static int applyVariableLengthRedaction(List<TextRun> runs, Redaction redaction) {
        for (TextRun run : runs) {
            int index = run.text.indexOf(redaction.original());
            if (index < 0) {
                continue;
            }
            while (index >= 0) {
                run.text.replace(index, index + redaction.original().length(), redaction.replacement());
                index = run.text.indexOf(redaction.original(), index + redaction.replacement().length());
            }
            run.modified = true;
            return 1;
        }
        logger.warn("No se ha podido localizar '{}' en un único fragmento de texto del PDF; se deja sin anonimizar",
                redaction.original());
        return 0;
    }

    private static PDFont resolveFont(PDResources resources, List<Object> operands) throws IOException {
        if (resources == null || operands.isEmpty() || !(operands.get(0) instanceof COSName fontName)) {
            return null;
        }
        return resources.getFont(fontName);
    }

    private static void registerOperand(Object operand, PDFont font, List<TextRun> runs, StringBuilder globalText) throws IOException {
        if (operand instanceof COSString str) {
            registerRun(str, font, runs, globalText);
        }
    }

    private static void registerArrayOperand(Object operand, PDFont font, List<TextRun> runs, StringBuilder globalText) throws IOException {
        if (!(operand instanceof COSArray array)) {
            return;
        }
        for (COSBase element : array) {
            if (element instanceof COSString str) {
                registerRun(str, font, runs, globalText);
            }
        }
    }

    private static void registerRun(COSString cosString, PDFont font, List<TextRun> runs, StringBuilder globalText) throws IOException {
        if (font == null) {
            return;
        }
        String decoded = decode(cosString, font);
        if (decoded.isEmpty()) {
            return;
        }
        runs.add(new TextRun(cosString, font, globalText.length(), decoded));
        globalText.append(decoded);
    }

    private static String decode(COSString cosString, PDFont font) throws IOException {
        StringBuilder text = new StringBuilder();
        try (ByteArrayInputStream in = new ByteArrayInputStream(cosString.getBytes())) {
            while (in.available() > 0) {
                int code = font.readCode(in);
                String unicode = font.toUnicode(code);
                if (unicode != null) {
                    text.append(unicode);
                }
            }
        }
        return text.toString();
    }

    // COSString.setValue(byte[]) sigue siendo la única forma de mutar el string en su sitio
    // (necesario porque el mismo objeto está referenciado desde dentro de `tokens`, incl.
    // dentro de COSArray para TJ); está deprecado en PDFBox 3 en favor de crear un
    // COSString nuevo, pero eso obligaría a reconstruir cada COSArray/operando.
    @SuppressWarnings("deprecation")
    private static void setValue(COSString cosString, byte[] bytes) {
        cosString.setValue(bytes);
    }

    private static void writeContent(PDDocument document, PDPage page, List<Object> tokens) throws IOException {
        PDStream newContent = new PDStream(document);
        try (OutputStream out = newContent.createOutputStream(COSName.FLATE_DECODE)) {
            new ContentStreamWriter(out).writeTokens(tokens);
        }
        page.setContents(newContent);
    }

    private record Redaction(String original, String replacement) {
    }

    private static final class TextRun {
        private final COSString cosString;
        private final PDFont font;
        private final int globalStart;
        private final StringBuilder text;
        private boolean modified;

        private TextRun(COSString cosString, PDFont font, int globalStart, String text) {
            this.cosString = cosString;
            this.font = font;
            this.globalStart = globalStart;
            this.text = new StringBuilder(text);
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        if (args.length != 2) {
            System.err.println("Uso: TicketPdfAnonymizer <entrada.pdf> <salida.pdf>");
            System.exit(1);
            return;
        }
        int applied = new TicketPdfAnonymizer().anonymize(Path.of(args[0]), Path.of(args[1]));
        System.out.println("Campos anonimizados: " + applied);
    }
}
