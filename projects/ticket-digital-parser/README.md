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
