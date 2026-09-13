# ticket-digital-parser

Librería sin CLI propia que lee un ticket digital de Mercadona —en PDF o, si ya
dispones del texto extraído, directamente en texto plano— y lo convierte en un objeto
Java inmutable con toda la información estructurada: qué se compró, cuánto costó y cómo
se pagó. Es parte del proyecto [`ticket-digital`](../../README.md); la usa internamente
[`ticket-digital-data`](../ticket-digital-data/README.md). No accede a Gmail ni a
ningún otro servicio: solo interpreta el fichero que se le pasa.

## Qué hace

El punto de entrada es `TicketMercadona.parse(Path)` (o `parse(String)` si ya tienes el
texto del ticket):

- Para un PDF, extrae el texto con PDFBox conservando la indentación original —
  necesaria para distinguir secciones como los productos frescos, que aparecen
  agrupados bajo una cabecera de categoría (p. ej. "PESCADO").
- Interpreta el texto línea a línea, reconociendo en orden: los datos de la tienda, la
  cabecera del ticket, la lista de productos comprados, el total, el desglose de IVA y
  los datos del pago con tarjeta.
- Antes de devolver el objeto, valida que la suma de las líneas de producto coincide con
  el total impreso y que ese total coincide con el importe cargado a la tarjeta; si algo
  no cuadra, lanza una excepción en vez de devolver un ticket con datos inconsistentes.

## Modelo de datos

| Clase | Contenido |
|---|---|
| `ShopData` | Datos de la tienda: nombre, CIF, dirección, código postal, provincia, teléfono. |
| `TicketHeader` | Fecha y hora de compra, código de operación, número de factura simplificada. |
| `PurchasedItem` | Interfaz sellada con 4 variantes según cómo se vendió el producto: `OneItemByUnit` (1 unidad), `NItemsByUnit` (N unidades al mismo precio), `ItemByWeight` (por peso) y `FreshItemByWeight` (fresco por peso, agrupado bajo su sección). Todas exponen `id` (la descripción impresa en el ticket), cantidad o peso, precio unitario y precio final. |
| `Parking` | Horario de validación de parking, si el ticket lo incluye. |
| `VatBreakdown` / `VatRate` | Desglose de IVA por tipo: base imponible y cuota de cada tramo. |
| `CardPayment` | Datos del pago con tarjeta: últimos 4 dígitos, N.C./AUT/AID/ARC, marca e importe. |
| `TicketMercadona` | El ticket completo: agrega todo lo anterior más el total y el importe cargado a la tarjeta. |

Todas son `record` inmutables. El único identificador de un producto es el texto libre
de su descripción (`id`) — no hay categoría, código de barras ni ningún otro dato
adicional, porque el propio ticket tampoco los trae.

## PDFs de test (`src/test/resources`)

Los 4 PDF de `src/test/resources` son tickets reales (del autor del proyecto), necesarios
para probar el parser contra variaciones de formato reales (productos frescos, la columna
"Cnt." que Mercadona añadió en 2026, etc.) que no merece la pena reproducir a mano. Antes
de subirlos al repositorio se les aplica `TicketPdfAnonymizer`
([código](src/main/java/org/terra/incognita/ticketdigital/mercadona/anonymizer/TicketPdfAnonymizer.java)),
que reescribe el propio contenido del PDF (no un tapado visual) para:

- Sustituir los datos personales/identificativos: últimos 4 dígitos de tarjeta, dirección,
  localidad y teléfono de la tienda, fecha y hora de compra, código de operación y número
  de factura simplificada.
- Eliminar todas las imágenes incrustadas (logo, icono de atención al cliente y código de
  barras) — el logo es una marca registrada de Mercadona que no aporta nada al parser (que
  solo lee texto), y el código de barras, al ser una imagen, seguiría codificando en sus
  píxeles el número de factura original aunque el texto ya estuviera anonimizado.

Se conservan sin modificar los productos comprados, sus precios, el IVA y el resto de datos
del pago con tarjeta (N.C./AUT/AID/ARC): no identifican al comprador y son necesarios para
que estos PDF sigan sirviendo como fixtures reales del parser.

Las condiciones de uso del servicio Ticket Digital de Mercadona restringen la distribución
pública del contenido del servicio sin autorización previa. Como usuario individual no hay
forma práctica de obtener esa autorización por escrito; el criterio seguido aquí ha sido
reducir la reproducción a lo estrictamente necesario para las pruebas (sin marca, sin datos
personales, sin ánimo comercial) — ver también el disclaimer de no afiliación en el
[README principal](../../README.md). Si Mercadona solicitara la retirada de estos ficheros,
se atenderá esa solicitud.

## Limitaciones del propio ticket digital (no de este código)

- **No se puede verificar que un ticket sea real y no haya sido modificado.** El PDF no
  lleva firma, sello ni ningún otro elemento que permita comprobar que lo emitió
  Mercadona y que nadie ha alterado su contenido después: este módulo se limita a
  interpretar el texto que contiene y da por buena su veracidad. Por eso todo el
  proyecto está pensado para analizar tus propios tickets (que sabes que son reales
  porque los has recibido tú), no para funcionar como agregador o base de datos de
  tickets de terceros — un PDF ajeno no se podría dar por válido.
- **La descripción de un producto no es un identificador único y estable en el tiempo.**
  Es el único dato disponible para identificar qué se compró, y si en algún momento
  Mercadona cambia el formato o el tamaño de un envase sin cambiar el texto impreso en
  el ticket, el histórico tratará ambos productos —el de antes y el de después del
  cambio— como si fueran el mismo. Esto puede distorsionar el análisis de la evolución
  de precios de ese producto en concreto.

  Es un caso distinto al de comprar un mismo producto unas veces por unidad y otras por
  peso, que sí se distingue como dos entradas separadas de la cesta (ver
  [`docs/inflation-index.md`](../../docs/inflation-index.md)): ese caso se detecta
  porque cambia la forma de venta, no la descripción.
