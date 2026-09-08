package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.model.TicketMercadona;
import org.terra.incognita.ticketdigital.mercadona.utils.FileUtils;
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
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Genera un fichero xlsx a partir de la plantilla {@code Mercadona-base.xlsx}, sustituyendo el
 * contenido de la hoja "Datos" por los datos reales de los tickets. El resto de la plantilla
 * (tablas y gráficos dinámicos, estilos, etc.) se copia sin modificar; es la propia plantilla la
 * que debe tener activado el refresco automático de sus tablas dinámicas al abrir el fichero.
 */
public class XlsxDatosWriter {
    private static final Logger logger = LoggerFactory.getLogger(XlsxDatosWriter.class);

    private static final String NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String NS_RELS_PKG = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String NS_RELS_DOC = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String TEMPLATE_RESOURCE = "/Mercadona-base.xlsx";
    private static final String DATOS_SHEET_NAME = "Datos";
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);
    private static final String DATA_COLUMN_ORDER = "ABCDEFG";

    public static void writeXlsxToFile(Path ticketsDir, File outputFile) throws IOException {
        logger.debug("Iniciando la escritura del fichero xlsx, ticketsDir: {}, outputFile: {}", ticketsDir, outputFile.getAbsolutePath());
        List<Path> files = FileUtils.searchInDir(ticketsDir);
        if (files == null) return;

        List<PurchasedItemRecord> records = files.stream()
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
                .toList();

        try (InputStream templateIn = XlsxDatosWriter.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (templateIn == null) {
                throw new IOException("No se encuentra la plantilla " + TEMPLATE_RESOURCE + " en el classpath");
            }
            writeXlsx(templateIn.readAllBytes(), records, outputFile);
        }
        logger.debug("Finalizada la escritura del fichero xlsx");
    }

    static void writeXlsx(byte[] template, List<PurchasedItemRecord> records, File outputFile) throws IOException {
        String datosSheetPart = resolveDatosSheetPart(template);
        byte[] newSheetXml = buildDatosSheetXml(template, datosSheetPart, records);

        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(template));
             ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outputFile))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                zout.putNextEntry(new ZipEntry(entry.getName()));
                if (entry.getName().equals(datosSheetPart)) {
                    zout.write(newSheetXml);
                } else {
                    zin.transferTo(zout);
                }
                zout.closeEntry();
            }
        }
    }

    // Resuelve dinámicamente a qué worksheets/sheetN.xml corresponde la hoja "Datos", en vez de
    // asumir un nombre de fichero fijo, para que la plantilla pueda ganar hojas/tablas/gráficos
    // nuevos sin que este código deba cambiar.
    private static String resolveDatosSheetPart(byte[] template) throws IOException {
        Document workbookDoc = parseZipEntry(template, "xl/workbook.xml");
        String sheetRid = null;
        NodeList sheets = workbookDoc.getElementsByTagNameNS(NS_MAIN, "sheet");
        for (int i = 0; i < sheets.getLength(); i++) {
            Element sheet = (Element) sheets.item(i);
            if (DATOS_SHEET_NAME.equals(sheet.getAttribute("name"))) {
                sheetRid = sheet.getAttributeNS(NS_RELS_DOC, "id");
                break;
            }
        }
        if (sheetRid == null) {
            throw new IOException("No se encuentra la hoja '" + DATOS_SHEET_NAME + "' en la plantilla");
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

    private static byte[] buildDatosSheetXml(byte[] template, String datosSheetPart, List<PurchasedItemRecord> records) throws IOException {
        Document sheetDoc = parseZipEntry(template, datosSheetPart);
        Element root = sheetDoc.getDocumentElement();

        Element sheetData = firstChild(root, "sheetData");
        if (sheetData == null) {
            throw new IOException("La hoja '" + DATOS_SHEET_NAME + "' no tiene sheetData");
        }

        Element headerRow = null;
        List<Element> dataRows = new ArrayList<>();
        NodeList rows = sheetData.getElementsByTagNameNS(NS_MAIN, "row");
        for (int i = 0; i < rows.getLength(); i++) {
            Element row = (Element) rows.item(i);
            if ("1".equals(row.getAttribute("r"))) {
                headerRow = row;
            } else {
                dataRows.add(row);
            }
        }
        if (headerRow == null) {
            throw new IOException("La hoja '" + DATOS_SHEET_NAME + "' no tiene fila de cabecera");
        }
        for (Element row : dataRows) {
            sheetData.removeChild(row);
        }

        int rowNum = 2;
        for (PurchasedItemRecord record : records) {
            sheetData.appendChild(buildRow(sheetDoc, rowNum, record));
            rowNum++;
        }

        String newRange = "A1:" + DATA_COLUMN_ORDER.charAt(DATA_COLUMN_ORDER.length() - 1) + Math.max(1, rowNum - 1);
        Element dimension = firstChild(root, "dimension");
        if (dimension != null) {
            dimension.setAttribute("ref", newRange);
        }
        Element autoFilter = firstChild(root, "autoFilter");
        if (autoFilter != null) {
            autoFilter.setAttribute("ref", newRange);
        }

        return serialize(sheetDoc);
    }

    private static Element buildRow(Document doc, int rowNum, PurchasedItemRecord record) {
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
