package org.terra.incognita.ticketdigital.mercadona.model;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.List;

/**
 * PDFTextStripper que preserva la indentación al inicio de cada línea.
 *
 * El PDFTextStripper estándar no inserta espacios iniciales en función de la posición X
 * del texto, de modo que líneas con distinto nivel de indentación quedan visualmente
 * iguales. Esta clase realiza dos pasadas sobre el documento:
 *   1. Encuentra la X mínima (margen izquierdo) del documento.
 *   2. Para el primer fragmento de cada línea calcula cuántos espacios preceden al texto
 *      como round((x - minX) / anchoEspacio) y los escribe antes del contenido.
 */
class IndentPreservingTextStripper extends PDFTextStripper {

    private static final float SPACING_TOLERANCE = 0.2f;

    private boolean atLineStart = true;
    private final float leftMarginX;

    private IndentPreservingTextStripper(float leftMarginX) throws IOException {
        this.leftMarginX = leftMarginX;
    }

    @Override
    protected void startPage(PDPage page) throws IOException {
        atLineStart = true;
        super.startPage(page);
    }

    @Override
    protected void writeLineSeparator() throws IOException {
        atLineStart = true;
        super.writeLineSeparator();
    }

    @Override
    protected void writeParagraphSeparator() throws IOException {
        atLineStart = true;
        super.writeParagraphSeparator();
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        if (atLineStart) {
            atLineStart = false;
            if (!textPositions.isEmpty()) {
                float lineX = textPositions.get(0).getX();
                float spaceWidth = textPositions.get(0).getWidthOfSpace();
                if (spaceWidth <= 0) spaceWidth = textPositions.get(0).getWidth();
                int numSpaces = Math.max(0, Math.round((lineX - leftMarginX) / spaceWidth)/2); // división por 2 para indentación de dos espacios
                output.write(" ".repeat(numSpaces));
            }
        }
        super.writeString(text, textPositions);
    }

    /**
     * Extrae el texto del documento PDF preservando la indentación de cada línea.
     *
     * @param document documento PDF ya cargado
     * @return texto extraído con espacios iniciales proporcionales a la posición X
     */
    static String extractText(PDDocument document) throws IOException {
        float minX = findMinimumX(document);
        IndentPreservingTextStripper stripper = new IndentPreservingTextStripper(minX);
        stripper.setSortByPosition(true);
        stripper.setSpacingTolerance(SPACING_TOLERANCE);
        return stripper.getText(document);
    }

    private static float findMinimumX(PDDocument document) throws IOException {
        float[] minX = {Float.MAX_VALUE};
        PDFTextStripper scanner = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions) {
                if (!textPositions.isEmpty()) {
                    float x = textPositions.get(0).getX();
                    if (x < minX[0]) minX[0] = x;
                }
            }
        };
        scanner.setSortByPosition(true);
        scanner.setSpacingTolerance(SPACING_TOLERANCE);
        scanner.getText(document);
        return minX[0] == Float.MAX_VALUE ? 0f : minX[0];
    }
}
