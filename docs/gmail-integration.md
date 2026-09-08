# Integrar la descarga de tickets desde Gmail en el proyecto

> Documento de análisis + diseño de implementación (Opción A, con las
> modificaciones acordadas). Pendiente de implementar.

## Contexto

Hoy el flujo es manual en dos pasos: se ejecuta a mano
`GmailAttachmentsExtractor.jar` (herramienta de terceros,
https://github.com/TeWu/GmailAttachmentsExtractor, copia local en
`C:\java\GMailExtractor`) para bajar los PDFs de los tickets de Mercadona desde
Gmail a un directorio, y luego se apunta `write-items-csv`/`write-items-xlsx`
a ese directorio. Se quiere integrar el primer paso (la descarga) dentro del
propio proyecto `ticket-digital`, en vez de depender de una herramienta CLI
externa suelta.

Investigando la herramienta actual para valorar si es viable "engancharla"
como librería:

- **Es un jar fat/shaded** (Maven `assembly-plugin`, un solo jar con todo
  volcado dentro, sin relocación de paquetes): incluye `com.google.api.client.*`,
  `com.google.api.services.gmail.*`, `com.sun.mail.*` (JavaMail) y **picocli
  4.2.0** — justo la misma librería que ya usa este proyecto, pero en
  `4.7.6`. No publica ningún artefacto de librería por separado: no hay forma
  limpia de añadirlo como dependencia de Gradle sin arriesgarse a choques de
  versión en el classpath (empezando por picocli).
- **El código propio de la herramienta es pequeño**: un único paquete
  `pl.geek.tewu.gmail_attachments_extractor` con ~8 clases (`Main`, `Options`,
  `GmailAttachmentsExtractor`, `GmailInit`, `AccessibleMimeMessage`, `Utils`,
  `DigestUtils`, `AppInfo`). Licencia **Apache-2.0** (permite reutilizar/forkear
  el código con atribución).
- **El repositorio está parado**: último commit `2021-05-27`, última release
  `1.0.3` (`2021-04-14`), y uno de los últimos mensajes de commit es
  literalmente *"Mark as unstable version"*. No es un proyecto activamente
  mantenido — apoyarse en él a largo plazo (con jar o con fork) hereda ese
  estancamiento y unas dependencias de Google de 2018 (`google-api-client
  1.23.0`, `google-api-services-gmail v1-rev83-1.23.0`).
- **El uso real es muy acotado** (visto en `command.txt`):
  ```
  java -jar GmailAttachmentsExtractor.jar \
    "label:mercadona from: ticket_digital@mail.mercadona.com" \
    --no-modify-gmail --filename '.*\.pdf$' .\mails\
  ```
  Es decir: una búsqueda de Gmail por query, un filtro de nombre de adjunto
  por regex, y una carpeta de salida. No usa ninguna otra característica de
  la herramienta (no reenvía, no marca como leído, no reescribe correos).
- El `credentials.json` que ya está configurado es un cliente OAuth de tipo
  **"installed" (app de escritorio)** estándar de Google — totalmente
  reutilizable tal cual con una integración propia, sin tener que crear un
  proyecto nuevo en Google Cloud Console ni repetir el consentimiento (mismas
  credenciales, mismo scope `gmail.readonly`).

## Respuesta directa: ¿hay algún proyecto Java que permita una integración más natural?

Sí: las propias librerías oficiales de Google —
`com.google.api-client:google-api-client`,
`com.google.oauth-client:google-oauth-client-jetty` y
`com.google.apis:google-api-services-gmail` — son exactamente eso. Son las
mismas librerías sobre las que está construido `GmailAttachmentsExtractor`,
pero publicadas en Maven Central, mantenidas activamente por Google, y
usables como dependencia normal de Gradle. No hace falta la herramienta de
terceros para tener una integración "de librería" — la propia herramienta es
solo una fina capa de CLI encima de esa librería oficial.

(Única alternativa de terceros digna de mención: el componente
`camel-google-mail` de Apache Camel. Descartado salvo que ya se use Camel en
otro sitio — es un framework de integración completo, desproporcionado para
esto.)

## Opciones valoradas

### Opción A — Integración directa contra la librería oficial de Gmail (recomendada)
Añadir como dependencias normales de Gradle `google-api-client`,
`google-oauth-client-jetty` (o su sucesor `google-auth-library-oauth2-http`) y
`google-api-services-gmail` (versión actual, no la de 2018), y escribir una
clase nueva que reproduce exactamente el caso de uso actual: autenticar,
buscar mensajes con una query, recorrer los adjuntos y guardar los PDF.

- **Ventajas**: dependencia versionada y mantenida por Google, sin conflictos
  de classpath (se eligen las versiones, no vienen precocinadas en un fat
  jar), integración real de código (nuevo subcomando picocli, igual que
  `write-items-csv`/`write-items-xlsx`), reutiliza tal cual el
  `credentials.json` ya existente.
- **Inconvenientes**: hay que escribir el código de búsqueda/descarga (aunque
  es poco: listar mensajes paginado, recorrer `payload.parts` buscando el
  adjunto, pedir los bytes en base64url y guardarlos — no hace falta ni
  JavaMail, la propia API de Gmail ya da las partes MIME).

### Opción B — Invocar el jar existente como proceso externo
Un subcomando que lanza `java -jar GmailAttachmentsExtractor.jar ...` vía
`ProcessBuilder` con los mismos argumentos actuales.

- **Ventajas**: mínimo código nuevo, reutiliza la herramienta tal cual, ya
  probada.
- **Inconvenientes**: sigue dependiendo de un jar suelto sin gestionar por
  Gradle (ruta hardcodeada o que aportar como recurso), nada de seguridad de
  tipos, y sigue atado a una herramienta parada desde 2021 marcada como
  "inestable" por su propio autor — es automatizar la dependencia actual, no
  quitársela de encima. Es la opción más alejada de "integración natural".

### Opción C — Vendorizar/forkear el código fuente de la herramienta
Copiar las ~8 clases de `pl.geek.tewu.gmail_attachments_extractor` (Apache-2.0,
permite hacerlo con atribución) a un módulo propio, ajustando lo necesario.

- **Ventajas**: parte de lógica ya escrita (recorrido de adjuntos vía
  JavaMail, manejo de paginación).
- **Inconvenientes**: hereda dependencias de 2018 y una base de código ajena y
  parada que pasaría a mantenerse en este proyecto; añade JavaMail como
  dependencia extra sin necesidad real (la Opción A no la necesita). No aporta
  ventaja clara sobre la Opción A dado lo acotado del caso de uso real.

## Recomendación

**Opción A.** El caso de uso actual es pequeño y bien definido (una query, un
filtro de nombre de fichero, una carpeta de salida), así que el coste de
escribirlo directamente contra la librería oficial de Google es bajo, y evita
tanto el conflicto de classpath del jar fat como la dependencia de un
proyecto de terceros parado desde 2021.

## Decisiones acordadas (revisión sobre el boceto inicial)

Sobre la Opción A, con estas modificaciones respecto al boceto original:

1. **Nombre del módulo**: `ticket-digital-tickets-downloader` (en vez de
   `ticket-digital-gmail`) — el nombre refleja el propósito (descarga de
   tickets), no el proveedor concreto (Gmail) usado por dentro.
2. **Directorio de datos por defecto**: `~/.ticket-digital/data` (en vez de
   una ruta relativa al proyecto), para no depender de dónde se ejecute el
   comando.
3. **Descarga incremental**: no se vuelve a descargar todo en cada
   ejecución; solo se trae lo nuevo desde la última vez.
4. **Secretos fuera del repositorio, siempre**: nunca se comitea
   `credentials.json` ni el token OAuth. Ambos viven bajo `~/.ticket-digital/`
   (fuera del repo) por defecto, con opción de override por línea de
   comandos.
5. **`README.md` del módulo**: guía paso a paso de cómo obtener
   `credentials.json` desde Google Cloud Console, más instrucciones de uso en
   línea de comandos (incluyendo el equivalente al uso actual con
   `GmailAttachmentsExtractor.jar` de `command.txt`).
6. **Alcance OAuth de solo lectura, verificado en runtime**: se solicita
   únicamente `gmail.readonly`; si el token obtenido tuviera un alcance mayor
   (por ejemplo, un token reutilizado de la herramienta anterior con permisos
   de modificación), el programa aborta antes de tocar la API de Gmail.
7. **CLI standalone**: el módulo nuevo tiene su propio punto de entrada
   picocli, ejecutable sin depender de `ticket-digital-data`.
8. **Módulo agregador `ticket-digital-launcher`** (revisión posterior, ver
   nota más abajo): en vez de que `ticket-digital-data` dependa
   directamente de `ticket-digital-tickets-downloader`, ambos quedan
   completamente independientes (cada uno solo carga sus propias
   dependencias), y un tercer módulo nuevo compone los subcomandos de los
   dos en un único CLI "todo en uno", sin duplicar código.

## Boceto de implementación (con las decisiones anteriores)

1. **Nuevo módulo** `ticket-digital-tickets-downloader`
   (`projects/ticket-digital-tickets-downloader`, añadido en
   `settings.gradle` junto a `ticket-digital-parser` y `ticket-digital-data`),
   con su propio código de descarga y su propia CLI standalone. No depende
   de `ticket-digital-data`, ni al revés: cada uno se compila, distribuye y
   ejecuta solo, cargando únicamente sus propias dependencias
   (`ticket-digital-data` no carga las librerías de Google si solo se usa
   para `write-items-csv`/`write-items-xlsx`, que no tocan Gmail).

   **Módulo agregador `ticket-digital-launcher`**: para quien quiera un
   único CLI con todo, un tercer módulo nuevo depende de los dos y compone,
   programáticamente con la API de `picocli` (`CommandSpec.addSubcommand`),
   los `CommandLine` que cada `Main` ya construye para sí mismo —
   `write-items-csv`/`write-items-xlsx` de `ticket-digital-data` y
   `download-tickets` de `ticket-digital-tickets-downloader` — como
   subcomandos directos de un único comando raíz `ticket-digital`. No
   duplica opciones ni lógica: reutiliza tal cual las clases de comando de
   cada módulo.

2. **Clase de descarga** (p. ej. `GmailTicketDownloader`):
   - Autenticación: `GoogleAuthorizationCodeFlow` + `LocalServerReceiver`
     (flujo "installed app" estándar), scope **únicamente**
     `https://www.googleapis.com/auth/gmail.readonly`.
   - **Validación de alcance antes de descargar**: tras obtener el
     `Credential`, se consulta el endpoint de tokeninfo de Google
     (`https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=...`) y se
     comprueba que el campo `scope` devuelto es exactamente
     `gmail.readonly` (nada de `gmail.modify`, `mail.google.com`,
     `gmail.settings.*`, etc.). Si no lo es, se aborta con un error claro
     *antes* de hacer ninguna llamada a `users().messages()...` — protege
     tanto contra un cambio accidental de scope en el código como contra la
     reutilización de un token antiguo con permisos más amplios (p. ej. el de
     la herramienta actual).
   - Cliente `Gmail`; pagina `users().messages().list(userId, q=<query>)`.
   - **Descarga incremental**: se guarda un fichero de estado local (p. ej.
     `~/.ticket-digital/state/gmail-download-state.json`) con la fecha/hora
     del último mensaje procesado con éxito. En cada ejecución, la query
     efectiva añade una cláusula `after:<fecha del último estado>` (con
     margen de solape de un día para cubrir zonas horarias/mensajes tardíos),
     de forma que Gmail ya filtra la mayoría de mensajes ya vistos. Como
     doble comprobación (y para cubrir el solape), por cada adjunto candidato
     se comprueba si el fichero ya existe en `--data-dir` (mismo nombre) antes
     de descargarlo, y se salta si ya está.
   - Por cada mensaje nuevo, `users().messages().get(...)`, recorre
     `payload.parts` recursivamente buscando partes con `filename` que
     matchee el regex (p. ej. `.*\.pdf$`) y `body.attachmentId` no nulo;
     descarga cada adjunto con `users().messages().attachments().get(...)`
     (base64url) y lo escribe en `--data-dir`.

3. **CLI standalone** (picocli) en el propio módulo, ejecutable de forma
   independiente (`./gradlew :ticket-digital-tickets-downloader:run --args=...`
   o vía jar ejecutable), con las opciones:
   ```
   download-tickets \
     --data-dir ~/.ticket-digital/data \
     --credentials-file ~/.ticket-digital/credentials.json \
     --query "label:mercadona from: ticket_digital@mail.mercadona.com" \
     --filename-regex '.*\.pdf$'
   ```
   `--data-dir`, `--credentials-file` y `--query`/`--filename-regex` con los
   valores actuales como default, para poder invocarlo sin argumentos en el
   caso de uso habitual. `ticket-digital-launcher` añade este mismo comando
   (`download-tickets`) reutilizando esta clase tal cual (ver punto 1),
   cumpliendo a la vez "standalone" e "integrado" sin duplicar lógica ni
   acoplar `ticket-digital-data` a las dependencias de Google.

4. **Credenciales y secretos, siempre fuera del repo**:
   - `credentials.json`: **no se comitea nunca**. Por defecto se busca en
     `~/.ticket-digital/credentials.json`; se puede sobreescribir con
     `--credentials-file`. Se añade `credentials.json` (y cualquier ruta bajo
     `~/.ticket-digital/`) a `.gitignore` como refuerzo, y ningún test ni
     recurso del repo incluye un `credentials.json` real (a lo sumo un
     fixture claramente falso para tests).
   - Token OAuth: se guarda en `~/.ticket-digital/tokens/` (equivalente a la
     carpeta `tokens` actual de la herramienta), también fuera del repo.
   - Fichero de estado de descarga incremental: `~/.ticket-digital/state/`.

5. **`README.md`** en `ticket-digital-tickets-downloader/`:
   - Paso a paso para obtener `credentials.json` desde Google Cloud Console:
     crear/seleccionar proyecto, habilitar la API de Gmail, configurar la
     pantalla de consentimiento OAuth, crear credenciales de tipo "Aplicación
     de escritorio" (Desktop app), descargar el JSON y colocarlo en
     `~/.ticket-digital/credentials.json`.
   - Instrucciones de uso en línea de comandos: ejemplo de invocación con los
     valores por defecto, y el ejemplo equivalente al uso actual documentado
     en `command.txt` de `GmailAttachmentsExtractor`:
     ```
     java -jar GmailAttachmentsExtractor.jar \
       "label:mercadona from: ticket_digital@mail.mercadona.com" \
       --no-modify-gmail --filename '.*\.pdf$' .\mails\
     ```
     mostrando cómo se traduce a la nueva CLI (`download-tickets --query ...
     --filename-regex ... --data-dir ...`).
   - Nota explícita de seguridad: el scope solicitado es solo lectura y el
     programa aborta si detecta un token con permisos mayores.

## Verificación (cuando se implemente)

- La verificación de extremo a extremo (autenticación OAuth real) requiere
  interacción de navegador con la cuenta de Gmail — no automatizable: ejecutar
  el nuevo comando con la misma query de `command.txt` sobre un rango de
  fechas ya conocido, y comparar los PDFs descargados contra los que ya
  existen en `mails/` para confirmar que coinciden (mismos ficheros, mismo
  contenido).
- Tests unitarios posibles sin red: el filtrado por regex de nombre de
  adjunto y el recorrido recursivo de `payload.parts` (con un `MessagePart`
  construido a mano) sí son testeables sin credenciales reales.
- Lógica de descarga incremental (lectura/escritura del fichero de estado,
  cálculo de la cláusula `after:` con solape, y el "ya existe en disco → se
  salta") testeable con ficheros temporales, sin red.
- Validación de scope: testeable simulando distintas respuestas del
  tokeninfo (mockeando el cliente HTTP) — casos: scope exacto
  `gmail.readonly` (continúa), scope vacío/erróneo (aborta), scope con
  `gmail.modify` u otro más amplio (aborta).