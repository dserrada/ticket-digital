# Integrar la descarga de tickets desde Gmail en el proyecto

> Documento de análisis, no implementado. Guardado como referencia para una
> futura implementación.

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

## Boceto de implementación (si se decide seguir con la Opción A)

1. **Nuevo módulo** `ticket-digital-gmail` (junto a `ticket-digital-parser` y
   `ticket-digital-data` en `settings.gradle`), para aislar la dependencia
   pesada de las librerías de Google del resto del proyecto — `Main.java` en
   `ticket-digital-data` pasaría a depender también de este módulo nuevo,
   igual que ya depende de `ticket-digital-parser`. (Alternativa más simple:
   añadir la clase directamente en `ticket-digital-data` si se prefiere no
   crear un módulo nuevo.)
2. **Clase de descarga** (p. ej. `GmailTicketDownloader`): usa
   `GoogleAuthorizationCodeFlow` + `LocalServerReceiver` (flujo "installed
   app" estándar) apuntando al `credentials.json` existente, guarda el token
   en un fichero local (equivalente a la carpeta `tokens` actual); crea el
   cliente `Gmail`; pagina `users().messages().list(userId, q=<query>)`; por
   cada mensaje, `users().messages().get(...)`, recorre `payload.parts`
   recursivamente buscando partes con `filename` que matchee el regex (p. ej.
   `.*\.pdf$`) y `body.attachmentId` no nulo; descarga cada adjunto con
   `users().messages().attachments().get(...)` (base64url) y lo escribe en
   `--data-dir`, evitando volver a descargar lo que ya exista (nombre de
   fichero o `messageId` ya presente).
3. **Nuevo subcomando picocli** en `Main.java`, hermano de
   `write-items-csv`/`write-items-xlsx`, p. ej.:
   `download-tickets --data-dir <dir> --query "label:mercadona from: ticket_digital@mail.mercadona.com" --filename-regex '.*\.pdf$'`
   (query y regex con el valor actual como default, para no tener que
   escribirlos cada vez).
4. **Credenciales**: reutilizar `credentials.json` tal cual (copiarlo a
   `src/main/resources` del nuevo módulo o dejarlo fuera del repo y pasarlo
   por `--credentials-file`, a decidir — es un secreto, no debe comitearse).

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