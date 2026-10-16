# Sprint 0 — Medidas de una placa Arduino en una página web

**Autor:** David Ferra · **Asignatura:** PBIO/PBTIO · **Grupo:** 3A · **Fecha:** 2026-10-15

---

## Qué hace el proyecto

Una placa **Arduino** mide el dióxido de carbono y lo anuncia por **Bluetooth LE** (formato iBeacon). Un teléfono **Android** escucha ese beacon y reenvía la medida por una **API REST** a un servidor con **lógica de negocio** en PHP, que la guarda en una **base de datos** MariaDB. Una **página web** muestra la medida más reciente.

```
ARDUINO (C++)  --BLE-->  ANDROID (Java)  --HTTP-->  REST (PHP)
                                                          |
                                                          v
                                                  LÓGICA DE NEGOCIO
                                                          |
                                                          v
                                              MariaDB (bd_beacon) --HTTP--> PÁGINA WEB
```

La medida ficticia del Sprint 0 es **123 ppm de CO2** (`Medidor.h` → `medirCO2()`). Es el valor que debe recorrer toda la cadena hasta aparecer en la página.

---

## Cómo desplegarlo

### 1. Instalar XAMPP

Descargarlo de [apachefriends.org](https://www.apachefriends.org/) e instalarlo. Desde el panel hay que arrancar **Apache** y **MariaDB**.

### 2. Crear la base de datos

Con `mysql`:

```
C:\xampp\mysql\bin\mysql.exe -u root < src/database/crear_bd.sql
```

O bien desde **phpMyAdmin** (`http://localhost/phpmyadmin` → Importar → `src/database/crear_bd.sql`).

### 3. Copiar el proyecto en el servidor

Copiar la carpeta `Prototipo` dentro de:

```
C:\xampp\htdocs\Prototipo
```

Con esto, la aplicación queda en `http://localhost/Prototipo/src/gui/Aplicacion.html`.

### 4. Verificar el servidor REST

```
curl http://localhost/Prototipo/src/communication/consultarUltimaMedida.php
```

Debe responder `{"error":"no hay medidas guardadas"}` si la base de datos está vacía.

### 5. Compilar el teléfono Android

```
cd src\android
gradlew assembleDebug
```

La APK queda en `app\build\outputs\apk\debug\app-debug.apk`. Se instala en el móvil con depuración USB activada.

> **IMPORTANTE:** en `src\android\...\ClienteRESTHttp.java` hay que cambiar la constante `URL_BASE_SERVIDOR` por la IP del ordenador (`ipconfig` → IPv4), por ejemplo `http://192.168.1.50:8080`.

### 6. Subir el programa a la placa

Abrir `src\arduino\HolaMundoIBeacon\HolaMundoIBeacon.ino` en **Arduino IDE**, con la librería **Adafruit nRF52** instalada, y seleccionar la placa (Feather nRF52840 Express). Después, subir.

> **Nota:** un boceto de Arduino debe estar siempre dentro de una carpeta que lleve su mismo nombre. Por eso el código está en `src\arduino\HolaMundoIBeacon\`, y no suelto en `src\arduino\`. Si Arduino IDE lo pide, hay que dejar que cree esa carpeta.

---

## Cómo ejecutar los tests

**Requisito:** XAMPP arrancado (Apache y MariaDB).

```
test\ejecutar_tests.bat
```

Ejecuta los cuatro juegos de pruebas. Devuelve `0` si todo pasa y `1` si algo falla, por lo que sirve también en un proceso de integración continua.

| Fichero | Qué comprueba | Pruebas |
|---|---|---|
| `test\test_bd.php` | Que la base de datos impone sus propias restricciones | 22 |
| `test\test_logica_negocio.php` | Las reglas de negocio de las medidas | 23 |
| `test\test_rest.php` | El servidor REST con peticiones HTTP reales | 17 |
| `test\test_ui.php` | Que la interfaz no se comunica por su cuenta y que la lógica del navegador tiene la misma firma que la del backend | 26 |

El test de interfaz es **estático**: revisa el código de `src/gui/` y de `src/frontend_business_logic/` sin necesitar Apache, navegador ni base de datos, así que se puede ejecutar en cualquier momento.

Se pueden ejecutar por separado:

```
C:\xampp\php\php.exe test\test_logica_negocio.php
```

**Todos los tests son independientes e idempotentes:** cada uno crea y borra sus propios datos, y al terminar la base de datos queda en el mismo estado que al empezar.

---

## Estructura del proyecto

```
Prototipo/
├── author.md                  autor del proyecto
├── README.md                  este fichero
├── src/
│   ├── arduino/               C++ — emite el beacon
│   ├── android/               Java — recibe y reenvía
│   ├── communication/         PHP — servidor REST
│   ├── business_logic/        PHP — reglas y base de datos
│   ├── database/              SQL — esquema de la base de datos
│   ├── frontend_business_logic/  JavaScript — lógica del navegador
│   └── gui/                   HTML — página de resultados
├── doc/
│   ├── android_design.md
│   ├── arduino_design.md
│   ├── communication_design.md
│   ├── business_logic_design.md
│   ├── database_design.md
│   ├── frontend_business_logic_design.md
│   ├── gui_design.md
│   └── prompts.md
└── test/
    ├── ejecutar_tests.bat
    ├── test_bd.php
    ├── test_logica_negocio.php
    ├── test_rest.php
    └── test_ui.php
```

Cada carpeta de `src/` tiene su diseño correspondiente en `doc/`, siguiendo la estructura de la asignatura.

---

## Cómo probarlo de principio a fin

1. Arrancar XAMPP (Apache y MariaDB).
2. Abrir `http://localhost/Prototipo/src/gui/Aplicacion.html`. Debe poner **«Consultando…»** o **«no hay medidas guardadas»**.
3. Encender la placa Arduino. Esperar unos segundos.
4. Abrir la aplicación en el móvil y pulsar **BUSCAR**. Debe poner `[ENVIADO] Medida enviada: CO2 = 123`.
5. Volver al navegador. En tres segundos aparecerá **123 ppm**.

Si no se tiene la placa a mano, en `MainActivity.java` se puede poner `USAR_COMPONENTES_FAKE = true` y la aplicación simula la misma medida sin Bluetooth.

---

## Requisitos de los programas

| Programa | Versión |
|---|---|
| XAMPP (Apache + MariaDB + PHP) | 8.2 / MariaDB 10.4 |
| Android Studio | cualquiera reciente |
| Gradle | 9.6.0 (incluido en el proyecto) |
| Java | 17 |
| Arduino IDE | cualquiera reciente |
| Librería Adafruit nRF52 | última versión |
