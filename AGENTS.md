# ticket-digital — Agent Guide

## Propósito del proyecto

Aplicación Java modular que extrae y analiza datos de **tickets digitales de Mercadona** (PDF o TXT) y los exporta a CSV. Es una herramienta de uso personal para análisis local de histórico de compras.

---

## Estructura del proyecto

```
ticket-digital/
├── projects/
│   ├── ticket-digital-parser/   # Librería: modelos de dominio y parsers
│   └── ticket-digital-data/     # Aplicación: CLI, generación de CSV
└── docs/tasks.md                # Backlog de tareas pendientes
```

Build system: **Gradle 8.x**, Java **24**, módulos multi-project.

---

## Módulos

### `ticket-digital-parser` (java-library)

Modelos de dominio y lógica de parsing.

**Dependencias:** `pdfbox:3.0.3`, `slf4j-api`

**Paquete raíz:** `org.terra.incognita.ticketdigital.mercadona.model`

**Modelos principales:**

| Clase | Descripción |
|---|---|
| `PurchasedItem` | Sealed interface base para ítems comprados. Define constantes regex, `SPANISH_LOCALE`, helpers de parsing. |
| `OneItemByUnit` | 1 unidad a precio fijo. |
| `NItemsByUnit` | N unidades × precio unitario. |
| `ItemByWeight` | Producto vendido por kg (peso × precio/kg). |
| `FreshItemByWeight` | Producto fresco por peso (pescadería), agrupado bajo una cabecera de categoría (p.ej. "PESCADO"). |
| `ShopData` | Datos del establecimiento (`shopName`, `cif`, `address`, `postalCode`, `state`, `phoneNumber`). |
| `TicketHeader` | Cabecera del ticket (`purchaseDate`, `operationCode`, `simplifiedInvoiceNumber`). |
| `Parking` | Horario de parking (`start`/`end`). |
| `VatRate` | Un tramo del desglose de IVA (`rate`, `taxableBase`, `vatAmount`). |
| `VatBreakdown` | Desglose de IVA completo (`rates`, `totalTaxableBase`, `totalVatAmount`), valida que la suma de tramos cuadre con la fila TOTAL. |
| `CardPayment` | Datos del pago con tarjeta al final del ticket (`lastFourDigits`, `nc`, `aut`, `aid`, `arc`, `brand`, `amount`). Tolera dos formatos distintos de N.C/AUT/AID/ARC vistos en tickets reales. |
| `TicketMercadona` | Ticket completo. Punto de entrada del parsing: `TicketMercadona.parse(Path)`. |
| `ParserStatusInfo` | Iterador de líneas con rollback para el parser chain. |

Todos los modelos son **records inmutables**. Los parsers son métodos estáticos en cada record que devuelven `null` si la línea no coincide (patrón parser chain); las excepciones (`ParseException`) solo se lanzan una vez el bloque se considera inequívoco (p.ej. tras casar la línea de tarjeta enmascarada en `CardPayment`).

Nomenclatura: los campos de los records están en inglés, salvo los conceptos que solo tienen sentido en español o que son códigos literales impresos en el ticket sin traducción clara (`cif`, `nc`, `aut`, `aid`, `arc`).

---

### `ticket-digital-data` (application)

CLI y generación de CSV.

**Dependencias:** `picocli:4.7.6`, `:ticket-digital-parser`

**Paquete raíz:** `org.terra.incognita.ticketdigital.mercadona`

| Clase | Descripción |
|---|---|
| `Main` | CLI picocli. Subcomando `write-items-csv`. |
| `CSVGenerator` | Orquesta el proceso: busca PDFs → parsea → escribe CSV. |
| `PurchasedItemRecord` | Representación tabular de una compra. Método `fromTicket()` para convertir, `toCSV()` para serializar. |
| `ProductNameNormalizer` | Corrige erratas de nombre de producto (acentos, puntuación) según `nombres-normalizacion.csv`. Se aplica dentro de `PurchasedItemRecord.fromTicket()`; el CSV se carga una única vez en un mapa estático. |
| `FileUtils` | Búsqueda recursiva de ficheros PDF en un directorio. |

---

## Flujo de datos

```
CLI (Main) → CSVGenerator → FileUtils.searchInDir()
                                    ↓
                         TicketMercadona.parse(Path)
                         [ShopData → TicketHeader → Items (+ Parking) →
                          total → VatBreakdown → CardPayment → disclaimer]
                         (ver docs/parser-phases.mmd)
                                    ↓
                         PurchasedItemRecord.fromTicket()
                         [normaliza el id vía ProductNameNormalizer.normalize()]
                                    ↓
                         PurchasedItemRecord.toCSV()
                                    ↓
                         BufferedWriter (UTF-8, separador ";")
```

El CSV de salida usa **separador `;`** y **decimales con coma** (locale español).

Los nombres de producto ya salen **normalizados** (`ProductNameNormalizer`, tabla en
`nombres-normalizacion.csv`): corrige erratas de tipografía/acentos/puntuación que
Mercadona imprime de forma inconsistente para el mismo producto (p.ej.
`ACEITE OLIVA 0'4` / `ACEITE OLIVA 0`4` → `ACEITE OLIVA 0.4`). Cualquier comprobación
posterior sobre nombres de producto (agrupaciones, totales por producto, etc.) puede
asumir que esta corrección **ya se ha aplicado** y no necesita repetirla ni tenerla en
cuenta como fuente de inconsistencias.

---

## Comandos de uso

```bash
# Compilar y ejecutar tests
./gradlew test

# Compilar todo
./gradlew build

# Ejecutar CLI
./gradlew :ticket-digital-data:run --args="write-items-csv --data-dir /ruta/pdfs --csv-file salida.csv"
```

---

## Estado de implementación

| Feature | Estado |
|---|---|
| Parsing OneItemByUnit, NItemsByUnit, ItemByWeight | Completo |
| Parsing ShopData, TicketHeader, Parking | Completo |
| Parsing FreshItemByWeight (productos frescos) | Completo |
| Parsing total, importe pagado con tarjeta, desglose de IVA | Completo |
| Parsing datos de pago con tarjeta (tarjeta enmascarada, N.C/AUT/AID/ARC, marca, importe) | Completo |
| Extracción de PDF (PDFBox) y TXT | Completo |
| CLI picocli | Completo |
| Generación CSV | Completo |

---

## Convenciones del código

- Todos los modelos de dominio son **Java records** (inmutables).
- Los parsers devuelven **`null`** si no coinciden (nunca lanzan excepción en ese caso).
- Las constantes regex están en `PurchasedItem` como `static final Pattern`.
- Se usan **named groups** en regex: `(?<id>...)`, `(?<price>...)`.
- Logging vía SLF4J; Logback solo en test scope.
- Los tests de integración asumen PDFs en rutas externas al repositorio.

---

## Áreas de trabajo pendiente

Ver `docs/tasks.md` para el backlog general (parcialmente desactualizado/genérico). Gaps conocidos y concretos en el código actual:

1. **`FreshItemByWeight.FRESH_TYPE_PATTERN`** — solo reconoce la categoría "PESCADO" como cabecera de sección fresca; otras categorías (carnicería, frutería, etc., si existen en algún ticket) no se detectarían.
2. **`TicketMercadonaTest.pruebaFichero()`** — referencia `src/test/resources/20230101090000 Ticket Digital Mercadona.txt`, que no existe en el repo; el test falla con `NoSuchFileException` (fallo preexistente, no introducido por el parseo de total/IVA/tarjeta).
3. **`OneItemByUnitTest.testParseValidSingleItem()`** — falla en una aserción (fallo preexistente, no relacionado con el parseo de total/IVA/tarjeta ni con el renombrado de campos).
4. **`CardPayment`** — el bloque final del ticket (relleno entre AID/ARC y la línea `Importe:`) se parsea de forma tolerante en base a los formatos vistos hasta ahora en ~150 tickets reales; nuevas plantillas de recibo de Mercadona podrían requerir ajustes.
