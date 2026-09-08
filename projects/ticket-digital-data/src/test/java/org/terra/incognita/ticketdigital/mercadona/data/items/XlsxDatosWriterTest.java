package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class XlsxDatosWriterTest {

    @Test
    void writeXlsxReplacesOnlyTheDatosSheetContent() throws IOException {
        byte[] template = readTemplate();

        List<PurchasedItemRecord> records = List.of(
                new PurchasedItemRecord("BARRA DE PAN", LocalDateTime.of(2026, 9, 1, 10, 0), 1,
                        new BigDecimal("1.10"), null, null, new BigDecimal("1.10")),
                new PurchasedItemRecord("TOMATE PERA & CO", LocalDateTime.of(2026, 9, 2, 11, 30), 1,
                        null, new BigDecimal("0.750"), new BigDecimal("2.00"), new BigDecimal("1.50"))
        );

        File outputFile = Files.createTempFile("Mercadona-test", ".xlsx").toFile();
        outputFile.deleteOnExit();
        XlsxDatosWriter.writeXlsx(template, records, outputFile);

        Map<String, byte[]> originalEntries = readZipEntries(template);
        Map<String, byte[]> outputEntries = readZipEntries(Files.readAllBytes(outputFile.toPath()));

        // El conjunto de partes del paquete no cambia: no se añaden ni eliminan entradas.
        assertEquals(originalEntries.keySet(), outputEntries.keySet());

        String changedEntry = null;
        for (String name : originalEntries.keySet()) {
            if (!java.util.Arrays.equals(originalEntries.get(name), outputEntries.get(name))) {
                assertNull(changedEntry, "Más de una entrada del zip cambió: " + changedEntry + " y " + name);
                changedEntry = name;
            }
        }

        assertNotNull(changedEntry, "Ninguna entrada cambió");
        assertEquals("xl/worksheets/sheet1.xml", changedEntry, "La hoja 'Datos' de esta plantilla es sheet1.xml");

        String newSheetXml = new String(outputEntries.get(changedEntry), StandardCharsets.UTF_8);
        assertTrue(newSheetXml.contains("BARRA DE PAN"));
        assertTrue(newSheetXml.contains("TOMATE PERA &amp; CO"), "El id con '&' debe ir escapado en el XML");
        assertTrue(newSheetXml.contains("ref=\"A1:G3\""), "dimension/autoFilter deben cubrir cabecera + 2 filas");
        assertFalse(newSheetXml.contains("COCKTAIL RODEO"), "Los datos de ejemplo deben desaparecer");
    }

    private static byte[] readTemplate() throws IOException {
        try (InputStream in = XlsxDatosWriter.class.getResourceAsStream("/Mercadona-base.xlsx")) {
            assertNotNull(in, "No se encuentra Mercadona-base.xlsx en el classpath");
            return in.readAllBytes();
        }
    }

    private static Map<String, byte[]> readZipEntries(byte[] zipBytes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                entries.put(entry.getName(), zin.readAllBytes());
            }
        }
        return entries;
    }
}
