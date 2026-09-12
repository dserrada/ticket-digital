package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica en la práctica la garantía de thread-safety documentada en {@link TicketMercadona}:
 * llamar a {@link TicketMercadona#parse} concurrentemente desde varios hilos (como haría un
 * servicio REST atendiendo varias peticiones a la vez) no debe lanzar errores inesperados ni,
 * más importante todavía, mezclar el estado de una llamada con el de otra.
 */
class TicketMercadonaConcurrencyTest {

    private static final int THREADS = 16;

    /**
     * El propio {@link CyclicBarrier} obliga a que todos los hilos empiecen a la vez, en vez
     * de arrancar escalonados por el coste de crear cada hilo, maximizando el solape real
     * entre llamadas a parse() y por tanto la probabilidad de detectar una condición de
     * carrera si la hubiera.
     */
    private static void runConcurrently(int threads, ConcurrentTask task) throws Exception {
        CyclicBarrier startBarrier = new CyclicBarrier(threads);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int t = 0; t < threads; t++) {
                int threadIndex = t;
                futures.add(pool.submit(() -> {
                    try {
                        startBarrier.await();
                        task.run(threadIndex);
                    } catch (Throwable e) {
                        failures.add(e);
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        }
        assertTrue(failures.isEmpty(), () -> "Fallos en hilos concurrentes: " + failures);
    }

    @FunctionalInterface
    private interface ConcurrentTask {
        void run(int threadIndex) throws Exception;
    }

    /**
     * Cada hilo parsea repetidamente, entrelazado con los demás, ficheros DISTINTOS con
     * totales distintos. Si hubiera estado compartido entre llamadas concurrentes (p.ej. un
     * campo que debería ser local pero es static), lo esperable es que el resultado de un
     * hilo se contamine con datos del ticket que está parseando otro hilo a la vez, y el total
     * (u otro campo) no coincida con el esperado para ESE fichero.
     */
    @Test
    void parseConcurrenteDeFicherosDistintosNoMezclaEstadoEntreHilos() throws Exception {
        Map<String, BigDecimal> expectedTotalByFile = Map.of(
                "20230907 Mercadona 33,50 €.pdf", new BigDecimal("33.50"),
                "20241108 Mercadona 77,71 €.pdf", new BigDecimal("77.71"),
                "20250809 Mercadona 122,05 €.pdf", new BigDecimal("122.05"),
                "20260909 Mercadona 4,65 €.pdf", new BigDecimal("4.65")
        );
        List<String> fileNames = List.copyOf(expectedTotalByFile.keySet());
        int parsesPerThread = 20;

        runConcurrently(THREADS, threadIndex -> {
            for (int i = 0; i < parsesPerThread; i++) {
                int iteration = i;
                String fileName = fileNames.get((threadIndex + i) % fileNames.size());
                Path path = Path.of("src/test/resources/" + fileName);
                TicketMercadona ticket = TicketMercadona.parse(path);
                BigDecimal expectedTotal = expectedTotalByFile.get(fileName);
                assertEquals(expectedTotal, ticket.total(),
                        () -> "total() incorrecto para " + fileName + " (hilo " + threadIndex + ", iteración " + iteration + ")");
                assertEquals(expectedTotal, ticket.itemsTotal(),
                        () -> "itemsTotal() incorrecto para " + fileName + " (hilo " + threadIndex + ", iteración " + iteration + ")");
            }
        });
    }

    /**
     * Complementa el test anterior con muchas más repeticiones del MISMO ticket en paralelo
     * (sin el coste de abrir un PDF en cada iteración): si dos hilos se pisaran al leer/escribir
     * el mismo estado de parseo compartido, lo habitual es que el resultado salga corrupto y
     * alguna de las dos llamadas lance ParseException o similar, no que "por suerte" les
     * cuadre a ambas el resultado correcto (misma entrada o no).
     */
    @Test
    void parseConcurrenteDelMismoTicketEnTextoAguantaCargaAlta() throws Exception {
        String ticketData = """
                                    MERCADONA, S.A. A-46103834
                                    AV. DE FERRRAN EL CATÒLIC, 1
                                       46113 MONCADA
                                     TELÉFONO: 961309467
                                  01/01/2023 09:00 OP: 257136
                              FACTURA SIMPLIFICADA: 4567-891-113122


                           Descripción                       P. Unit    Importe
                       1   BARRA DE PAN                                    0,48
                       1   PANECILLO 11UDS                                 1,10
                       2   FRANKFURT VIENA QUES                 2,80       5,60
                       1   CHORIZO 4PACK                                   1,97
                       1   BANANA
                               0,336 kg                   1,45 €/kg        0,49
                                                         TOTAL (€)         9,64
                                                TARJETA BANCARIA           9,64

                            IVA           BASE IMPONIBLE (€)     CUOTA (€)
                            10%                   7,76             0,78
                             0%                   1,10             0,00
                           TOTAL                  8,86             0,78

                       TARJ. BANCARIA: **** **** **** 1234
                       N.C: 1234567                                  AUT: Z12345
                       AID: A0000000041010                           ARC: 46113


                       MASTERCARD
                       Importe: 9,64 €                          MASTERCARD




                             SE ADMITEN DEVOLUCIONES CON TICKET
                """;

        int parsesPerThread = 100;

        runConcurrently(THREADS, threadIndex -> {
            for (int i = 0; i < parsesPerThread; i++) {
                int iteration = i;
                TicketMercadona ticket = TicketMercadona.parse(ticketData);
                assertEquals(new BigDecimal("9.64"), ticket.total(),
                        () -> "total() incorrecto (hilo " + threadIndex + ", iteración " + iteration + ")");
                assertEquals(5, ticket.items().size(),
                        () -> "número de items incorrecto (hilo " + threadIndex + ", iteración " + iteration + ")");
            }
        });
    }
}
