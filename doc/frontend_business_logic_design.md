# 4. Diseño de la lógica fake del navegador

**Componente:** `src/frontend_business_logic/`
**Autor:** David Ferra · **Fecha:** 2026-10-15

---

## 4.1. Diseño del componente

### Qué se quiere conseguir

Crear en **JavaScript** una capa que permita a la interfaz del navegador consultar la última medida almacenada.

Esta capa será la encargada de comunicarse con el servidor REST. **La interfaz gráfica no tendrá que conocer los detalles de la petición HTTP.**

### Diseño

La función principal será:

```
cb:Funcion --> consultarUltimaMedida() --> void
```

La función realizará un `GET` a:

```
../communication/consultarUltimaMedida.php
```

y entregará el resultado mediante `callback`.

El navegador podrá recibir **dos situaciones principales**:

#### Hay una medida

```json
{
  "sensor": "CO2",
  "valor": 123,
  "contador": 5,
  "fecha": "2026-10-15 19:55:22"
}
```

#### No hay medidas

```json
{
  "error": "no hay medidas guardadas"
}
```

### Diagrama de la comunicación

```
   +------------------------------------+
   |            PANTALLA                |
   |        gui/Aplicacion.html         |
   |                                    |
   |   consulta Ultima Medida           |
   |         cb(null, medida)           |
   |         cb(error, null)            |
   +------------------+-----------------+
                      |
                      |  consultarUltimaMedida(cb)
                      v
   +------------------------------------+
   |     LÓGICA FAKE DEL NAVEGADOR      |
   |   frontend_business_logic/         |
   |   consultarUltimaMedida.js         |
   +------------------+-----------------+
                      |
                      |  GET
                      |  ../communication/consultarUltimaMedida.php
                      v
   +------------------------------------+
   |      SERVIDOR REST (Apache)        |
   +------------------------------------+
```

---

## 4.2. Aclaraciones de diseño

### ¿Por qué se llama "lógica fake"?

Porque **sustituye a la lógica del servidor en el navegador**. El navegador no tiene base de datos ni reglas de negocio: sólo tiene esta capa, que hace de puente.

Esto permite dos cosas:

1. **Probar la pantalla sin servidor.** Basta con sustituir `consultarUltimaMedida.js` por una versión que responda siempre lo mismo, y la página se pinta igual.
2. **Identidad de firmas.** La función `consultarUltimaMedida()` del navegador es la misma que la del servidor:

   ```
   cliente:   cb:Funcion --> consultarUltimaMedida() --> void
   servidor:  --> consultarUltimaMedida() --> (sensor, valor, contador, fecha) | error
   ```

   Se llaman igual y trabajan con el mismo dominio. Lo único que cambia es que en el cliente la respuesta llega de forma asíncrona.

### ¿Por qué se usa `XMLHttpRequest` y no `fetch`?

Por consistencia con el código de la asignatura y porque funciona en todos los navegadores sin transpilación. Es la opción más simple para este prototipo.

### ¿Cómo se distingue el éxito del error?

**Por el tipo del primer argumento del callback**, no por un código:

* `cb(null, medida)` → hay medida
* `cb(mensajeDeError, null)` → no hay medida o ha fallado la conexión

De esta forma la pantalla no tiene que interpretar códigos de estado.

### ¿Qué hace la interfaz gráfica?

**Sólo pinta.** La función `refrescar()` de `Aplicacion.html` llama a `consultarUltimaMedida()` y escribe el resultado en el DOM. No hace peticiones, no parsea JSON, no decide nada.

`refrescar()` **no es una función de la lógica de negocio del navegador**: pertenece a la interfaz gráfica y sólo existe en `src/gui/Aplicacion.html`. Por eso **no tiene contrapartida en el backend**, y no rompe la regla de identidad de firmas. La única función que esta capa expone es `consultarUltimaMedida()`, que sí tiene su gemela exacta en `src/business_logic/medidas.php`.

### Actualización automática

La página se refresca sola cada 3 segundos, así que basta con tenerla abierta para ver aparecer las medidas que llegan del teléfono.

---

## 4.3. Reglas generales

* **Lenguaje de programación:** **JavaScript** (ES5, sin dependencias externas).
* **Cabeceras de fichero:** el fichero `consultarUltimaMedida.js` lleva cabecera con **nombre, descripción, copyright, fecha, autor y aportación**.
* **Cabeceras de función:** cada función lleva su diseño lógico dentro de un bloque delimitado por líneas `--------------------`, por ejemplo `cb:Funcion --> consultarUltimaMedida() --> void`.
* **Legibilidad:** el callback tiene una firma clara (`cb(error, resultado)`) y los pasos van numerados en comentarios. Un solo fichero por operación.
* **Pruebas automatizadas:** `test/test_ui.php` comprueba de forma estática que esta capa define `consultarUltimaMedida`, que encapsula la única petición del navegador y que no toca el DOM.
* **Idempotencia:** esta capa no escribe en la base de datos, sólo lee, por lo que no altera el estado.
