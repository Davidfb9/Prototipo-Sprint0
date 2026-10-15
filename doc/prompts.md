# Prompts utilizados con el agente de IA

**Autor:** David Ferra · **Fecha:** 2026-10-15

---

Este documento recoge los prompts empleados para desarrollar el Sprint 0, ordenados por fases. Se ha reescrito ligeramente cada prompt para que quede claro qué se le pidió al agente en cada paso, y se han retirado los prompts relacionados con la instalación de programas, que no forman parte del trabajo de programación.

Cada prompt indica **qué se le pidió** y **qué se obtuvo**.

---

## FASE 1 — Análisis de los materiales de partida

### Prompt 1.1 — Visión general

> Tengo que hacer el Sprint 0 de la asignatura. El objetivo es que una medida ficticia de una placa Arduino llegue hasta una página web pasando por un teléfono Android, una API REST y una base de datos. Antes de escribir nada, lee todos los materiales que tengo en la carpeta del proyecto y explícame qué se me pide exactamente, qué me sobra y qué me falta.

**Resultado:** se identificaron los cinco componentes obligatorios (Arduino, Android, REST, lógica de negocio, base de datos y web) y se comprobó que el código de partida ya cubría Arduino y Android, pero no el backend ni la página.

### Prompt 1.2 — Rúbrica

> Lee la rúbrica del Sprint 0 y la rúbrica general de DSIC. Dime exactamente qué puntos se evalúan, con cuánto pesa cada uno, y qué errores graves provocarían perder nota.

**Resultado:** se detectaron requisitos que no estaban en el enunciado: la **lógica fake en una clase independiente** tanto en Android como en el navegador, la **identidad de firmas** entre cliente y servidor, los tests **independientes e idempotentes** y el uso de **git flow** con `develop`, `master` y `features`.

### Prompt 1.3 — Normas del agente revisor

> Lee el `AGENTS.md` del agente revisor y el `Software_Engineering_Spec.md`. Resume qué estructura de carpetas exige, cómo se llaman los ficheros de diseño y en qué formato debe estar la documentación de la base de datos.

**Resultado:** se obtuvieron las reglas que condicionan todo lo demás: la estructura `doc/` + `src/` + `tests/`, los nombres exactos `xxx_design.md`, las tres secciones obligatorias de cada diseño y el formato `TABLE / DESCRIPTION / COLUMNS / PRIMARY KEY / FOREIGN KEYS / CONSTRAINTS`.

### Prompt 1.4 — Tarea anterior

> He añadido una carpeta que se llama "Tarea anterior". Revísala y dime si es necesaria para esta tarea y qué se puede reutilizar de ella.

**Resultado:** se confirmó que contenía cinco de los siete documentos de diseño que faltaban, se actualizó el `author.md` con el nombre del autor y se detectaron seis problemas a corregir: nombres de fichero incorrectos, falta de la sección de reglas generales, formato de base de datos no válido, ausencia del diseño de Arduino, modelo de datos obsoleto y uso de un escáner simulado.

### Prompt 1.5 — Documentación de la asignatura

> Revisa los PDF de la carpeta de apuntes y dime cuáles afectan a este proyecto y cuáles no. Presta especial atención a la arquitectura en capas y a la guía de git.

**Resultado:** se determinó que `backends.pdf` y `frontends.pdf` eran teoría general de Flask, Django y Vue que no aplicaban a este proyecto, y se extrajo el esquema de responsabilidades por capas y las reglas de git flow.

---

## FASE 2 — Arquitectura y diseño del modelo de datos

### Prompt 2.1 — Estructura de carpetas

> Quiero que en la carpeta del prototipo me crees carpetas separadas para los códigos de Arduino, Android, PHP, JavaScript y HTML, respetando la estructura que exige el agente revisor. Dime qué carpeta es para qué.

**Resultado:** se creó la estructura `src/arduino`, `src/android`, `src/communication`, `src/business_logic`, `src/frontend_business_logic` y `src/gui`, de modo que cada una tiene su diseño correspondiente en `doc/`.

### Prompt 2.2 — Diseño de la base de datos

> Diseña la base de datos del proyecto en el formato exacto que exige `Database_Design_Spec.md`. Necesito guardar todas las medidas que llegan de la placa, sin borrar nada, para poder consultar siempre la última. Usa MariaDB de XAMPP. Incluye las restricciones que impidan guardar datos inválidos y explica cada decisión.

**Resultado:** se creó `bd/crear_bd.sql` con la tabla `MEDIDAS` y sus tres restricciones `CHECK` sobre el sensor, el valor y el contador, además del `doc/database_design.md` con el formato exigido.

### Prompt 2.3 — Modelo de datos

> La tarea anterior guardaba un único código entero. Ahora necesito guardar medidas de verdad. Dime cómo hay que cambiar el modelo y por qué el diseño anterior ya no sirve.

**Resultado:** se cambió el modelo de un campo `codigo` a los campos `sensor`, `valor`, `contador` y `fecha_recepcion`, y se añadió un dominio cerrado de sensores (`CO2`, `TEMPERATURA`).

---

## FASE 3 — Arduino

### Prompt 3.1 — Medida ficticia

> En el código de Arduino necesito una medida ficticia para poder probar toda la cadena sin depender de un sensor real. Ponla en el sitio correcto y que sea un valor fijo y fácil de reconocer.

**Resultado:** se modificó `Medidor.h` para que `medirCO2()` devuelva siempre **123**, y se documentó en `doc/arduino_design.md` que ese es el valor que debe recorrer toda la cadena.

### Prompt 3.2 — Nombre del beacon

> Quiero que el beacon tenga un nombre propio y reconocible, por ejemplo `GTI-David`, en lugar del genérico que tiene ahora. Cámbialo en todo el proyecto, tanto en la placa como en la constante del teléfono que define qué beacon se busca, y comprueba que coinciden.

**Resultado:** se cambió el nombre de emisora en `Publicador.h` y la constante `NOMBRE_EMISORA_BUSCADA` de Android, y se comprobó en `EmisoraBLE.h` que el nombre se añade a la respuesta de escaneo, que es lo que permite al teléfono filtrar por nombre.

---

## FASE 4 — Backend: lógica de negocio y base de datos

### Prompt 4.1 — Lógica de negocio

> Escribe la lógica de negocio de las medidas en PHP, dentro de `src/business_logic/`. Esta capa **no puede contener nada de HTTP**: sin rutas, sin códigos de estado, sin peticiones, sin JSON. Sólo reglas del dominio y acceso a la base de datos. Que tenga la notación de la asignatura en las cabeceras de cada función y que explique cada decisión.

**Resultado:** se creó `src/business_logic/medidas.php` con `guardarMedida()`, `consultarUltimaMedida()` y las funciones auxiliares de validación, más `config.php` con la conexión PDO.

### Prompt 4.2 — Separación de capas

> Comprueba que la lógica de negocio no depende del servidor REST. Si encuentras alguna referencia a HTTP, dímela y sepárala donde corresponda.

**Resultado:** se verificó que el fichero no contiene rutas, códigos de estado, superglobales ni `json_encode`, y se documentó explícitamente en el diseño que esa es la razón de que las funciones devuelvan un array o un texto de error en lugar de códigos.

---

## FASE 5 — API REST

### Prompt 5.1 — Servidor REST

> Crea el servidor REST en `src/communication/` con dos rutas: una para que el teléfono registre una medida y otra para que el navegador consulte la última. Estas rutas **sólo deben traducir** entre HTTP y la lógica de negocio: la validación ya vive en el backend. Usa `POST` para crear y `GET` para leer, y devuelve los códigos de estado adecuados.

**Resultado:** se crearon `guardarMedida.php` y `consultarUltimaMedida.php`, con comprobación de método, comprobación de que el cuerpo sea JSON y los códigos `200`, `400` y `405`.

### Prompt 5.2 — Documentación de la API

> Documenta la API REST con ejemplos reales de `curl` para cada operación y para cada respuesta posible.

**Resultado:** se añadieron ejemplos de uso y una tabla con las seis respuestas posibles en `doc/communication_design.md`.

---

## FASE 6 — Página web y lógica del navegador

### Prompt 6.1 — Lógica fake del navegador

> Crea la capa de JavaScript que hará de lógica fake en el navegador, en `src/frontend_business_logic/`. Esta función es la ÚNICA que puede hablar con el servidor REST. La interfaz gráfica no debe hacer ninguna petición HTTP. Usa `XMLHttpRequest` y entrega el resultado por callback.

**Resultado:** se creó `consultarUltimaMedida.js` con la función `consultarUltimaMedida(cb)` y un callback con la firma `cb(error, resultado)`.

### Prompt 6.2 — Interfaz gráfica

> Crea la página que muestra la última medida. **Sólo pinta**, sin lógica: que llame a la función de la lógica fake y nada más. Que el valor sea grande y centrado, para que se lea durante la demostración, y que se refresque sola.

**Resultado:** se creó `src/gui/Aplicacion.html`, que no contiene ninguna petición ni interpretación de datos y se actualiza cada tres segundos.

### Prompt 6.3 — Identidad de firmas

> Comprueba que la función del navegador y la del servidor se llaman igual y trabajan con el mismo dominio, como pide la regla de identidad de firmas. Dime en qué se parecen y en qué difieren.

**Resultado:** se documentó en `doc/frontend_business_logic_design.md` que ambas se llaman `consultarUltimaMedida()` y trabajan con los mismos tipos, y que lo único que cambia es la asincronía de la respuesta.

---

## FASE 7 — Cliente Android

### Prompt 7.1 — Separación en clases

> La rúbrica exige que la lógica fake del teléfono esté en una clase independiente. Hoy el `MainActivity` llama directamente al cliente REST, lo cual está mal. Sepáralo: una interfaz para el buscador de beacons, otra para el cliente REST, una implementación real y una simulada de cada una, y una clase de lógica que coordine todo sin conocer ni Bluetooth ni HTTP.

**Resultado:** se crearon `EscannerBeacon`, `EscannerBeaconBTLE`, `EscannerBeaconFake`, `ClienteREST`, `ClienteRESTHttp`, `ClienteRESTFake` y `LogicaTelefono`, y se dejó `MainActivity` sin lógica de negocio.

### Prompt 7.2 — La lógica no debe conocer el hardware

> Quiero poder probar toda la lógica del teléfono sin tener la placa encendida ni el servidor montado. Diseña el código de forma que baste con cambiar dos líneas para pasar de los componentes reales a los simulados, sin tocar ninguna otra clase.

**Resultado:** se añadió la constante `USAR_COMPONENTES_FAKE` en `MainActivity.java`, que selecciona los fakes en `crearEscanner()` y `crearClienteREST()`.

### Prompt 7.3 — Decodificación de la trama

> El Arduino codifica la medida repartida entre el `major` y el `minor` del beacon. Escribe en el diseño exactamente cómo se reparte y con qué identificadores, para que quede documentada la trama que el teléfono tiene que decodificar.

**Resultado:** se documentó la fórmula `major = (identificadorDeMedicion << 8) + contador` y `minor = valor medido`, con la tabla de identificadores (`11` CO2, `12` TEMPERATURA) y un ejemplo numérico desglosado.

---

## FASE 8 — Pruebas

### Prompt 8.1 — Tests automáticos

> Crea tests automáticos para la base de datos, para la lógica de negocio y para el servidor REST. Tienen que ser **independientes** (ninguno depende de que otro se haya ejecutado antes) e **idempotentes** (al terminar, la base de datos tiene que quedar igual que al empezar). Ponme un lanzador que ejecute los tres y devuelva un código de salida útil.

**Resultado:** se crearon `test_bd.php` (22 pruebas), `test_logica_negocio.php` (23 pruebas) y `test_rest.php` (17 pruebas), más `ejecutar_tests.bat`, con **62 pruebas en total, todas en verde**.

### Prompt 8.2 — Verificación de la cadena completa

> Comprueba de principio a fin que una medida recorre toda la cadena: simula el envío del teléfono por REST, verifica que queda guardada en la base de datos y que el navegador la recupera. Muéstrame la salida.

**Resultado:** se ejecutó la cadena completa y se comprobó que la medida pasa del `POST` a MariaDB y de ahí al `GET` del navegador, viendo además el historial completo en la base de datos.

### Prompt 8.3 — Corrección de un test

> Un test ha fallado porque consulta una columna que no existe en esa consulta. Arréglalo y vuelve a ejecutar la batería completa.

**Resultado:** se corrigió la comprobación del campo autoincremental, que consultaba `SHOW TABLE STATUS` en lugar de `SHOW COLUMNS`, y se volvió a ejecutar la batería completa hasta dejarla en verde.

---

## FASE 9 — Documentación y repositorio

### Prompt 9.1 — Diseños de los componentes

> Escribe los siete documentos de diseño que exige el agente revisor, aprovechando la tarea anterior. Renómbralos al formato `xxx_design.md`, añade la sección de reglas generales que falta, corrige el formato de la base de datos y crea el que falta de Arduino. Cada uno con su diagrama.

**Resultado:** se generaron los siete ficheros de `doc/`, incluido `arduino_design.md`, que no existía, y el `database_design.md` con el formato exacto exigido por la norma.

### Prompt 9.2 — Ingeniería inversa de Arduino y Android

> Documenta el diseño de Arduino y de Android por ingeniería inversa, a partir del código real. Indica qué se ha cambiado respecto al código original.

**Resultado:** se documentaron los cambios aplicados (medida ficticia `123`, nombre de emisora `GTI-David`, paquetes Java, rutas relativas de PHP y JavaScript) y la estructura de clases del teléfono.

### Prompt 9.3 — README

> Escribe un README con una descripción del proyecto, cómo desplegarlo paso a paso y cómo ejecutar los tests. Que alguien que llegue nuevo pueda levantarlo siguiendo sólo ese fichero.

**Resultado:** se creó `README.md` con la descripción, los seis pasos de despliegue, la tabla de tests y la estructura del proyecto.

### Prompt 9.4 — Repositorio

> Inicializa el repositorio con git y créame las ramas que exige la rúbrica: `master`, `develop` y una de `feature`. Configura mi usuario y haz los commits con mensajes descriptivos.

**Resultado:** se inicializó el repositorio, se crearon las tres ramas alineadas según git flow y se hicieron los commits con mensajes descriptivos.

### Prompt 9.5 — Limpieza

> Revisa la estructura final y elimina los ficheros de ejemplo del esqueleto que ya no forman parte del proyecto, para que el revisor vea una estructura limpia.

**Resultado:** se retiraron los ficheros de demostración de la plantilla (`diHola`, `hacerLogin`, carpeta `logicaFake`) y se volvió a ejecutar la batería de pruebas para comprobar que nada se había roto.

---

## FASE 10 — Autorrevisión

### Prompt 10.1 — Estado final

> Dime qué he terminado y qué me falta por terminar, separando lo que sólo puedo hacer yo de lo que queda por programar.

**Resultado:** se generó `PENDIENTES.md` con la lista de tareas terminadas, las que quedan pendientes y las tres cosas de material físico que se necesitan para la demostración.

---

## Resumen del uso del agente

| Fase | Prompts | Qué se obtuvo |
|---|---|---|
| 1. Análisis | 5 | Entendimiento del encargo y de las normas |
| 2. Arquitectura | 3 | Estructura de carpetas y modelo de datos |
| 3. Arduino | 2 | Medida ficticia y nombre del beacon |
| 4. Lógica de negocio | 2 | `medidas.php` y `config.php` |
| 5. API REST | 2 | Las dos rutas y su documentación |
| 6. Web | 3 | Lógica fake del navegador y página |
| 7. Android | 3 | Separación en clases y componentes fakes |
| 8. Pruebas | 3 | 62 pruebas automáticas |
| 9. Documentación | 5 | Diseños, README y repositorio |
| 10. Autorrevisión | 1 | Lista de pendientes |
| **Total** | **29** | |
