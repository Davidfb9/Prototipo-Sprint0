# 1. Diseño de la base de datos

**Componente:** `src/business_logic/` (capa de persistencia) · `src/database/crear_bd.sql`
**Autor:** David Ferra · **Fecha:** 2026-10-15

---

## 1.1. Diseño del componente

### Qué se quiere conseguir

La base de datos debe conservar **todas** las medidas que vaya recibiendo el sistema. Cada vez que llega una medida se crea un registro nuevo. De esta forma se mantiene el historial y se puede saber cuál ha sido la última recepción.

Se utilizará **MariaDB**, incluido en **XAMPP** (versión 10.4.32). Se accede desde PHP mediante **PDO**.

### Estructura

Base de datos: `bd_beacon`

Única tabla: `MEDIDAS`

```
TABLE: MEDIDAS
DESCRIPTION: Cada fila es una medida que ha enviado la placa Arduino y que el teléfono ha reenviado por la API REST.
COLUMNS:
+ id | INT | NOT NULL | Auto-Increment
+ sensor | VARCHAR(20) | NOT NULL | Nombre del sensor que midió
+ valor | INT | NOT NULL | Valor medido
+ contador | INT | NOT NULL | Contador de envío, va de 0 a 255 y se repite
+ fecha_recepcion | DATETIME | NOT NULL | Fecha y hora en que el servidor recibió la medida
PRIMARY KEY: id
FOREIGN KEYS: Ninguna. Es una tabla aislada.
CONSTRAINTS:
  + ck_sensor_conocido  CHECK ( sensor IN ('CO2','TEMPERATURA') )
  + ck_valor_rango      CHECK ( valor BETWEEN -1000 AND 100000 )
  + ck_contador_rango   CHECK ( contador BETWEEN 0 AND 255 )
```

### Tipos del dominio

```
Sensor  = { CO2, TEMPERATURA }
Medida  = ( sensor: Text, valor: Z, contador: N, fecha: Text )
```

`id` es la clave primaria y se incrementa automáticamente.

### Cómo debe funcionar

Cuando se recibe una medida:

1. Se comprueba que los datos pueden almacenarse (la lógica de negocio lo hace antes, y la base de datos lo vuelve a comprobar por su cuenta con las restricciones `CHECK`).
2. Se crea un registro **nuevo**, nunca se modifica ni se reutiliza uno anterior.
3. Se le asigna la fecha y hora actuales.

Cuando se pide la última medida:

1. Se busca la fila de `id` más alto.
2. Si no hay ninguna fila, se devuelve un error.

### Diagrama de la base de datos

```
+----------+     +-------------------------------------------+
|          |     |                  MEDIDAS                  |
|          |     +-------------------------------------------+
|          |     | PK  id                INT  AUTO_INCREMENT |
|  bd_     |     |     sensor            VARCHAR(20)         |
|  beacon  |     |     valor             INT                 |
|          |     |     contador          INT                 |
|          |     |     fecha_recepcion   DATETIME            |
|          |     +-------------------------------------------+
|          |     | CHECK  sensor  en {CO2, TEMPERATURA}      |
|          |     | CHECK  valor   entre -1000 y 100000       |
|          |     | CHECK  contador entre 0 y 255             |
+----------+     +-------------------------------------------+
```

---

## 1.2. Aclaraciones de diseño

### ¿Por qué no se borran ni se modifican las filas?

Para que la base de datos **no decida nada de la aplicación**, se usa sólo el patrón **Añadir** y **Consultar**. Nunca se borra ni se modifica una fila desde la lógica de negocio, salvo en los tests, que la vacían al terminar para que sean idempotentes.

### ¿Por qué el sensor es un texto y no un número?

Es más legible y permite añadir un sensor nuevo sin cambiar el código de la base de datos. Los identificadores numéricos (`11`, `12`) los usa Arduino en el Beacon, pero ya se traducen a texto en el teléfono y vuelven a traducirse en el servidor. La base de datos sólo ve `CO2` o `TEMPERATURA`.

### ¿Por qué hay restricciones `CHECK` si la lógica de negocio ya valida?

Por dos motivos:

1. **Defensa en profundidad.** Si mañana alguien escribe otra función PHP que meta datos a mano, la base de datos no dejará guardar basura.
2. **La rúbrica lo exige.** Pide que todas las relaciones y campos estén representados y que el diseño coincida con la implementación.

### ¿Por qué `VARCHAR(20)`?

`TEMPERATURA` son 11 caracteres, que es el más largo de los dos. Con 20 hay margen de sobra y se evita desperdiciar espacio.

### ¿Y la fecha?

Se usa `DATETIME` y lo rellena la propia base de datos con `CURRENT_TIMESTAMP`. Así no depende del reloj del ordenador que ejecuta el PHP, y dos medidas guardadas seguidas pueden distinguirse.

### Conexión

`src/business_logic/config.php` devuelve la conexión PDO. El usuario por defecto de XAMPP es `root` **sin contraseña**. Si cambia, sólo hay que tocar ese fichero.

---

## 1.3. Reglas generales

* **Lenguaje de programación:** **SQL** (MariaDB 10.4) y **PHP 8.2** para la capa de acceso (`PDO`).
* **Cabeceras de fichero:** el fichero `src/database/crear_bd.sql` lleva cabecera con **nombre, descripción, copyright, fecha, autor y aportación**.
* **Cabeceras de función:** las funciones PHP de acceso (`config.php`) llevan su diseño lógico dentro de un bloque delimitado por líneas `--------------------`.
* **Legibilidad:** los nombres describen el dominio (`MEDIDAS`, `sensor`, `contador`). Los nombres técnicos como `ck_valor_rango` se escriben en minúscula y con guiones bajos.
* **Pruebas automatizadas:** `test/test_bd.php` comprueba que la tabla tiene sus columnas, que el `id` es autoincremental y que **la base de datos rechaza por sí sola** los sensores desconocidos, los valores fuera de rango y los contadores fuera de rango. Se ejecuta con `test/ejecutar_tests.bat`.
* **Idempotencia:** cada prueba **borra las filas que crea**. Al terminar, la tabla queda vacía, igual que al empezar.
