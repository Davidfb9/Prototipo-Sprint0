# 2. Diseño de la lógica de negocio del backend

**Componente:** `src/business_logic/`
**Autor:** David Ferra · **Fecha:** 2026-10-15

---

## 2.1. Diseño del componente

### Qué se quiere conseguir

Esta parte será la encargada de decidir qué hacer con las medidas que recibe el sistema. Se implementará en **PHP** y utilizará la base de datos definida en `database_design.md`.

**La lógica tiene que funcionar independientemente de cómo llegue la petición. Por eso no debe depender del servidor REST.**

> Este fichero **no debe contener** ninguna referencia a HTTP, rutas, peticiones, respuestas, sockets, códigos 200, 302, 404, 500, ni a formatos de datos de la comunicación.

### Operaciones

#### Guardar una medida

```
sensor:Texto, valor:Z, contador:N -> guardarMedida() -> VoF
```

La función recibirá una medida y determinará si se puede registrar.

* Si el sensor es `CO2` o `TEMPERATURA`, el valor está entre `-1000` y `100000` y el contador está entre `0` y `255`, se guardará en la base de datos con la fecha actual y se devolverá `true`.
* Si cualquiera de los tres está fuera de dominio, **no se hará ninguna inserción** y se devolverá `false`.

#### Recuperar la última medida

```
-> consultarUltimaMedida() -> (sensor:Texto, valor:Z, contador:N, fecha:Texto) | error:Texto
```

Esta función buscará la medida más reciente.

* Si existe, devolverá un array con el sensor, el valor, el contador y la fecha.
* Si no hay ninguna medida almacenada, devolverá el texto `no hay medidas guardadas`.

#### Funciones auxiliares

```
s:Texto -> esSensorValido() -> VoF
v:Z     -> valorEnRango() -> VoF
c:N     -> contadorEnRango() -> VoF
-> contarMedidas() -> N
-> borrarTodasLasMedidas() -> VoF
```

### Diagrama de clases

```
+-------------------------------------------------------+
|                    business_logic                     |
+-------------------------------------------------------+
|                                                       |
|   guardarMedida()                                     |
|      sensor:Texto, valor:Z, contador:N -> VoF         |
|                                                       |
|   consultarUltimaMedida()                             |
|      -> (sensor, valor, contador, fecha) | error      |
|                                                       |
|   esSensorValido()  s:Texto -> VoF                    |
|   valorEnRango()    v:Z     -> VoF                    |
|   contadorEnRango() c:N     -> VoF                    |
|   contarMedidas()           -> N                      |
|   borrarTodasLasMedidas()   -> VoF                    |
|                                                       |
+---------------------------+---------------------------+
                            |
                            | utiliza
                            v
+-------------------------------------------------------+
|           MEDIDAS  (ver database_design.md)           |
+-------------------------------------------------------+
```

---

## 2.2. Aclaraciones de diseño

### ¿Qué es un "sensor"?

El Arduino puede medir dos magnitudes. Cada una es un sensor distinto:

| Sensor | Magnitud | Unidad | Valores |
|---|---|---|---|
| `CO2` | Dióxido de carbono | ppm | -1000 a 100000 |
| `TEMPERATURA` | Temperatura ambiente | °C | -1000 a 100000 |

### ¿Qué es el contador?

Es el número de envío que el Arduino incrementa cada vez que publica una medida. Va de `0` a `255` y luego vuelve a empezar. Permite saber si se ha perdido algún envío. Es el mismo campo que el Beacon transporta en el `major`.

### ¿Por qué `consultarUltimaMedida` devuelve un texto en el caso de error?

Para que **el tipo de retorno indique el resultado**, sin necesidad de comprobar códigos. Devuelve dos tipos distintos:

* Un **array** con la medida, si la hay.
* Un **texto** de error, si no la hay.

El llamador distingue los dos casos con `is_array()` o `is_string()`. No hace falta mirar ningún código de estado HTTP.

### ¿Por qué se comprueban los parámetros dos veces?

La primera en la lógica de negocio, para dar un mensaje claro. La segunda en la base de datos, con las restricciones `CHECK`, para que ninguna otra vía pueda colar un dato inválido.

### ¿Dónde está la conexión con la base de datos?

En `config.php`, que devuelve un objeto `PDO`. La lógica de negocio lo usa, pero **no sabe nada del servidor REST**. Podría reutilizarse igual desde un script de consola, un cron o un test.

### Nada de HTTP

En todo el fichero no aparecen:

* rutas (`/communication/...`)
* códigos de estado (`200`, `404`, `405`)
* peticiones (`$_GET`, `$_POST`, `REQUEST_METHOD`)
* formato de los datos (`json_encode`, cabeceras)

---

## 2.3. Reglas generales

| Regla | Aplicación en este componente |
|---|---|
| **Lenguaje de programación** | **PHP 8.2** (XAMPP). Acceso a datos con PDO. |
| **Cabeceras** | Fichero `medidas.php` con cabecera completa. Cada función lleva su diseño lógico entre líneas `--------------------`, por ejemplo `sensor:Texto, valor:Z, contador:N -> guardarMedida() -> VoF`. |
| **Legibilidad** | Una función por responsabilidad. Los límites del dominio están en constantes con nombre (`VALOR_MINIMO`, `VALOR_MAXIMO`), no en números sueltos. |
| **Pruebas automatizadas** | `tests/test_logica_negocio.php` con 23 pruebas: validaciones, historial, tipos devueltos e idempotencia. |
| **Idempotencia** | Cada prueba crea y borra sus propios datos. Al terminar, la tabla tiene el mismo número de filas que al empezar. |
