# ticket-digital

Descarga tus tickets digitales de Mercadona desde Gmail y genera un CSV o un XLSX con
el detalle de todo lo comprado, para analizar tu gasto y cómo han evolucionado los
precios de los productos que compras.

> **Este proyecto no tiene ninguna relación con Mercadona** ni está afiliado a la
> empresa de ninguna forma. No intenta obtener más información que la que ya está
> impresa en tus propios tickets: se limita a leer ese texto y a analizarlo. Está
> pensado para usarlo con **tus propios** tickets, no como agregador o base de datos de
> tickets de otras personas — ver [Limitaciones](#limitaciones-del-ticket-digital) sobre
> por qué un ticket ajeno no se puede dar por válido.

## Índice

- [Objetivo del proyecto](#objetivo-del-proyecto)
- [Cómo funciona](#cómo-funciona)
- [Módulos](#módulos)
- [Funcionalidades](#funcionalidades)
- [Instalación y uso](#instalación-y-uso)
- [Calidad y seguridad](#calidad-y-seguridad)
- [Limitaciones del ticket digital](#limitaciones-del-ticket-digital)
- [Alternativas libres similares](#alternativas-libres-similares)
- [Licencia](#licencia)

## Objetivo del proyecto

Es un proyecto personal: quería poder analizar mi propio histórico de compra en
Mercadona (cuánto gasto, en qué, y cómo suben los precios de los productos que compro
habitualmente) a partir de los tickets digitales que la propia Mercadona envía por
email. Se ha intentado escribir de forma algo más genérica que mi caso particular, para
que le pueda servir a cualquiera que reciba ese mismo tipo de correo — pero sigue siendo
una herramienta de uso personal, no un producto ni un servicio.

Un segundo objetivo, que no estaba en la idea inicial, es que este proyecto también me
sirve como banco de pruebas para programar con ayuda de IA — en concreto, con
[Claude Code](https://claude.com/claude-code). Al no ser el propósito original, una
parte importante del código (sobre todo la más antigua) no se escribió así; el proyecto
convive con ambos orígenes.

**Estado del proyecto**: son fases muy iniciales (de ahí la versión `0.1-SNAPSHOT`, sin
API ni CLI estables todavía), pero ya en un punto sólido: el parser ha procesado
correctamente más de 500 tickets reales.

## Cómo funciona

El proceso completo tiene tres pasos, cada uno a cargo de un módulo distinto:

1. **Descargar** los PDF de los tickets nuevos desde Gmail
   (`ticket-digital-tickets-downloader`).
2. **Leer** cada PDF y convertirlo en un objeto Java con la información estructurada
   (`ticket-digital-parser`).
3. **Analizar** ese histórico y generar un CSV, un XLSX o un índice de inflación
   (`ticket-digital-data`, que usa el paso 2 internamente).

Los tres pasos se invocan con un único comando a través de `ticket-digital-launcher`,
la CLI recomendada para el uso normal.

## Módulos

Proyecto Gradle multi-módulo: cada uno se puede compilar y ejecutar por separado
(cargando solo sus propias dependencias), y hay un módulo agregador con todo junto.

| Módulo | Qué hace | README |
|---|---|---|
| [`ticket-digital-launcher`](projects/ticket-digital-launcher/README.md) | **CLI recomendada para uso normal**: un único comando `ticket-digital` con todos los subcomandos de los dos módulos siguientes. | [README](projects/ticket-digital-launcher/README.md) |
| [`ticket-digital-tickets-downloader`](projects/ticket-digital-tickets-downloader/README.md) | Descarga desde Gmail los PDF de tickets nuevos a un directorio local. | [README](projects/ticket-digital-tickets-downloader/README.md) |
| [`ticket-digital-data`](projects/ticket-digital-data/README.md) | Genera CSV/XLSX y calcula índices de gasto a partir de los PDF ya descargados. | [README](projects/ticket-digital-data/README.md) |
| [`ticket-digital-parser`](projects/ticket-digital-parser/README.md) | Librería de parseo: lee un PDF de ticket de Mercadona y lo convierte en un objeto Java estructurado. Sin CLI propia; la usa `ticket-digital-data` internamente. | [README](projects/ticket-digital-parser/README.md) |

Dos de ellos solo funcionan bajo ciertas condiciones, que conviene tener claras antes de
empezar:

- **`ticket-digital-tickets-downloader`** necesita que (a) estés suscrito al ticket
  digital de Mercadona (lo emite la propia tienda al pagar, al email que le indiques),
  (b) esos correos lleguen a una cuenta de Gmail, y (c) tengas alguna forma de
  localizarlos entre el resto de tu correo — por defecto se buscan por remitente
  (`ticket_digital@mail.mercadona.com`) y por una etiqueta de Gmail que debes crear tú
  mismo (`label:mercadona`); el [README del módulo](projects/ticket-digital-tickets-downloader/README.md)
  explica cómo configurarlo y cómo obtener las credenciales de Google necesarias.
- **`ticket-digital-data`** genera, además del CSV, un XLSX a partir de una plantilla
  que ya trae varias tablas dinámicas de ejemplo en la hoja **"Datos"** (y un análisis de
  inflación en la hoja "MiInflación"): el programa solo sustituye los datos en bruto,
  Excel se encarga de refrescar las tablas dinámicas al abrir el fichero.

## Funcionalidades

Subcomandos disponibles (los mismos en los tres módulos con CLI — ver cada README para
el detalle de sus opciones):

| Subcomando | Qué hace |
|---|---|
| `download-tickets` | Descarga desde Gmail los PDF de tickets que aún no tengas. |
| `write-items-csv` | Genera un CSV con todas las líneas de producto compradas. |
| `write-items-xlsx` | Genera un XLSX con esas mismas líneas y varias tablas dinámicas de ejemplo ya preparadas. |
| `inflation-index` | Calcula cómo ha subido el precio de tu cesta habitual, año a año (índices de Laspeyres y Paasche). |
| `basket-composition` | Muestra de qué se compone esa cesta y qué peso tiene cada producto en el gasto. |

## Instalación y uso

**Requisitos**: Java 25 (el propio `./gradlew` se encarga de todo lo demás, no hace
falta instalar Gradle). El `sourceCompatibility`/`targetCompatibility` de todos los
módulos está fijado a esa versión en el `build.gradle` raíz.

Gradle aquí es únicamente la **herramienta de construcción**: se usa para compilar y
generar el artefacto, pero no para ejecutarlo día a día. El flujo es siempre "construir
una vez, ejecutar después sin Gradle" con el script ya generado.

Vía el plugin `application`, Gradle genera para cada módulo con CLI un script de
arranque con todas sus dependencias (no un único "fat jar": el `.jar` suelto en
`build/libs/` no lleva las dependencias y **no** es ejecutable directamente con
`java -jar`). El entregable pensado para el uso normal es la **distribución de
`ticket-digital-launcher`**, que trae empaquetados los otros dos módulos ejecutables:

```bash
# Construir: genera el script + las dependencias en build/install/ticket-digital-launcher/
./gradlew :ticket-digital-launcher:installDist
```

```bash
# Ejecutar (Linux/macOS) — sin Gradle a partir de aquí
LAUNCHER=./projects/ticket-digital-launcher/build/install/ticket-digital-launcher/bin/ticket-digital-launcher

# Ver todos los subcomandos disponibles
$LAUNCHER --help

# Descargar los tickets nuevos desde Gmail
$LAUNCHER download-tickets

# Generar el CSV a partir de los PDF ya descargados
$LAUNCHER write-items-csv --data-dir=~/.ticket-digital/data --csv-file=~/tickets.csv

# Generar el XLSX
$LAUNCHER write-items-xlsx --data-dir=~/.ticket-digital/data --output-dir=~/

# Índice de inflación de la cesta y su composición
$LAUNCHER inflation-index --data-dir=~/.ticket-digital/data
$LAUNCHER basket-composition --data-dir=~/.ticket-digital/data

# Cualquier subcomando con -v/--verbose para ver el detalle en DEBUG
$LAUNCHER write-items-csv --data-dir=~/.ticket-digital/data --csv-file=~/tickets.csv -v
```

```bat
:: En Windows: usa el .bat generado en la misma carpeta
projects\ticket-digital-launcher\build\install\ticket-digital-launcher\bin\ticket-digital-launcher.bat --help
```

Ese directorio (`build/install/ticket-digital-launcher/`) es autocontenido: se puede
copiar a otra máquina con Java (misma versión o superior) y ejecutarse ahí sin Gradle ni
este repositorio; añadiendo su `bin/` al `PATH` se puede invocar directamente
`ticket-digital-launcher` sin la ruta completa. Para empaquetarlo y llevarlo a otro
sitio en un único fichero:

```bash
./gradlew :ticket-digital-launcher:distZip   # o :distTar
# genera projects/ticket-digital-launcher/build/distributions/ticket-digital-launcher-0.1-SNAPSHOT.zip
```

La primera vez, sigue el [README de `ticket-digital-tickets-downloader`](projects/ticket-digital-tickets-downloader/README.md#1-obtener-credentialsjson-desde-google-cloud-console)
para obtener el fichero `credentials.json` (necesario solo para descargar; no hace falta
si ya tienes los PDF en un directorio).

Cada módulo también se puede construir y ejecutar solo (sin cargar las dependencias de
los demás), sustituyendo el nombre del módulo en los mismos comandos
(`:ticket-digital-data:installDist`, `:ticket-digital-tickets-downloader:installDist`) —
ver su README para el detalle de opciones de cada subcomando.
`ticket-digital-parser` no tiene distribución propia porque no tiene CLI: es una
librería que consumen los otros módulos en tiempo de compilación.

## Calidad y seguridad

`./gradlew build` (o `./gradlew check`) ejecuta además, en todos los módulos:

- **[OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/)**: escanea
  las dependencias de los 4 módulos en busca de vulnerabilidades (CVE) conocidas.
  Informe en `build/reports/dependency-check/dependency-check-report.html` (raíz del
  repo). Por defecto
  el build falla si aparece algún hallazgo de severidad alta/crítica (CVSS ≥ 7).

  Usa como fuente de datos la NVD (National Vulnerability Database) de NIST, vía su API
  2.0, que rechaza toda petición sin clave. **Mientras no configures una API key**, el
  build detecta su ausencia antes de invocar la tarea `dependencyCheckAggregate`, la
  omite y muestra un banner bien visible en la salida de `./gradlew build`/`check`
  avisando de que el escaneo no se ha ejecutado — pero el build **no se interrumpe**.

  Pide la API key gratuita (autoservicio, instantánea, activación por email) en
  https://nvd.nist.gov/developers/request-an-api-key y guárdala **fuera del repo**, en
  `~/.gradle/gradle.properties` (créalo si no existe):

  ```properties
  nvdApiKey=tu-api-key-aquí
  ```

  Nunca la pongas en un fichero del proyecto ni la subas al repo.

  Si algún día aparece un falso positivo, se puede suprimir con
  [`dependencyCheck.suppressionFile`](https://dependency-check.github.io/DependencyCheck/dependency-check-gradle/configuration.html)
  apuntando a un XML de supresiones — no hay ninguno todavía porque no ha hecho falta.

- **[PMD](https://pmd.github.io/)** (`toolVersion 7.27.0`, incluido en Gradle sin plugin
  externo): analiza el código en busca de bugs y malas prácticas reales (ruleset propio
  en [`config/pmd/ruleset.xml`](config/pmd/ruleset.xml), centrado en
  `errorprone`/`bestpractices`, no en estilo/formato). Informe por módulo en
  `<módulo>/build/reports/pmd/main.html` y `test.html`. Bloquea el build ante cualquier
  hallazgo (`ignoreFailures = false`); las exclusiones ya investigadas y justificadas
  están documentadas con su motivo en el propio ruleset.

  Las reglas de concurrencia (`category/java/multithreading.xml`) se configuran aparte,
  por módulo, según si está pensado para ser thread-safe:
  [`config/pmd/multithreading-strict.xml`](config/pmd/multithreading-strict.xml) (sin
  ninguna exclusión) para `ticket-digital-parser` — pensado como base de un futuro
  servicio REST, donde varias peticiones concurrentes reutilizarán las mismas clases — y
  [`config/pmd/multithreading-relaxed.xml`](config/pmd/multithreading-relaxed.xml) para
  el resto de módulos (CLIs de un solo hilo sin planes de dejar de serlo).

- **[SpotBugs](https://spotbugs.github.io/)** (`toolVersion 4.10.4`, plugin
  `com.github.spotbugs`): segunda capa de análisis, complementaria a PMD — en vez de
  patrones sobre el código fuente, analiza el bytecode compilado con dataflow analysis,
  lo que detecta cosas que PMD no ve (p.ej. un posible NullPointerException por el valor
  de retorno de un método, o un `record` que expone una lista/mapa mutable interno sin
  copia defensiva, rompiendo su propia inmutabilidad). Soporta Java 25 desde la 4.9.7
  (subieron BCEL a la 6.11.0; antes de eso no se podía usar en este proyecto —
  [spotbugs/spotbugs#3564](https://github.com/spotbugs/spotbugs/issues/3564)). Informe
  por módulo en `<módulo>/build/reports/spotbugs/main.html` y `test.html`. Bloquea el
  build ante cualquier hallazgo (`ignoreFailures = false`).

## Limitaciones del ticket digital

Dos limitaciones que vienen del propio concepto de "ticket digital" de Mercadona, no de
este programa (desarrolladas con más detalle en el
[README de `ticket-digital-parser`](projects/ticket-digital-parser/README.md#limitaciones-del-propio-ticket-digital-no-de-este-código)):

- **No hay forma de verificar que un ticket sea real y no haya sido modificado.** El PDF
  no lleva firma ni sello: el programa se limita a interpretar su texto y da por buena
  su veracidad. Por eso está pensado para tus propios tickets, no para actuar como
  agregador de tickets de terceros.
- **La descripción de un producto no es un identificador único y estable en el tiempo.**
  Es el único dato disponible para saber qué se compró; si Mercadona cambia el formato o
  el tamaño de un envase sin cambiar el texto impreso, el histórico tratará ambos
  productos como si fueran el mismo.

## Alternativas libres similares

No se ha encontrado ningún proyecto público que haga el conjunto completo (descarga
desde Gmail + parseo a items + CSV/XLSX + índice de inflación personal); esa combinación
parece bastante propia de este proyecto. Sí existen piezas sueltas comparables: varios
parseadores de tickets de Mercadona más pequeños o incompletos (requieren subir el PDF a
mano, sin descarga ni análisis de inflación), y un motor genérico de parseo de facturas,
[`invoice2data`](https://github.com/invoice-x/invoice2data) (MIT, activo), al que le
faltaría una plantilla de Mercadona y toda la parte de descarga/inflación. El análisis
completo, con enlaces a cada proyecto, está en
[`docs/similar-projects.md`](docs/similar-projects.md).

## Licencia

Este proyecto se distribuye bajo la [GNU GPL versión 3](gpl-3.0.md). En resumen: es
software libre, y esa libertad está protegida con copyleft fuerte — puedes usarlo,
estudiarlo, modificarlo y redistribuirlo, pero **cualquier trabajo derivado que
distribuyas tiene que licenciarse también bajo GPL-3.0** (código abierto, con el
código fuente disponible). No se puede tomar este código para cerrarlo en un producto
o servicio propietario.
