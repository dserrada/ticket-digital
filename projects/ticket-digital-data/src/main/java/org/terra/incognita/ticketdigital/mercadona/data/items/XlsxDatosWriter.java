package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.data.inflation.InflationAnalyzer;
import org.terra.incognita.ticketdigital.mercadona.data.inflation.InflationReport;
import org.terra.incognita.ticketdigital.mercadona.data.inflation.YearInflationIndex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Genera un fichero xlsx a partir de la plantilla {@code Mercadona-base.xlsx}, sustituyendo el
 * contenido de la hoja "Datos" por los datos reales de los tickets, y el de la hoja "MiInflación"
 * por el índice de inflación anual (Laspeyres/Paasche) calculado sobre esos mismos datos (ver
 * {@link InflationAnalyzer}); si no hay histórico suficiente para calcular un año base, esa hoja
 * se deja tal cual está en la plantilla. El resto de la plantilla (tablas y gráficos dinámicos,
 * estilos, etc.) se copia sin modificar; es la propia plantilla la que debe tener activado el
 * refresco automático de sus tablas dinámicas al abrir el fichero.
 */
public class XlsxDatosWriter {
    private static final Logger logger = LoggerFactory.getLogger(XlsxDatosWriter.class);

    private static final String NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String NS_RELS_PKG = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String NS_RELS_DOC = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String TEMPLATE_RESOURCE = "/Mercadona-base.xlsx";
    private static final String DATOS_SHEET_NAME = "Datos";
    private static final int DATOS_HEADER_ROW = 1;
    private static final String DATOS_COLUMN_ORDER = "ABCDEFG";
    private static final String INFLACION_SHEET_NAME = "MiInflación";
    private static final int INFLACION_HEADER_ROW = 6;
    private static final String INFLACION_COLUMN_ORDER = "ABCDEF";
    // Estilo añadido a xl/styles.xml (cellXfs índice 8) con numFmtId="10", el formato
    // porcentaje "0.00%" incorporado de Excel.
    private static final String PERCENT_CELL_STYLE = "8";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);

    public static void writeXlsxToFile(Path ticketsDir, File outputFile) throws IOException {
        logger.debug("Iniciando la escritura del fichero xlsx, ticketsDir: {}, outputFile: {}", ticketsDir, outputFile.getAbsolutePath());
        List<PurchasedItemRecord> records = TicketRecordsReader.readAll(ticketsDir);
        if (records == null) {
            throw new IOException("El directorio de datos no existe: " + ticketsDir.toAbsolutePath());
        }

        try (InputStream templateIn = XlsxDatosWriter.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (templateIn == null) {
                throw new IOException("No se encuentra la plantilla " + TEMPLATE_RESOURCE + " en el classpath");
            }
            writeXlsx(templateIn.readAllBytes(), records, outputFile);
        }
        logger.debug("Finalizada la escritura del fichero xlsx");
    }

    static void writeXlsx(byte[] template, List<PurchasedItemRecord> records, File outputFile) throws IOException {
        Map<String, byte[]> replacedParts = new LinkedHashMap<>();

        String datosSheetPart = resolveSheetPart(template, DATOS_SHEET_NAME);
        replacedParts.put(datosSheetPart, buildDatosSheetXml(template, datosSheetPart, records));

        try {
            InflationReport report = InflationAnalyzer.analyze(records);
            String inflacionSheetPart = resolveSheetPart(template, INFLACION_SHEET_NAME);
            replacedParts.put(inflacionSheetPart, buildInflacionSheetXml(template, inflacionSheetPart, report));
        } catch (IllegalStateException e) {
            // No hay histórico suficiente para fijar un año base (p.ej. muy pocos tickets todavía):
            // se deja la hoja "MiInflación" tal cual está en la plantilla en vez de fallar toda la
            // generación del xlsx, que sigue siendo útil solo con la hoja "Datos".
            logger.warn("No se ha podido calcular el índice de inflación para la hoja '{}': {}",
                    INFLACION_SHEET_NAME, e.getMessage());
        }

        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(template));
             ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outputFile))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                zout.putNextEntry(new ZipEntry(entry.getName()));
                byte[] replacement = replacedParts.get(entry.getName());
                if (replacement != null) {
                    zout.write(replacement);
                } else {
                    zin.transferTo(zout);
                }
                zout.closeEntry();
            }
        }
    }

    // Resuelve dinámicamente a qué worksheets/sheetN.xml corresponde una hoja por su nombre, en vez
    // de asumir un nombre de fichero fijo, para que la plantilla pueda ganar/reordenar hojas sin que
    // este código deba cambiar.
    private static String resolveSheetPart(byte[] template, String sheetName) throws IOException {
        Document workbookDoc = parseZipEntry(template, "xl/workbook.xml");
        String sheetRid = null;
        NodeList sheets = workbookDoc.getElementsByTagNameNS(NS_MAIN, "sheet");
        for (int i = 0; i < sheets.getLength(); i++) {
            Element sheet = (Element) sheets.item(i);
            if (sheetName.equals(sheet.getAttribute("name"))) {
                sheetRid = sheet.getAttributeNS(NS_RELS_DOC, "id");
                break;
            }
        }
        if (sheetRid == null) {
            throw new IOException("No se encuentra la hoja '" + sheetName + "' en la plantilla");
        }

        Document relsDoc = parseZipEntry(template, "xl/_rels/workbook.xml.rels");
        NodeList rels = relsDoc.getElementsByTagNameNS(NS_RELS_PKG, "Relationship");
        for (int i = 0; i < rels.getLength(); i++) {
            Element rel = (Element) rels.item(i);
            if (sheetRid.equals(rel.getAttribute("Id"))) {
                return "xl/" + rel.getAttribute("Target");
            }
        }
        throw new IOException("No se encuentra la relación " + sheetRid + " en workbook.xml.rels");
    }

    private static byte[] buildDatosSheetXml(byte[] template, String sheetPart, List<PurchasedItemRecord> records) throws IOException {
        Document sheetDoc = parseZipEntry(template, sheetPart);

        List<Element> rows = new ArrayList<>();
        int rowNum = DATOS_HEADER_ROW + 1;
        for (PurchasedItemRecord record : records) {
            rows.add(buildDatosRow(sheetDoc, rowNum, record));
            rowNum++;
        }
        replaceDataRows(sheetDoc, DATOS_SHEET_NAME, DATOS_HEADER_ROW, DATOS_COLUMN_ORDER, rows);

        return serialize(sheetDoc);
    }

    private static byte[] buildInflacionSheetXml(byte[] template, String sheetPart, InflationReport report) throws IOException {
        Document sheetDoc = parseZipEntry(template, sheetPart);

        List<Element> rows = new ArrayList<>();
        int rowNum = INFLACION_HEADER_ROW + 1;
        for (YearInflationIndex index : report.yearIndices()) {
            rows.add(buildInflacionRow(sheetDoc, rowNum, index));
            rowNum++;
        }
        replaceDataRows(sheetDoc, INFLACION_SHEET_NAME, INFLACION_HEADER_ROW, INFLACION_COLUMN_ORDER, rows);

        return serialize(sheetDoc);
    }

    // Sustituye las filas de datos de una hoja (todo lo que va después de la fila de cabecera) por
    // las filas nuevas ya construidas, conservando tal cual la cabecera y cualquier fila anterior a
    // ella (p.ej. una nota informativa por encima), y actualiza dimension/autoFilter para reflejar
    // el nuevo rango.
    private static void replaceDataRows(Document sheetDoc, String sheetName, int headerRowNumber,
                                         String columnOrder, List<Element> newRows) throws IOException {
        Element root = sheetDoc.getDocumentElement();
        Element sheetData = firstChild(root, "sheetData");
        if (sheetData == null) {
            throw new IOException("La hoja '" + sheetName + "' no tiene sheetData");
        }

        boolean headerFound = false;
        List<Element> rowsToRemove = new ArrayList<>();
        NodeList rows = sheetData.getElementsByTagNameNS(NS_MAIN, "row");
        for (int i = 0; i < rows.getLength(); i++) {
            Element row = (Element) rows.item(i);
            int rowNumber = Integer.parseInt(row.getAttribute("r"));
            if (rowNumber == headerRowNumber) {
                headerFound = true;
            } else if (rowNumber > headerRowNumber) {
                rowsToRemove.add(row);
            }
        }
        if (!headerFound) {
            throw new IOException("La hoja '" + sheetName + "' no tiene fila de cabecera en la fila " + headerRowNumber);
        }
        for (Element row : rowsToRemove) {
            sheetData.removeChild(row);
        }
        for (Element row : newRows) {
            sheetData.appendChild(row);
        }

        int lastRow = Math.max(headerRowNumber, headerRowNumber + newRows.size());
        String newRange = "A1:" + columnOrder.charAt(columnOrder.length() - 1) + lastRow;
        Element dimension = firstChild(root, "dimension");
        if (dimension != null) {
            dimension.setAttribute("ref", newRange);
        }
        Element autoFilter = firstChild(root, "autoFilter");
        if (autoFilter != null) {
            autoFilter.setAttribute("ref", newRange);
        }
    }

    private static Element buildDatosRow(Document doc, int rowNum, PurchasedItemRecord record) {
        Element row = doc.createElementNS(NS_MAIN, "row");
        row.setAttribute("r", String.valueOf(rowNum));

        appendInlineStringCell(doc, row, "A" + rowNum, record.id());
        appendDateCell(doc, row, "B" + rowNum, record.date());
        appendNumericCell(doc, row, "C" + rowNum, record.units() == null ? null : BigDecimal.valueOf(record.units()));
        appendNumericCell(doc, row, "D" + rowNum, record.unitPrice());
        appendNumericCell(doc, row, "E" + rowNum, record.weightKg());
        appendNumericCell(doc, row, "F" + rowNum, record.pricePerKg());
        appendNumericCell(doc, row, "G" + rowNum, record.price());

        return row;
    }

    private static Element buildInflacionRow(Document doc, int rowNum, YearInflationIndex index) {
        Element row = doc.createElementNS(NS_MAIN, "row");
        row.setAttribute("r", String.valueOf(rowNum));

        appendNumericCell(doc, row, "A" + rowNum, BigDecimal.valueOf(index.year()));
        appendBooleanCell(doc, row, "B" + rowNum, index.baseYear());
        appendBooleanCell(doc, row, "C" + rowNum, index.complete());
        // Laspeyres/Paasche son un índice en base 100 (100 = año base); se muestran como variación
        // porcentual (p.ej. 103.45 -> +3,45%), con celda de tipo porcentaje de Excel.
        appendPercentCell(doc, row, "D" + rowNum, index.laspeyres() == null ? null : index.laspeyres().subtract(ONE_HUNDRED));
        appendPercentCell(doc, row, "E" + rowNum, index.paasche() == null ? null : index.paasche().subtract(ONE_HUNDRED));
        appendNumericCell(doc, row, "F" + rowNum, BigDecimal.valueOf(index.matchedProductCount()));

        return row;
    }

    private static void appendInlineStringCell(Document doc, Element row, String ref, String value) {
        if (value == null) return;
        Element c = doc.createElementNS(NS_MAIN, "c");
        c.setAttribute("r", ref);
        c.setAttribute("t", "inlineStr");
        Element is = doc.createElementNS(NS_MAIN, "is");
        Element t = doc.createElementNS(NS_MAIN, "t");
        t.setTextContent(value);
        is.appendChild(t);
        c.appendChild(is);
        row.appendChild(c);
    }

    private static void appendNumericCell(Document doc, Element row, String ref, BigDecimal value) {
        if (value == null) return;
        Element c = doc.createElementNS(NS_MAIN, "c");
        c.setAttribute("r", ref);
        Element v = doc.createElementNS(NS_MAIN, "v");
        v.setTextContent(value.toPlainString());
        c.appendChild(v);
        row.appendChild(c);
    }

    // Recibe una variación en puntos porcentuales (p.ej. 3.45 para "+3,45%") y la escribe como
    // celda de tipo porcentaje de Excel: el valor guardado es la fracción (0,0345), y el estilo
    // aplica el formato "0.00%" para que se muestre igual que por consola.
    private static void appendPercentCell(Document doc, Element row, String ref, BigDecimal percentPoints) {
        if (percentPoints == null) return;
        Element c = doc.createElementNS(NS_MAIN, "c");
        c.setAttribute("r", ref);
        c.setAttribute("s", PERCENT_CELL_STYLE);
        Element v = doc.createElementNS(NS_MAIN, "v");
        v.setTextContent(percentPoints.divide(ONE_HUNDRED).toPlainString());
        c.appendChild(v);
        row.appendChild(c);
    }

    private static void appendBooleanCell(Document doc, Element row, String ref, boolean value) {
        Element c = doc.createElementNS(NS_MAIN, "c");
        c.setAttribute("r", ref);
        c.setAttribute("t", "b");
        Element v = doc.createElementNS(NS_MAIN, "v");
        v.setTextContent(value ? "1" : "0");
        c.appendChild(v);
        row.appendChild(c);
    }

    private static void appendDateCell(Document doc, Element row, String ref, LocalDateTime date) {
        if (date == null) return;
        Element c = doc.createElementNS(NS_MAIN, "c");
        c.setAttribute("r", ref);
        c.setAttribute("s", "1"); // Reutiliza el estilo de fecha ya definido en la plantilla para la columna B
        Element v = doc.createElementNS(NS_MAIN, "v");
        v.setTextContent(String.valueOf(ChronoUnit.DAYS.between(EXCEL_EPOCH, date.toLocalDate())));
        c.appendChild(v);
        row.appendChild(c);
    }

    private static Element firstChild(Element parent, String localName) {
        NodeList children = parent.getElementsByTagNameNS(NS_MAIN, localName);
        return children.getLength() > 0 ? (Element) children.item(0) : null;
    }

    private static Document parseZipEntry(byte[] zipBytes, String entryName) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    try {
                        return newDocumentBuilder().parse(zin);
                    } catch (SAXException e) {
                        throw new IOException("Error al parsear " + entryName, e);
                    }
                }
            }
        }
        throw new IOException("No se encuentra la entrada " + entryName + " en el fichero xlsx");
    }

    private static DocumentBuilder newDocumentBuilder() throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException("Error al configurar el parser XML", e);
        }
    }

    private static byte[] serialize(Document doc) throws IOException {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return out.toByteArray();
        } catch (TransformerException e) {
            throw new IOException("Error al serializar el XML de la hoja", e);
        }
    }
}
