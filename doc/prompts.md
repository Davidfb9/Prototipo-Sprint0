# Prompts utilizados con el agente de IA

**Autor:** David Ferra · **Fecha:** 2026-10-15

---

Este documento recoge los prompts empleados para desarrollar el Sprint 0, **organizados por bloques de commit**: cada bloque de código corresponde a un commit del repositorio, y entre bloque y bloque se recogen los **problemas reales** que aparecieron durante el desarrollo y el prompt con el que se le pidieron al agente.

Cada prompt indica **qué se le pidió** y **qué se obtuvo**.

**Estructura del documento:**

1. Contexto inicial de la conversación
2. Configuración de git
3. Los ocho bloques de código, cada uno con su prompt de creación y su prompt de subida
4. Los problemas reales, intercalados entre los bloques donde ocurrieron
5. El prompt que crea este documento
6. La subida a GitHub

| Bloque | Commit | Fecha | Qué contiene |
|---|---|---|---|
| 1 | `7a6a5b4` | 13/10 | Estructura inicial del prototipo |
| 2 | `24897b0` | 13/10 | Base de datos: esquema de `bd_beacon` |
| 3 | `047fb80` | 13/10 | Backend PHP: lógica de negocio y API REST |
| 4 | `7dd4a6b` | 14/10 | Tests automáticos de BD, lógica y REST |
| 5 | `3940f0d` | 14/10 | Web: interfaz y lógica fake del navegador |
| 6 | `361f7b5` | 14/10 | Arduino: emisión de los iBeacon |
| 7 | `ba85614` | 15/10 | Android: aplicación de escaneo y envío REST |
| 8 | `f07cb4f` | 15/10 | Documentación del Sprint 0 |

---

## Contexto inicial de la conversación

Antes de escribir ni una línea, se le entregó al agente todo el material de partida, para que trabajara sobre lo que ya existía y no sobre una hipótesis.

### Prompt I.1 — Código de partida y diseños de la tarea anterior

> Te dejo, como contexto inicial de esta conversación, dos cosas:
>
> **1. El código de partida que nos ha dado el profesor, a medio terminar.** Está en la carpeta del proyecto. Es un esqueleto que ya trae Arduino y Android empezados, con un beacon que emite y una app que escanea, pero **no tiene backend, ni base de datos, ni página web**. No lo borres ni lo empieces de cero: léelo entero, entiende qué hace cada fichero y qué decisiones ya están tomadas, y sigue construyendo encima. Si algo del código de partida está mal, dímelo antes de tocarlo.
>
> **2. Los documentos de diseño que creé en la tarea anterior.** Están en la carpeta «Tarea anterior». Son cinco de los siete documentos que va a exigir el revisor. Revísalos y dime cuáles se pueden reutilizar, cuáles hay que renombrar y cuáles faltan.
>
> Con eso como base, explícame qué se me pide en el Sprint 0, qué tengo ya hecho, qué me falta y por dónde conviene empezar. **Todavía no escribas código.**

**Resultado:** se identificaron los cinco componentes obligatorios (Arduino, Android, API REST, lógica de negocio, base de datos y web), se comprobó que el código de partida ya cubría Arduino y Android pero no el backend ni la página, y se confirmó que la carpeta de la tarea anterior contenía cinco de los siete documentos de diseño, con seis problemas que corregir: nombres de fichero incorrectos, falta de la sección de reglas generales, formato de base de datos no válido, ausencia del diseño de Arduino, modelo de datos obsoleto y uso de un escáner simulado.

### Prompt I.2 — Rúbrica y normas del revisor

> Ahora lee la rúbrica del Sprint 0, la rúbrica general de DSIC, el `AGENTS.md` del agente revisor y el `Software_Engineering_Spec.md`. Dime exactamente qué puntos se evalúan, con cuánto pesa cada uno, qué errores graves provocarían perder nota, y qué estructura de carpetas y qué nombres de fichero exige el revisor.

**Resultado:** se detectaron requisitos que no estaban en el enunciado y que condicionan todo lo demás: la **lógica fake en una clase independiente** tanto en Android como en el navegador, la **identidad de firmas** entre cliente y servidor, los tests **independientes e idempotentes**, el **git flow** con `develop`, `master` y `features`, la estructura `doc/` + `src/` + `test/`, los nombres exactos `xxx_design.md` y el formato `TABLE / DESCRIPTION / COLUMNS / PRIMARY KEY / FOREIGN KEYS / CONSTRAINTS` para la base de datos.

---

## Configuración de git

Antes del primer commit había que tener el repositorio, las ramas y el usuario correctos. Estos son los prompts de conexión a git.

### Prompt G.1 — Inicializar el repositorio

> Inicializa el repositorio git en la raíz del proyecto. Crea un `.gitignore` que deje fuera las dependencias, los ficheros compilados, los APKs y cualquier fichero con credenciales — en concreto `config.php`, que lleva la contraseña de la base de datos y **no puede subirse**. No hagas todavía ningún commit.

**Resultado:** se creó el `.gitignore` con `.idea/`, `*.iml`, `*.apk`, `local.properties`, `test/out/`, las carpetas `build/` de Gradle y, sobre todo, `src/business_logic/config.php`.

### Prompt G.2 — Ramas de git flow

> Crea las ramas que exige la rúbrica siguiendo git flow: `master` como rama de producción, `develop` como rama de integración y `feature` para el trabajo en curso. Deja `master` y `develop` apuntando al mismo punto por ahora, y trabaja sobre `feature`.

**Resultado:** se crearon `master`, `develop` y la rama de `feature`, alineadas según la guía de git de la asignatura.

### Prompt G.3 — Autor de los commits

> Configura el usuario de git **sólo para este repositorio**, sin tocar la configuración global de mi equipo: nombre `David Ferra` y correo `davidf.boix@gmail.com`. Los commits tienen que ir firmados con ese autor.

**Resultado:** se aplicó `git config user.name` y `git config user.email` en modo local, de modo que el resto de repositorios del equipo no cambian.

---

## BLOQUE 1 — Estructura inicial del prototipo

**Commit `7a6a5b4` · 13/10/2026 · 3 ficheros, 209 líneas**

### ✅ Criterio de aceptación del entregable

**Qué tiene que ser cierto:** el repositorio tiene `author.md`, `README.md`, siete `doc/*_design.md` y una subcarpeta por componente dentro de `src/`, cada una con su diseño homónimo.

**Cómo se comprueba (reproducible):**

```
git ls-files "doc/*_design.md"
git ls-files "src/*"
C:\xampp\php\php.exe test\test_ui.php
```

**Pasa si:** la primera orden devuelve **siete** `*_design.md`; `src/` contiene `android arduino business_logic communication database frontend_business_logic gui`; y el test termina con código de salida `0`.

---

### Prompt 1.1 — Crear la estructura del proyecto

> Con lo que ya hemos hablado, crea la estructura de carpetas del prototipo. Necesito que los códigos de Arduino, Android, PHP, JavaScript y HTML estén **en carpetas separadas**, respetando exactamente la estructura que exige el agente revisor. Añade el `author.md` con mis datos y un `README.md` mínimo. Dime qué carpeta es para qué y qué fichero va en cada una.

**Resultado:** se crearon `src/arduino`, `src/android`, `src/communication`, `src/business_logic`, `src/frontend_business_logic` y `src/gui`, de modo que cada una tiene su diseño correspondiente en `doc/`, más `author.md`, `README.md` y `.gitignore`.

### Prompt 1.2 — Subir el bloque a git

> Haz el commit de este bloque con un mensaje descriptivo y **súbelo al repositorio**. Que se vea en la rama `feature`.

**Resultado:** commit `7a6a5b4` — *Estructura inicial del prototipo*.

---

### 🔴 Problema real 1 — La estructura no coincidía con lo que exigía el revisor

El esqueleto inicial agrupaba el código por typeos de archivo y no por componente. El agente revisor espera una carpeta por componente y su diseño homónimo en `doc/`.

> **Prompt:** He mirado la estructura que has creado y no coincide con la que exige el `AGENTS.md`. Comprueba qué espera el revisor, compáralo con lo que hay y **corrige la estructura** sin romper nada de lo que ya funcione. Dime exactamente qué has movido y por qué.

**Resultado:** se reorganizaron las carpetas para que cada componente (`communication`, `business_logic`, `frontend_business_logic`, `gui`, `arduino`, `android`) tuviera su contrapartida exacta en `doc/*_design.md`.

---

## BLOQUE 2 — Base de datos: esquema de `bd_beacon`

**Commit `24897b0` · 13/10/2026 · 1 fichero, 49 líneas**

### ✅ Criterio de aceptación del entregable

**Qué tiene que ser cierto:** la tabla `MEDIDAS` existe con sus cinco columnas, y es la base de datos —no la lógica— quien rechaza los sensores desconocidos, los valores fuera de rango y los contadores fuera de rango.

**Cómo se comprueba (reproducible):**

```
C:\xampp\php\php.exe test\test_bd.php
```

**Pasa si:** termina con `Pruebas: 22   Fallos: 0` y código de salida `0`. Es **idempotente**: cada prueba borra lo que crea, así que se puede ejecutar seguidas veces y devuelve siempre lo mismo.

---

### Prompt 2.1 — Crear la base de datos

> Diseña la base de datos del proyecto en el formato **exacto** que exige `Database_Design_Spec.md`. Necesito guardar todas las medidas que llegan de la placa, **sin borrar nada**, para poder consultar siempre la última. Usa MariaDB de XAMPP. Incluye las restricciones que impidan guardar datos inválidos y explica cada decisión que tomes.

**Resultado:** se creó `src/database/crear_bd.sql` con la tabla `MEDIDAS` y sus tres restricciones `CHECK` sobre el sensor, el valor y el contador.

### Prompt 2.2 — Subir el bloque a git

> Commitea este bloque y **súbelo**.

**Resultado:** commit `24897b0` — *Base de datos: esquema de `bd_beacon`*.

---

### 🔴 Problema real 2 — El modelo de la tarea anterior ya no servía

La tarea anterior guardaba un único código entero en una sola columna. Con medidas de verdad eso no sirve: no hay sensor, no hay contador y no hay fecha.

> **Prompt:** El modelo de datos de la tarea anterior guarda un único `codigo` entero. Ahora necesito guardar **medidas de verdad**. Dime cómo hay que cambiar el modelo, por qué el diseño anterior ya no sirve, y qué restricciones hay que añadir para que no se guarden sensores inventados ni valores fuera de rango.

**Resultado:** se cambió el modelo de un campo `codigo` a los campos `sensor`, `valor`, `contador` y `fecha_recepcion`, y se añadió un dominio cerrado de sensores (`CO2`, `TEMPERATURA`) con tres `CHECK`.

---

## BLOQUE 3 — Backend PHP: lógica de negocio y API REST

**Commit `047fb80` · 13/10/2026 · 4 ficheros, 390 líneas**

### ✅ Criterio de aceptación del entregable

**Qué tiene que ser cierto:** la lógica de negocio no contiene **nada** de transporte, y la API distingue `200` (medida guardada o consultada), `400` (JSON mal formado o incompleto) y `405` (método incorrecto).

**Cómo se comprueba (reproducible):**

```
C:\xampp\php\php.exe test\test_logica_negocio.php
C:\xampp\php\php.exe test\test_rest.php
Select-String -Path "src\business_logic\*.php" -Pattern '\$_GET|\$_POST|json_|php://input|http_response_code'
```

**Pasa si:** los dos tests terminan con `Fallos: 0` (23 y 17 pruebas) y la última orden **no devuelve ninguna línea**: la capa de negocio no sabe que existe el servidor.

---

### Prompt 3.1 — Lógica de negocio

> Escribe la lógica de negocio de las medidas en PHP, dentro de `src/business_logic/`. Esta capa **no puede contener nada de HTTP**: sin rutas, sin códigos de estado, sin peticiones, sin JSON. Sólo reglas del dominio y acceso a la base de datos. Que tenga la notación de la asignatura en las cabeceras de cada función y que explique cada decisión.

**Resultado:** se crearon `src/business_logic/medidas.php` con `guardarMedida()`, `consultarUltimaMedida()` y las funciones auxiliares de validación, más `config.php` con la conexión PDO.

### Prompt 3.2 — Servidor REST

> Crea el servidor REST en `src/communication/` con dos rutas: una para que el teléfono registre una medida y otra para que el navegador consulte la última. Estas rutas **sólo deben traducir** entre HTTP y la lógica de negocio: la validación ya vive en el backend. Usa `POST` para crear y `GET` para leer, y devuelve los códigos de estado adecuados.

**Resultado:** se crearon `guardarMedida.php` y `consultarUltimaMedida.php`, con comprobación de método, comprobación de que el cuerpo sea JSON y los códigos `200`, `400` y `405`.

### Prompt 3.3 — Documentación de la API

> Documenta la API REST con ejemplos reales de `curl` para cada operación y para cada respuesta posible, dentro de `doc/communication_design.md`.

**Resultado:** se añadieron ejemplos de uso y una tabla con las seis respuestas posibles.

### Prompt 3.4 — Subir el bloque a git

> Commitea este bloque y **súbelo** al repositorio.

**Resultado:** commit `047fb80` — *Backend PHP: lógica de negocio y API REST*.

---

### 🔴 Problema real 3 — La lógica de negocio se estaba contaminando de HTTP

Al principio, `medidas.php` acabó llamando a `http_response_code()` y leyendo `php://input`. Eso mezcla dos capas que la rúbrica exige separar.

> **Prompt:** Comprueba que la lógica de negocio **no depende en absoluto** del servidor REST. Si encuentras alguna referencia a HTTP —superglobales, `json_encode`, códigos de estado, rutas— dímela y sepárala donde corresponda. Después vuelve a leer el fichero entero y confírmame que ya no queda nada de HTTP.

**Resultado:** se verificó que `medidas.php` no contiene rutas, códigos de estado, superglobales ni `json_encode`, y se documentó en el diseño que ésa es la razón de que las funciones devuelvan un array o un texto de error en lugar de códigos de estado. Las funciones pasaron a devolver `array{ok:boolean, error:string}`.

---

## BLOQUE 4 — Tests automáticos de base de datos, lógica y REST

**Commit `7dd4a6b` · 14/10/2026 · 4 ficheros, 605 líneas**

### ✅ Criterio de aceptación del entregable

**Qué tiene que ser cierto:** existen **cuatro** juegos de pruebas —base de datos, lógica de negocio, API e interfaz— todos **independientes** (ninguno depende de que otro se haya ejecutado antes) e **idempotentes** (al terminar, la base de datos queda exactamente igual que al empezar).

**Cómo se comprueba (reproducible):**

```
test\ejecutar_tests.bat
test\ejecutar_tests.bat
```

**Pasa si:** las dos ejecuciones seguidas devuelven **exactamente la misma salida**, muestran los cuatro bloques y terminan con `TODOS LOS TESTS HAN PASADO` y código de salida `0`. Total: **88 pruebas** (22 + 23 + 17 + 26).

---

### Prompt 4.1 — Batería de pruebas

> Crea tests automáticos para la base de datos, para la lógica de negocio y para el servidor REST. Tienen que ser **independientes** (ninguno depende de que otro se haya ejecutado antes) e **idempotentes** (al terminar, la base de datos tiene que quedar exactamente igual que al empezar). Ponme un lanzador que ejecute los tres y devuelva un código de salida útil para CI.

**Resultado:** se crearon `test/test_bd.php` (22 pruebas), `test/test_logica_negocio.php` (23 pruebas) y `test/test_rest.php` (17 pruebas), más `test/ejecutar_tests.bat`, con **62 pruebas en total**.

### Prompt 4.2 — Verificación de la cadena completa

> Comprueba de principio a fin que una medida recorre toda la cadena: simula el envío del teléfono por REST, verifica que queda guardada en la base de datos y que el navegador la recupera. Muéstrame la salida.

**Resultado:** se ejecutó la cadena completa y se comprobó que la medida pasa del `POST` a MariaDB y de ahí al `GET` del navegador.

### Prompt 4.3 — Subir el bloque a git

> Commitea este bloque y **súbelo**.

**Resultado:** commit `7dd4a6b` — *Tests automáticos de base de datos, logica y REST*.

---

### 🔴 Problema real 4 — Un test fallaba por consultar la tabla equivocada

> **Prompt:** Un test ha fallado. Te paso la salida: comprueba el campo autoincremental y dice que no existe. Mira la consulta que hace, compárala con el esquema real de la tabla y **arréglalo**. Después vuelve a ejecutar la batería completa hasta dejarla en verde y no toques nada más.

**Resultado:** el test consultaba `SHOW TABLE STATUS` en lugar de `SHOW COLUMNS`; se corrigió la comprobación del campo autoincremental y se volvió a ejecutar la batería completa hasta dejarla en verde.

---

## BLOQUE 5 — Web: interfaz y lógica fake del navegador

**Commit `3940f0d` · 14/10/2026 · 2 ficheros, 245 líneas**

### ✅ Criterio de aceptación del entregable

**Qué tiene que ser cierto:** la interfaz **no se comunica por su cuenta** (sin peticiones, sin SQL, sin PHP) y la lógica del navegador expone exactamente la **misma firma** que la lógica del backend.

**Cómo se comprueba (reproducible):**

```
C:\xampp\php\php.exe test\test_ui.php
```

**Pasa si:** `Pruebas: 26   Fallos: 0`. Es una prueba **estática**, así que no necesita Apache, navegador ni base de datos: se puede lanzar en cualquier momento.

---

### Prompt 5.1 — Lógica fake del navegador

> Crea la capa de JavaScript que hará de lógica fake en el navegador, en `src/frontend_business_logic/`. Esta función es la **ÚNICA** que puede hablar con el servidor REST. La interfaz gráfica no debe hacer ninguna petición HTTP. Usa `XMLHttpRequest` y entrega el resultado por callback.

**Resultado:** se creó `src/frontend_business_logic/consultarUltimaMedida.js` con la función `consultarUltimaMedida(cb)` y un callback con la firma `cb(error, resultado)`.

### Prompt 5.2 — Interfaz gráfica

> Crea la página que muestra la última medida. **Sólo pinta**, sin lógica: que llame a la función de la lógica fake y nada más. Que el valor sea grande y centrado, para que se lea durante la demostración, y que se refresque sola.

**Resultado:** se creó `src/gui/Aplicacion.html`, que no contiene ninguna petición ni interpretación de datos.

### Prompt 5.3 — Subir el bloque a git

> Commitea este bloque y **súbelo**.

**Resultado:** commit `3940f0d` — *Web: interfaz y logica fake del navegador*.

---

### 🔴 Problema real 5 — La interfaz hacía las peticiones ella sola

La primera versión de `Aplicacion.html` traía un `XMLHttpRequest` dentro de un `<script>`. Eso rompe la separación de capas: la GUI no debe conocer HTTP.

> **Prompt:** Me he vuelto a leer el HTML y la página **está haciendo ella misma la petición HTTP**. Saca toda la petición de la interfaz y móvela a `src/frontend_business_logic/`, de modo que la página sólo sepa llamar a una función y pintar lo que esa función le devuelva. Después comprueba que la función del navegador y la del servidor **se llaman igual** y trabajan con el mismo dominio, como pide la regla de identidad de firmas, y dime en qué se parecen y en qué difieren.

**Resultado:** se creó `consultarUltimaMedida.js` como única vía de acceso a HTTP, la página quedó sin ninguna petición, y se documentó en `doc/frontend_business_logic_design.md` que ambas se llaman `consultarUltimaMedida()` y trabajan con los mismos tipos, cambiando sólo en la asincronía de la respuesta.

---

## BLOQUE 6 — Arduino: emisión de los iBeacon

**Commit `361f7b5` · 14/10/2026 · 7 ficheros, 1089 líneas**

### ✅ Criterio de aceptación del entregable

**Qué tiene que ser cierto:** el firmware **compila** para la placa y el beacon que emite se llama `GTI-David`.

**Cómo se comprueba (reproducible):**

```
arduino-cli compile --fqbn adafruit:nrf52:feather52840 src\arduino\HolaMundoIBeacon
```

**Pasa si:** la compilación termina sin errores. El nombre y la medida ficticia se verifican con el test de funcionamiento de la rúbrica: se enciende la placa y se comprueba por Bluetooth que aparece `GTI-David`.

---

### Prompt 6.1 — Medida ficticia

> En el código de Arduino necesito una medida ficticia para poder probar toda la cadena sin depender de un sensor real. Ponla en el sitio correcto y que sea un valor fijo y fácil de reconocer.

**Resultado:** se modificó `Medidor.h` para que `medirCO2()` devuelva siempre **123**, y se documentó en `doc/arduino_design.md` que ese es el valor que debe recorrer toda la cadena.

### Prompt 6.2 — Nombre del beacon

> Quiero que el beacon tenga un nombre propio y reconocible, por ejemplo `GTI-David`, en lugar del genérico que tiene ahora. Cámbialo en todo el proyecto, tanto en la placa como en la constante del teléfono que define qué beacon se busca, y comprueba que coinciden.

**Resultado:** se cambió el nombre de emisora en `Publicador.h` y la constante `NOMBRE_EMISORA_BUSCADA` de Android, y se comprobó en `EmisoraBLE.h` que el nombre se añade a la respuesta de escaneo, que es lo que permite al teléfono filtrar por nombre.

### Prompt 6.3 — Emisión de la trama

> Escribe el módulo que emita el iBeacon completo con el UUID del proyecto, el `major` y el `minor` repartiendo la medida y el contador, y que lo haga de forma que el ciclo de vida de la placa quede orquestado desde un único `loop()`. Documenta en el diseño cómo se reparte la trama y con qué identificadores, para que el teléfono sepa decodificarla.

**Resultado:** se crearon `EmisoraBLE.h`, `Publicador.h`, `ServicioEnEmisora.h`, `Medidor.h`, `LED.h` y `PuertoSerie.h`, y se documentó la fórmula `major = (identificadorDeMedicion << 8) + contador` con `minor = valor medido`, la tabla de identificadores (`11` CO2, `12` TEMPERATURA) y un ejemplo numérico desglosado.

### Prompt 6.4 — Subir el bloque a git

> Commitea este bloque y **súbelo**.

**Resultado:** commit `361f7b5` — *Arduino: emision de los iBeacon*.

---

### 🔴 Problema real 6 — El nombre del beacon desaparecía a la tercera vuelta

Éste fue el más enrevesado, y casi rompe la demostración. El beacon funcionaba durante unos segundos y después el teléfono dejaba de encontrarlo, sin ningún error visible.

> **Prompt:** El teléfono encontraba el beacon al principio y **a los pocos segundos dejaba de encontrarlo**, sin que cambiara nada. Descompón el paquete de anuncio byte a byte, dime exactamente qué se envía en el paquete de anuncio y qué en la respuesta de escaneo, y busca por qué el nombre puede dejar de aparecer después de varias vueltas del `loop()`. No me des una hipótesis: traza el código y demuéstralo.

**Resultado:** la causa era que **la respuesta de escaneo se iba acumulando vuelta tras vuelta**: el paquete de anuncio lo limpia `BLEBeacon::start()` por sí solo, pero nadie limpiaba el `ScanResponse`, que se llenaba y a la tercera vuelta dejaba de contener el nombre. Como el teléfono filtra leyendo el nombre del paquete (AD type `0x09`), el beacon pasaba a ser invisible. Se añadió `ScanResponse.clearData()` antes de `addName()` en `EmisoraBLE.h`, de modo que en cada vuelta la respuesta se reconstruye desde cero.

---

## BLOQUE 7 — Android: aplicación de escaneo y envío REST

**Commit `ba85614` · 15/10/2026 · 47 ficheros, 2855 líneas**

### ✅ Criterio de aceptación del entregable

**Qué tiene que ser cierto:** la aplicación **compila**, escanea, filtra por nombre y envía las medidas al servidor sin que la lógica conozca el hardware.

**Cómo se comprueba (reproducible):**

```
cd src\android
.\gradlew.bat assembleDebug
```

**Pasa si:** `BUILD SUCCESSFUL`. El comportamiento —escaneo, filtrado y envío— se verifica con el test de funcionamiento de la rúbrica sobre el teléfono real.

---

### Prompt 7.1 — Separación en clases

> La rúbrica exige que la lógica fake del teléfono esté en una **clase independiente**. Hoy el `MainActivity` llama directamente al cliente REST, lo cual está mal. Sepáralo: una interfaz para el buscador de beacons, otra para el cliente REST, una implementación real y una simulada de cada una, y una clase de lógica que coordine todo sin conocer ni Bluetooth ni HTTP.

**Resultado:** se crearon `EscannerBeacon`, `EscannerBeaconBTLE`, `EscannerBeaconFake`, `ClienteREST`, `ClienteRESTHttp`, `ClienteRESTFake` y `LogicaTelefono`, y se dejó `MainActivity` sin lógica de negocio.

### Prompt 7.2 — Componentes simulados

> Quiero poder probar toda la lógica del teléfono **sin tener la placa encendida ni el servidor montado**. Diseña el código de forma que baste con cambiar dos líneas para pasar de los componentes reales a los simulados, sin tocar ninguna otra clase.

**Resultado:** se añadió la constante `USAR_COMPONENTES_FAKE` en `MainActivity.java`, que selecciona los fakes en `crearEscanner()` y `crearClienteREST()`.

### Prompt 7.3 — Filtro por nombre

> El escáner devuelve **todos** los beacons BTLE del entorno, y en el aula hay varios con el mismo UUID, porque todos llevan la misma plantilla. Haz que la app sólo acepte el nuestro. Ojo con `bluetoothDevice.getName()`: comprueba si funciona en este móvil antes de usarlo, y si no, lee el nombre del paquete de escaneo.

**Resultado:** el nombre se leyó del paquete de escaneo (AD type `0x09`) en vez de `bluetoothDevice.getName()`, que en MIUI devuelve `null` y exigía además `BLUETOOTH_CONNECT`.

### Prompt 7.4 — Subir el bloque a git

> Commitea este bloque y **súbelo** al repositorio.

**Resultado:** commit `ba85614` — *Android: aplicacion de escaneo y envio REST*.

---

### 🔴 Problema real 7 — Tres fallos de arranque en Android

Los tres se manifestaban igual: la app arrancaba y no escaneaba, o estallaba al pulsar el botón, sin mensaje alguno en pantalla.

> **Prompt:** La app no funciona al arrancar. Te paso el logcat: por un lado hay una `NullPointerException` en el escaneo, por otro la app **no llega nunca a pedir permisos** en el primer arranque, y hay veces que `getBluetoothLeScanner()` devuelve `null` y cualquier pulsación del botón cae a tierra. Arréglalo entero, y deja un log claro en cada tramo para que se vea por dónde pasa el flujo.

**Resultado:** se añadió el *null-check* de `getScanRecord()`; se retiró el `return` que bloqueaba `requestPermissions()` (en un primer arranque salía por ahí y la petición nunca se lanzaba); y el escáner pasó a obtenerse bajo demanda con `obtenerEscanner()`, porque `getBluetoothLeScanner()` devuelve `null` mientras el adaptador está apagado y `enable()` es asíncrono.

### 🔴 Problema real 8 — La temperatura nunca llegaba a la base de datos

Este fue el que hizo falta **medir en la base de datos** para verlo. No se notaba en el código: sólo mirando los datos reales.

> **Prompt:** He mirado la base de datos y algo va mal: hay **22 medidas de CO₂ y sólo 1 de temperatura**. La temperatura casi nunca llega. Investiga por qué, mira el orden en que se publican las medidas en el `loop()` del Arduino y el límite de envíos que aplicas en el teléfono, y arréglalo. Después **vuelve a medirlo de verdad** y demuéstrame con datos nuevos que ahora llegan las dos.

**Resultado:** el límite de envío era **global**: una sola marca de tiempo compartida entre todos los sensores. Como en el `loop()` el CO₂ se publica **antes** que la temperatura y el periodo del bucle (~7,6 s) ya supera los 5 segundos del cupo, cuando llegaba la temperatura el cupo ya estaba gastado. Se cambió a un límite **por sensor**, con un `Map<String, Long>` y un contador por sensor en `LogicaTelefono`, y el reparto se verificó midiendo de nuevo en la base de datos.

---

## BLOQUE 8 — Documentación del Sprint 0

**Commit `f07cb4f` · 15/10/2026 · 8 ficheros, 1335 líneas**

### ✅ Criterio de aceptación del entregable

**Qué tiene que ser cierto:** los siete documentos tienen sus tres secciones (`N.1` diseño del componente, `N.2` aclaraciones, `N.3` reglas generales) y sus cuatro reglas obligatorias, y las firmas de los documentos **coinciden con las del código**.

**Cómo se comprueba (reproducible):**

```
C:\xampp\php\php.exe test\test_ui.php
```

**Pasa si:** el test devuelve código de salida `0` (identidad de firmas y separación de la interfaz) y la revisión contra el `AGENTS.md` del agente revisor no señala ningún fallo.

---

### Prompt 8.1 — Los siete documentos de diseño

> Escribe los siete documentos de diseño que exige el agente revisor, aprovechando los de la tarea anterior. Renómbralos al formato `xxx_design.md`, añade la sección de reglas generales que falta, corrige el formato de la base de datos al que exige la norma y crea el que falta de Arduino. Cada uno con su diagrama y con las tres secciones obligatorias.

**Resultado:** se generaron los siete ficheros de `doc/` — `database_design`, `business_logic_design`, `communication_design`, `frontend_business_logic_design`, `gui_design`, `arduino_design` y `android_design` — incluido el de Arduino, que no existía.

### Prompt 8.2 — Ingeniería inversa de Arduino y Android

> Documenta el diseño de Arduino y de Android **por ingeniería inversa**, a partir del código real que ya tenemos. Deja claro en el propio documento que se ha hecho ingeniería inversa, e indica qué se ha cambiado respecto al código de partida.

**Resultado:** se documentaron los cambios aplicados (medida ficticia `123`, nombre de emisora `GTI-David`, paquetes Java, rutas relativas de PHP y JavaScript) y la estructura de clases del teléfono, dejando constancia explícita del método en `arduino_design.md`.

### Prompt 8.3 — README

> Escribe un README con una descripción del proyecto, cómo desplegarlo paso a paso y cómo ejecutar los tests. Que alguien que llegue nuevo pueda levantarlo siguiendo **sólo ese fichero**.

**Resultado:** se creó `README.md` con la descripción, los seis pasos de despliegue (XAMPP, base de datos, despliegue, verificación del REST, compilación de Android y subida a la placa), la tabla de tests y la estructura del proyecto.

### Prompt 8.4 — Subir el bloque a git

> Commitea este bloque y **súbelo**.

**Resultado:** commit `f07cb4f` — *Documentacion del Sprint 0*.

---

### 🔴 Problema real 9 — Los diseños de la tarea anterior no servían tal cual

> **Prompt:** He comparado los cinco documentos de la tarea anterior con lo que exige el revisor y **no valen tal cual**: tienen nombres de fichero distintos, les falta la sección de reglas generales, el de base de datos no sigue el formato `TABLE / DESCRIPTION / COLUMNS / ...`, no existe el de Arduino, y el modelo de datos que describen es el antiguo. Dime uno por uno qué hay que cambiar en cada documento y hazlo.

**Resultado:** se renombraron al formato `xxx_design.md`, se añadió la sección de reglas generales a los cinco, se rehizo `database_design.md` con el formato exacto, se creó `arduino_design.md` y se actualizó el modelo de datos en todos los que lo describían.

---

## El documento de prompts

### Prompt D.1 — Crear este documento

> Crea un documento en `doc/prompts.md` que recoja **todos los prompts que me has hecho durante el proyecto**. Quiero que esté organizado **por bloques de commit**: un bloque por cada commit que hemos hecho, con el hash, la fecha y lo que contiene, y **entre medio de los bloques, los problemas reales** que hemos tenido** — con el prompt con el que te los planteé y cómo se resolvieron. Empieza con el contexto inicial que te di, incluye los prompts de git, y al final pon los prompts de creación de este documento y de subida a GitHub. No inventes prompts que no te haya hecho.

**Resultado:** se creó `doc/prompts.md` con este contenido.

---

## Conexión con GitHub y subida

### Prompt H.1 — Autenticarse en GitHub

> Conéctate a GitHub desde la línea de comandos. Primero comprueba con `gh auth status` con qué cuenta estoy, porque en este equipo puede haber sesionado otro usuario. Si no es la mía, cierra la sesión y vuelve a iniciarla con la mía. Después dime con qué cuenta y con qué permisos he quedado.

**Resultado:** se verificó la sesión con `gh auth status`, se detectó la cuenta activa y se relanzó el inicio de sesión, quedando autenticado como **Davidfb9** con los ámbitos `gist`, `read:org`, `repo` y `workflow`.

### Prompt H.2 — Crear el repositorio y subirlo

> Conéctate a GitHub y créame el repositorio del proyecto. Quiero que se llame `Prototipo-Sprint0`, que sea **público**, y que tenga `master` y `develop` como ramas. Añádele el remoto `origin` y **súbelo tú**: las dos ramas y todas las etiquetas que haya. Después comprueba desde la API que el repositorio está creado, que es público y que efectivamente se ven las ramas.

**Resultado:** se creó `https://github.com/Davidfb9/Prototipo-Sprint0`, se configuró `origin`, se subieron `master` y `develop`, y se verificó el estado remoto.

### Prompt H.3 — Subir cada parte terminada

> **Cada vez que terminemos una parte**, has de hacer dos cosas: primero el commit con un mensaje que diga qué se ha hecho, y después **súbelo al repositorio** con `git push`. No me esperes a que lo pida: en cuanto quede una parte cerrada y verificada, commitea y sube. Si hay cambios sin commitear al empezar otra cosa, dímelo antes de tocar nada.

**Resultado:** este prompt se aplicó tras cada uno de los ocho bloques; cada bloque de este documento termina con su prompt de subida correspondiente.

### Prompt H.4 — Estado de la subida

> Comprueba que no queda **nada sin subir**: `git status`, `git log` de `master` y de `develop`, y el remoto. Dime si local y GitHub están de acuerdo y si hay algo pendiente.

**Resultado:** `master` y `develop` sincronizados con `origin`, working tree limpio.

---

## Criterios de aceptación de cada entregable

Cada bloque tiene arriba, en su propia sección, un criterio de aceptación **reproducible**: qué tiene que ser cierto, con qué orden se comprueba y qué resultado lo da por bueno. Esta tabla los resume:

| Entregable | Cómo se comprueba | Pasa si |
|---|---|---|
| **1. Estructura inicial** | `git ls-files "doc/*_design.md"` · `php test\test_ui.php` | 7 diseños `xxx_design.md`, una carpeta por componente en `src/`, código de salida `0` |
| **2. Base de datos** | `php test\test_bd.php` | `Pruebas: 22  Fallos: 0`; la BD rechaza por sí sola datos inválidos |
| **3. Backend PHP** | `php test\test_logica_negocio.php` · `php test\test_rest.php` · `Select-String -Path "src\business_logic\*.php" -Pattern '\$_GET\|json_\|php://input'` | 23 + 17 pruebas en verde y **cero** líneas de coincidencia |
| **4. Tests automáticos** | `test\ejecutar_tests.bat` ejecutado **dos veces seguidas** | Misma salida en ambas, `TODOS LOS TESTS HAN PASADO`, **88 pruebas** |
| **5. Web** | `php test\test_ui.php` | `Pruebas: 26  Fallos: 0`; la interfaz no se comunica sola |
| **6. Arduino** | `arduino-cli compile --fqbn adafruit:nrf52:feather52840 src\arduino\HolaMundoIBeacon` | Compila sin errores; el beacon se llama `GTI-David` |
| **7. Android** | `.\gradlew.bat assembleDebug` | `BUILD SUCCESSFUL` |
| **8. Documentación** | `php test\test_ui.php` + revisión contra el `AGENTS.md` | Código de salida `0` y ningún fallo señalado |

**Características comunes:** todos los criterios son **reproducibles** — cualquiera puede ejecutar la misma orden y obtener el mismo resultado — y los cuatro primeros se apoyan en **tests automáticos**, tal y como exige la rúbrica. Ninguna prueba depende del orden en que se ejecute, ni altera los datos de la anterior.

---

## Resumen del uso del agente

| Sección | Prompts | Qué se obtuvo |
|---|---|---|
| Contexto inicial | 2 | El encargo, la rúbrica y las normas del revisor |
| Configuración de git | 3 | Repositorio, ramas de git flow y autor |
| Bloque 1 — Estructura | 2 | Carpetas por componente |
| *Problema real 1* | 1 | Estructura corregida al revisor |
| Bloque 2 — Base de datos | 2 | `src/database/crear_bd.sql` |
| *Problema real 2* | 1 | Modelo de datos nuevo |
| Bloque 3 — Backend PHP | 4 | Lógica de negocio, API REST y su documentación |
| *Problema real 3* | 1 | Capas separadas sin HTTP en el dominio |
| Bloque 4 — Tests | 3 | 88 pruebas automáticas en 4 juegos (BD, lógica, REST e interfaz) |
| *Problema real 4* | 1 | Test corregido, batería en verde |
| Bloque 5 — Web | 3 | Lógica fake del navegador y página |
| *Problema real 5* | 1 | HTTP fuera de la interfaz, firmas idénticas |
| Bloque 6 — Arduino | 4 | Emisión de iBeacon y medida ficticia |
| *Problema real 6* | 1 | `ScanResponse` limpiado, el nombre deja de perderse |
| Bloque 7 — Android | 4 | Separación en clases, fakes y filtro por nombre |
| *Problemas reales 7 y 8* | 2 | Arranque arreglado y límite de envío **por sensor** |
| Bloque 8 — Documentación | 4 | Los siete diseños y el README |
| *Problema real 9* | 1 | Diseños de la tarea anterior recolocados |
| Documento de prompts | 1 | Este fichero |
| GitHub | 4 | Autenticación, repositorio y subida recurrente |
| **Total** | **46** | |
