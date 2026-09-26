# Propuestas de nuevas vistas dinámicas para el Excel

Análisis de qué otras vistas (tablas dinámicas, gráficos, cálculos) serían útiles para la
gestión del gasto en supermercado/alimentación, a partir de los datos que ya extraemos de los
tickets. Fecha del análisis: 2026-09-25.

## Situación actual de la plantilla (`Mercadona-base.xlsx`)

| Hoja | Qué muestra |
|---|---|
| Productos | Número de compras y precio unitario medio de cada producto |
| T_Precio | Gasto por año y mes, con gráfico de barras |
| T_Cantidad | Unidades por año y mes, filtrando por producto, con gráfico |
| Mes | Unidades de cada producto, filtrando por mes |
| Año | Número de compras y gasto de cada producto, por año |
| Último | Fecha de la última compra de cada producto |
| Evolucion-€ | Precio unitario medio de un producto por año y mes (línea) |
| Evolucion-N | Unidades de un producto por año (línea) |
| MiInflación | Índices de Laspeyres y Paasche (calculados en Java) |

Todas las tablas dinámicas salen de las 8 columnas de `Datos`: `id`, `fecha` (con hora y
minuto), `unidades`, `precioPorUnidad`, `pesoKg`, `precioKg`, `precio` y `factura` (número de
factura simplificada, identifica el ticket). El origen de la caché es el nombre definido
`DatosTablaDinamica` = `OFFSET(Datos!$A$1,0,0,1048576,COUNTA(Datos!$1:$1))`: abarca tantas
columnas como cabeceras haya en la fila 1, así que una columna nueva escrita por el programa
entra en las tablas dinámicas sin ampliar el rango (no se puede usar un rango fijo con
columnas de sobra porque Excel rechaza campos sin cabecera). LibreOffice no evalúa este
nombre como origen y no carga las tablas dinámicas; para verificarlas con LibreOffice hay que
sustituirlo temporalmente por un rango fijo. El parser saca bastante más información de la
que llega al Excel (tienda, total, desglose de IVA, tarjeta, parking, tipo de ítem), y ahí
están las vistas con más recorrido.

---

## A. Vistas que ya se pueden hacer con las columnas actuales

Basta con añadir tablas dinámicas a la plantilla, sin tocar Java.

1. **[Hecho: hoja `Pareto`] Productos que concentran el gasto (Pareto/ABC).** Gasto total por producto, ordenado de
   mayor a menor, con "% del total" y "% acumulado". Suele salir que unos 30–40 productos son
   el 80 % del gasto. Es la vista más útil para decidir dónde ahorrar: en la clase A, cambiar
   de marca o de formato se nota; en la clase C, no.

2. **[Hecho: hoja `Interanual`] Mismo mes, distintos años.** Meses en filas, años en columnas, suma de precio y
   "% de diferencia" frente al año anterior. Separa la estacionalidad (diciembre siempre es más
   caro) de la tendencia real. Los agrupamientos Años/Meses ya existen en la caché.

3. **Mapa de calor de producto por mes.** Productos en filas, año-mes en columnas y formato
   condicional de color. Deja ver los cambios de hábito (un producto que desaparece o uno nuevo
   que entra fuerte) y la estacionalidad (fruta, helados…).

4. **Precio medio ponderado, incluidos los productos a peso.** Evolucion-€ usa
   "Promedio de precioPorUnidad", que tiene dos problemas:
   - No pondera por cantidad: 1 unidad a 2 € y 5 unidades a 1 € dan de media 1,50 €, cuando el
     precio real pagado es 1,17 €.
   - Los productos a peso (fruta, carne, pescado) tienen `precioPorUnidad` vacío, así que no
     aparecen.

   Se arregla con campos calculados de la tabla dinámica, `=precio/unidades` y
   `=precio/pesoKg`. Excel los aplica sobre las sumas, así que el resultado queda bien
   ponderado.

5. **Altas y bajas de productos.** Primera compra (Mín. de fecha) junto a la última
   (Máx. de fecha, que ya está en Último) y el número de compras. Muestra qué entra en la dieta
   cada año y qué ha dejado de comprarse.

## B. Vistas que piden columnas derivadas en `Datos`

Hay que ampliar `PurchasedItemRecord` y `XlsxDatosWriter`, pero son cambios pequeños.

6. **Categoría del producto.** Es la que más aporta. Una tabla `producto → categoría`, del
   mismo estilo que `nombres-normalizacion.csv`, con categorías como lácteos, carne, pescado,
   fruta y verdura, panadería, bebidas, alcohol, limpieza, higiene o mascotas. Permite:
   - Ver el gasto por categoría y mes, y su evolución.
   - Ver el reparto entre **alimentación y no alimentación**. Importa porque la idea es el gasto
     en *alimentación* del hogar, y ahora mismo la droguería va mezclada con la comida.
   - Calcular la inflación por categoría.

   El coste es mantener la tabla de clasificación a mano. Un producto sin clasificar puede caer
   en "Sin categoría" y verse así en la propia tabla dinámica.

7. **Forma de venta (UNIDAD, PESO o FRESCO).** Ya se conoce en
   `PurchasedItemRecord.fromTicket()` por el tipo de ítem. Da el peso del gasto en frescos
   frente a envasados y los kg comprados al mes.

8. **Identificador de ticket, hora y día de la semana.** Hecho en parte: `fecha` ya lleva la
   hora y el minuto, y la columna `factura` identifica cada ticket. Falta una columna auxiliar "primera línea del ticket" a 1/0 para contar tickets
   con una suma, salen:
   - Número de visitas al supermercado por mes y días medios entre compras.
   - **Ticket medio** y artículos por ticket.
   - Gasto por día de la semana y por franja horaria.

9. **Tienda** (`ShopData`: dirección, código postal). Gasto y visitas por tienda. Solo aporta
   si se compra en más de un Mercadona.

## C. Una hoja nueva con una fila por ticket

Contendría fecha y hora, tienda, total, número de líneas, IVA por tramo, tarjeta (últimos 4
dígitos y marca) y parking. Todo eso ya lo extrae el parser y hoy se descarta.

10. **Distribución de importes por ticket.** Tramos de menos de 20 €, 20–50 €, 50–100 € y más
    de 100 €. Separa la compra grande planificada de las compras pequeñas de reposición, que
    suelen ser por donde se escapa el gasto.

11. **Gasto por tarjeta.** Si en casa pagan varias personas con tarjetas distintas, da el
    reparto del gasto entre ellas: quién ha puesto cuánto cada mes.

12. **IVA pagado por año y por tramo** (4 %, 10 %, 21 %). Sirve sobre todo para interpretar
    MiInflación: entre 2023 y 2024 hubo rebajas temporales del IVA de alimentos básicos. Con
    esta vista se distingue qué parte de la variación de precios viene del impuesto y cuál del
    precio neto.

## D. Vistas analíticas calculadas en Java, como MiInflación

13. **De dónde viene la subida del gasto.** Si el gasto anual sube un X %, cuánto se debe a
    precios más altos y cuánto a comprar más cantidad. Con Laspeyres ya calculado es casi
    directo: la variación del gasto es el índice de precios por el índice de cantidades.
    Responde a la pregunta real de gestión: "¿gasto más porque todo sube o porque compramos
    más?".

14. **Qué productos empujan la inflación.** Aportación de cada producto de la cesta al índice
    (su peso por su variación de precio). Suele ocurrir que 3 o 4 productos, como aceite,
    huevos o café, explican gran parte de la inflación personal.

15. **Inflación propia frente al IPC oficial.** El índice anual propio al lado del IPC de
    alimentos del INE, que se metería a mano en la plantilla.

16. **Alertas de precio.** Productos cuya última compra es más cara que la anterior por encima
    de un umbral, por ejemplo +10 %.

17. **Presupuesto frente a lo gastado.** Una hoja con un presupuesto mensual editable, la
    desviación acumulada y la proyección del mes en curso a partir del ritmo diario de gasto.

---

## Prioridad recomendada

- **4 (precio ponderado):** arregla la vista de evolución de precios que ya existe y es solo un
  cambio en la plantilla.
- **6 (categoría):** es la dimensión que más vistas nuevas abre.
- **8 (ticket y hora):** cambio de pocas líneas que habilita todo el análisis por visita.

## Nota de implementación

Una columna nueva en `Datos` obliga a cambiar tres cosas a la vez:

- `DATOS_COLUMN_ORDER` en `XlsxDatosWriter`.
- El rango de origen de la caché de tablas dinámicas en la plantilla `Mercadona-base.xlsx`.
- El orden de columnas de `PurchasedItemRecord.toCSV()` / `headerCSV()`.

## Implementado

### A.1 — Hoja `Pareto` (2026-09-25)

Añadida a `Mercadona-base.xlsx`, justo después de "Productos":

- Tabla dinámica `TablaDinámicaPareto` sobre la caché común: productos (`id`) ordenados por
  gasto descendente, con "Gasto", "% del gasto" (% del total) y "Nº compras"; filtro de página
  por Años (selección múltiple); elemento "(en blanco)" oculto.
- Columna E "% acumulado" y columna F "Clase" (A ≤ 80 %, B ≤ 95 %, C resto, decidido por el
  acumulado *antes* del producto) como **fórmulas fuera de la tabla dinámica**, precargadas
  hasta la fila 2000, y resumen por clase en H7:K11 con formato condicional.
- El "% del total en ejecución" nativo de la tabla dinámica (`showDataAs="runTotal"` + extensión
  x14 `percentOfRunningTotal`) no funcionó al escribirlo a mano en el XML: Excel mostraba el
  acumulado en € con formato %. Por eso el acumulado es una fórmula.
- La fila del total general se detecta por ser la última de la tabla, no por su texto
  ("Total general"/"Total Result" según idioma).
- `workbook.xml` lleva `fullCalcOnLoad="1"` para que las fórmulas se recalculen tras el
  refresco de la caché al abrir.

### A.2 — Hoja `Interanual` (2026-09-25)

Añadida a `Mercadona-base.xlsx`, justo antes de "T_Precio":

- Tabla dinámica `TablaDinámicaInteranual` sobre la caché común: meses (agrupación de `fecha`) en
  filas, Años en columnas, suma de precio; "(en blanco)" oculto.
- Debajo, el % de variación de cada mes frente al mismo mes del año anterior como **fórmulas**
  que leen la tabla por posición (misma fila = mismo mes, columna del año frente a la anterior),
  con rojo/verde por formato condicional. No se usa `showDataAs="percentDiff"` con
  `baseItem` "(anterior)" en una segunda tabla dinámica porque LibreOffice no lo calcula sobre un
  campo agrupado (`#VALUE!`) y no se podía verificar fuera de Excel.
- En lugar de comparar totales anuales (engañoso con el primer/último año incompletos), la fila
  "Total (meses comparables)" solo suma los meses con gasto en los dos años.
