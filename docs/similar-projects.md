# Proyectos similares de dominio público

**Fecha del análisis: 2026-09-10.**

Búsqueda en internet, GitHub y similares para ver si existe algún programa de dominio
público (cualquier lenguaje) con funcionalidades parecidas a `ticket-digital`: descarga
automática de tickets digitales de Mercadona desde Gmail, parseo del PDF a items
estructurados, generación de CSV/XLSX, y cálculo de un índice de inflación personal
(Laspeyres/Paasche) a partir del propio histórico de compras.

## Conclusión

No se ha encontrado ningún proyecto público que haga el conjunto completo (descarga
Gmail + parseo a items + CSV/XLSX + índice de inflación personal). Esa combinación, y
sobre todo la parte del índice de inflación, parece bastante propia de este proyecto.
Sí hay piezas sueltas comparables, descritas abajo.

Si el objetivo fuera evaluar si merece la pena seguir con este proyecto en vez de
adoptar/contribuir a otro: [`invoice2data`](https://github.com/invoice-x/invoice2data)
es la única alternativa con entidad real (comunidad, mantenimiento, arquitectura
genérica) — pero tocaría escribirle una plantilla de Mercadona y añadirle por cuenta
propia la descarga de Gmail y el cálculo de inflación, es decir, reconstruir buena parte
de lo que ya hay aquí.

## Parseadores de tickets de Mercadona específicamente

Todos más pequeños o menos completos que este proyecto:

- [`edugzlez/mdona-scrapper`](https://github.com/edugzlez/mdona-scrapper) — Python,
  licencia MIT, publicado en PyPI. Extrae items, nº de pedido/factura y fecha a un
  DataFrame de pandas. El PDF hay que dárselo a mano (sin descarga de Gmail), sin XLSX
  ni inflación. Muy pequeño: 0 estrellas, 9 commits.
- [`hiancdtrsnm/mercadona-ticket-ui`](https://github.com/hiancdtrsnm/mercadona-ticket-ui)
  — web (Astro/TypeScript) para subir el PDF a mano y ver la lista de productos + total.
  Sin licencia declarada, 3 commits.
- [`d3vv3/mercaticket`](https://github.com/d3vv3/mercaticket) — Next.js, licencia MIT.
  Subida manual del PDF, pero enfocado en **información nutricional** (usa IA sobre el
  catálogo de Mercadona), no en gasto ni inflación.
- [`josepablodmg/SQL---Creating-Mercadona-database-from-receipt`](https://github.com/josepablodmg)
  — vuelca un ticket a una base SQL. Muy pequeño/anecdótico (0 estrellas).

Dos proyectos encontrados **no son públicos** (repos privados), pero confirman que la
idea de partir del email `ticket_digital@mail.mercadona.com` no es única de este
proyecto:
- Jesús López Martín — dashboard de Power BI a partir de los tickets del email, con
  seguimiento de precios (artículo en LinkedIn, repo privado).
- Antonio Blanco (`ablancodev`) — usa la librería `PDFParse` + ChatGPT para interpretar
  el PDF (artículos en Medium); no se encontró un repo público correspondiente en su
  perfil de GitHub.

## Motor genérico de parseo de recibos/facturas

No específico de Mercadona, pero es la pieza más madura y reutilizable encontrada:

- [`invoice-x/invoice2data`](https://github.com/invoice-x/invoice2data) — licencia MIT,
  ~2.2k estrellas, muy activo. Plantillas YAML/regex por comercio + plugin para extraer
  líneas de producto, exporta a CSV/JSON/XML. No trae plantilla de Mercadona de fábrica
  (habría que escribirla) y tampoco hace descarga de email ni cálculo de inflación.

## Scrapers de catálogo/precios de Mercadona

No parsean *tus* tickets, sino el catálogo público de la tienda — útiles solo si algún
día se quisiera comparar precios pagados contra el catálogo actual:

- [`m0wer/mercaapi`](https://github.com/m0wer/mercaapi)
- [`nicolaspascual/mercadona-scrapper`](https://github.com/nicolaspascual/mercadona-scrapper)
- [`vgvr0/supermarket-mercadona-scraper`](https://github.com/vgvr0/supermarket-mercadona-scraper)

## Parseador genérico de recibos de supermercado (no Mercadona)

- [`EnriqueGlezGM/Supermercado`](https://github.com/EnriqueGlezGM/Supermercado) —
  React/Vite, PDF+OCR, categoriza el gasto (menciona Lidl explícitamente). Sin licencia
  declarada.

## Índice de inflación personal

No se encontró ningún proyecto open source que calcule un Laspeyres/Paasche real a
partir de tickets parseados. Lo más cercano conceptualmente es
[tu-ipc.es](https://tu-ipc.es/), pero es una calculadora de gasto por categorías
introducido a mano, no algo derivado de tickets, y no hay confirmación de que su código
sea público.
