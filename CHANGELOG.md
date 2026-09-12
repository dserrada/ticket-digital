# Changelog

Registro de cambios del proyecto. Formato basado en
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); versionado según
[SemVer](https://semver.org/lang/es/).

De momento solo se registran aquí los cambios que llevan asociado un cambio de versión
(es decir, cada release), no cada commit individual.

## [0.0.1] - 2026-09-12

Primera release del proyecto. Estado inicial pero funcional: el parser ha procesado
correctamente más de 500 tickets reales.

### Añadido

- Descarga de los PDF de tickets digitales de Mercadona desde Gmail
  (`ticket-digital-tickets-downloader`).
- Parseo de esos PDF a un objeto Java estructurado: productos (incluidos frescos),
  total, desglose de IVA y datos de pago con tarjeta (`ticket-digital-parser`).
- Generación de CSV y XLSX con el detalle de todas las líneas de producto compradas,
  y cálculo de índice de inflación de la cesta habitual (Laspeyres y Paasche) y de su
  composición (`ticket-digital-data`).
- CLI unificada (`ticket-digital-launcher`) con los subcomandos `download-tickets`,
  `write-items-csv`, `write-items-xlsx`, `inflation-index` y `basket-composition`.
- Análisis de calidad y seguridad en el build: OWASP Dependency-Check, PMD y SpotBugs.

[0.0.1]: https://github.com/dserrada/ticket-digital/releases/tag/v0.0.1
