package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProductNameNormalizerTest {

    @Test
    void corrigeUnNombreConocido() {
        assertEquals("ACEITE OLIVA 0.4", ProductNameNormalizer.normalize("ACEITE OLIVA 0'4"));
        assertEquals("FRANKFURT VIENA QUES", ProductNameNormalizer.normalize("FRANKFURT VIEA QUES"));
    }

    @Test
    void devuelveElMismoNombreSiNoHayCorreccion() {
        String nombre = "PAN DE MOLDE";
        assertSame(nombre, ProductNameNormalizer.normalize(nombre));
    }
}
