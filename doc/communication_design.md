# 3. Diseño del servidor REST

**Componente:** `src/communication/`
**Autor:** David Ferra · **Fecha:** 2026-10-15

---

## 3.1. Diseño del componente

### Qué se quiere conseguir

El servidor REST será el punto de comunicación entre el backend y los clientes. En este proyecto habrá **dos clientes**:

* El **teléfono**, que enviará medidas.
* El **navegador**, que solicitará la última medida.

El servidor se desarrollará en **PHP** y se ejecutará sobre **Apache** (XAMPP).

### Rutas

#### Registrar una medida

```
POST ../communication/guardarMedida.php
```

El teléfono enviará un objeto JSON como:

```json
{
  "sensor": "CO2",
  "valor": 123,
  "contador": 5
}
```

La ruta recogerá el valor y llamará a:

```
sensor:Text, valor:Z, contador:N --> guardarMedida() --> B
```

El resultado se transformará en una respuesta JSON.

#### Obtener la última medida

```
GET ../communication/consultarUltimaMedida.php
```

Esta ruta **no necesita recibir ningún parámetro**. Internamente utilizará:

```
--> consultarUltimaMedida() --> (sensor:Text, valor:Z, contador:N, fecha:Text) | error:Text
```

y devolverá el resultado al navegador.

### Diseño de las respuestas

| Situación | Código | Cuerpo |
|---|---|---|
| Medida registrada | `200` | `{"resultado": true}` |
| Medida rechazada | `400` | `{"resultado": false, "error": "medida no valida"}` |
| Cuerpo que no es JSON | `400` | `{"error": "el cuerpo no es un JSON valido"}` |
| Método incorrecto | `405` | `{"error": "se esperaba una peticion POST"}` |
| Última medida con éxito | `200` | `{"sensor":"CO2","valor":123,"contador":5,"fecha":"2026-10-15 19:55:22"}` |
| No hay medidas | `404` | `{"error": "no hay medidas guardadas"}` |

### Diagrama de la comunicación

```
   TELÉFONO (Android)                    NAVEGADOR (HTML + JS)
         |                                        |
         |  POST /communication/                  |  GET /communication/
         |        guardarMedida.php               |        consultarUltimaMedida.php
         |  {"sensor":"CO2",                      |
         |   "valor":123,                         |
         |   "contador":5}                        |
         |                                        |
         v                                        v
+----------------------------------------------------------+
|                    APACHE (XAMPP)                        |
|                                                          |
|   guardarMedida.php      consultarUltimaMedida.php       |
|         |                            |                   |
|         | guardarMedida()            | consultarUltima-  |
|         |                            | Medida()          |
|         v                            v                   |
|   +--------------------------------------------------+   |
|   |        business_logic / medidas.php              |   |
|   +--------------------------------------------------+   |
|                          |                               |
|                          | PDO                           |
|                          v                               |
|   +--------------------------------------------------+   |
|   |            MariaDB - bd_beacon - MEDIDAS        |   |
|   +--------------------------------------------------+   |
+----------------------------------------------------------+
         |                                        |
         |  {"resultado":true}                    |  {"sensor":"CO2",
         |                                        |   "valor":123, ...}
         v                                        v
```

### Ejemplos de uso

```bash
# guardar una medida
curl -X POST http://localhost/Prototipo/src/communication/guardarMedida.php ^
     -d "{\"sensor\":\"CO2\",\"valor\":123,\"contador\":5}"

# obtener la última
curl http://localhost/Prototipo/src/communication/consultarUltimaMedida.php
```

---

## 3.2. Aclaraciones de diseño

### ¿Por qué dos ficheros y no uno con parámetros?

Porque son operaciones distintas con métodos HTTP distintos. Separarlas en ficheros permite aplicar el método correcto a cada una (`POST` para crear, `GET` para leer) y devolver el código de estado adecuado.

### ¿Por qué el servidor no valida nada?

**La validación pertenece a la lógica de negocio.** El servidor REST sólo traduce:

* de HTTP a tipos del dominio (`json_decode` → `sensor`, `valor`, `contador`)
* y de vuelta (`array` → `json_encode`)

Si la lógica de negocio rechaza la medida, el servidor traduce ese `false` en un `400`. Si la lógica cambia mañana, el servidor no se toca.

### ¿Por qué se comprueba el método HTTP?

Porque una ruta GET no debería poder crear datos, y una ruta POST no debería devolver la última medida. Devolver `405` hace que un error de programación en el cliente salga a la luz enseguida.

### ¿Por qué se comprueba que el cuerpo sea JSON?

Si el cuerpo está mal formado, `json_decode` devuelve `null`. Sin esa comprobación, un error tonto en el cliente produciría un `500` confuso en lugar de un `400` claro.

### La URL del servidor desde el teléfono

`ClienteRESTHttp.java` contiene la constante `URL_BASE_SERVIDOR`. **Hay que cambiarla por la IP del ordenador** (`ipconfig` → IPv4). Es el único fichero que conoce esa dirección.

---

## 3.3. Reglas generales

* **Lenguaje de programación:** **PHP 8.2** sobre **Apache** (XAMPP).
* **Cabeceras de fichero:** cada fichero REST lleva cabecera con **nombre, descripción, copyright, fecha, autor y aportación**, además del método, el cuerpo, las respuestas y un ejemplo de `curl`.
* **Cabeceras de función:** cada función lleva su diseño lógico dentro de un bloque delimitado por líneas `--------------------`, por ejemplo `Text cuerpo --> guardarMedidaDesdeJson() --> B`.
* **Legibilidad:** sólo se distinguen los códigos `200`, `400` y `405`. Un fichero por ruta.
* **Pruebas automatizadas:** `test/test_rest.php` con 17 pruebas: pega peticiones HTTP reales con `curl` y comprueba códigos y cuerpos.
* **Idempotencia:** el test borra todas las medidas al terminar, por lo que puede ejecutarse cuantas veces se quiera.
