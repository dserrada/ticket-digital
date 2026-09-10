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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class XlsxDatosWriterTest {

    @Test
    void writeXlsxReplacesOnlyTheDatosSheetContentWhenThereIsNotEnoughHistoryForInflation() throws IOException {
        byte[] template = readTemplate();

        // Un único mes de compras: no hay ningún año "completo" (10 de 12 meses), así que
        // InflationAnalyzer no puede fijar año base y la hoja "MiInflación" se deja tal cual.
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
        for (Map.Entry<String, byte[]> entry : originalEntries.entrySet()) {
            String name = entry.getKey();
            if (!java.util.Arrays.equals(entry.getValue(), outputEntries.get(name))) {
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

    @Test
    void writeXlsxAlsoFillsInflacionSheetWhenThereIsEnoughHistory() throws IOException {
        byte[] template = readTemplate();

        List<PurchasedItemRecord> records = new ArrayList<>();
        // 2024: PAN comprado en 10 de 12 meses a 1,00€ -> año base, Laspeyres/Paasche = 100 (0,00%)
        for (int month = 1; month <= 10; month++) {
            records.add(new PurchasedItemRecord("PAN", LocalDateTime.of(2024, month, 1, 10, 0), 1,
                    BigDecimal.ONE, null, null, BigDecimal.ONE));
        }
        // 2025: PAN sube a 1,10€ -> +10,00% (año incompleto, pero se muestra igualmente)
        records.add(new PurchasedItemRecord("PAN", LocalDateTime.of(2025, 1, 1, 10, 0), 1,
                new BigDecimal("1.10"), null, null, new BigDecimal("1.10")));
        records.add(new PurchasedItemRecord("PAN", LocalDateTime.of(2025, 2, 1, 10, 0), 1,
                new BigDecimal("1.10"), null, null, new BigDecimal("1.10")));

        File outputFile = Files.createTempFile("Mercadona-test", ".xlsx").toFile();
        outputFile.deleteOnExit();
        XlsxDatosWriter.writeXlsx(template, records, outputFile);

        Map<String, byte[]> originalEntries = readZipEntries(template);
        Map<String, byte[]> outputEntries = readZipEntries(Files.readAllBytes(outputFile.toPath()));

        assertEquals(originalEntries.keySet(), outputEntries.keySet());

        List<String> changedEntries = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : originalEntries.entrySet()) {
            String name = entry.getKey();
            if (!java.util.Arrays.equals(entry.getValue(), outputEntries.get(name))) {
                changedEntries.add(name);
            }
        }
        assertEquals(2, changedEntries.size(), "Deben cambiar 'Datos' y 'MiInflación': " + changedEntries);
        assertTrue(changedEntries.contains("xl/worksheets/sheet1.xml"), "La hoja 'Datos' es sheet1.xml");
        assertTrue(changedEntries.contains("xl/worksheets/sheet3.xml"), "La hoja 'MiInflación' es sheet3.xml en esta plantilla");

        String inflacionSheetXml = new String(outputEntries.get("xl/worksheets/sheet3.xml"), StandardCharsets.UTF_8);
        assertTrue(inflacionSheetXml.contains("r=\"A7\""), "La primera fila de datos debe empezar justo tras la cabecera (fila 6)");
        assertTrue(inflacionSheetXml.contains("<v>2024</v>"));
        assertTrue(inflacionSheetXml.contains("<v>2025</v>"));
        assertTrue(inflacionSheetXml.contains("ref=\"A1:F8\""), "dimension/autoFilter deben cubrir hasta la fila 8 (cabecera fila 6 + 2 años)");

        // Laspeyres/Paasche de 2025 (PAN sube de 1,00€ a 1,10€ -> +10,00%): celda de tipo
        // porcentaje de Excel (estilo 8, formato "0.00%"), guardando la fracción 0,10.
        assertTrue(inflacionSheetXml.contains("<c r=\"D8\" s=\"8\"><v>0.10</v></c>"),
                "D8 (Laspeyres 2025) debe ser una celda porcentaje con la fracción 0,10: " + inflacionSheetXml);
        assertTrue(inflacionSheetXml.contains("<c r=\"E8\" s=\"8\"><v>0.10</v></c>"),
                "E8 (Paasche 2025) debe ser una celda porcentaje con la fracción 0,10: " + inflacionSheetXml);
        // Año base (2024): 0,00% también con formato porcentaje, no como número plano.
        assertTrue(inflacionSheetXml.contains("<c r=\"D7\" s=\"8\"><v>0.00</v></c>"),
                "D7 (Laspeyres año base) debe ser 0,00% con formato porcentaje: " + inflacionSheetXml);
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
