# 6. Diseño de la placa Arduino

**Componente:** `src/arduino/`
**Autor:** David Ferra · **Fecha:** 2026-10-15

---

## 6.1. Diseño del componente

### Qué se quiere conseguir

La placa debe tomar una medida con su sensor y **anunciarla mediante un Beacon Bluetooth LE** (formato iBeacon), para que cualquier teléfono cercano pueda recibirla.

### La medida ficticia

El Sprint 0 pide probar la cadena completa **sin depender de sensores reales**. Por eso `Medidor.h` devuelve un valor inventado y fijo:

```
Medidor :: medirCO2() --> 123
```

Este **123** es la medida que debe recorrer toda la cadena hasta aparecer en la página web. En los avances posteriores se sustituirá por la lectura real del sensor, sin tocar nada más.

### La emisora

El nombre de la emisora BLE se ha fijado en:

```
GTI-David
```

El teléfono busca **exactamente** ese nombre. Si cambia en la placa, hay que cambiarlo también en `MainActivity.java` → `NOMBRE_EMISORA_BUSCADA`.

### Cómo codifica el Beacon la medida

La medida viaja en el `major` y el `minor` de la trama iBeacon:

```
major = (identificadorDeMedicion << 8) + contador
minor = valor medido
```

| Magnitud | `identificadorDeMedicion` |
|---|---|
| CO2 | `11` |
| TEMPERATURA | `12` |

De forma que una medida de CO2 con valor `123` y contador `1` produce:

```
major = (11 << 8) + 1 = 2817  ->  bytes 0x0B 0x01
minor = 123                   ->  bytes 0x00 0x7B
```

El contador va de `0` a `255` y luego vuelve a empezar, para que el receptor pueda detectar envíos perdidos.

### Diagrama de la placa

```
+----------------------------------------------------------+
|                  HolaMundoIBeacon.ino                     |
|                                                          |
|   setup()   arranca puerto serie, LED, BLE y servicio    |
|   loop()    mide y publica                               |
|                                                          |
+------+------------+------------+------------+------------+
       |            |            |            |
       v            v            v            v
  +---------+  +----------+  +---------+  +-----------+
  | Medidor |  |Publicador|  |   LED   |  |PuertoSerie|
  | medir() |  | publicar |  | on / off|  | escribir  |
  +----+----+  +-----+----+  +---------+  +-----------+
       |             |
       | 123         | usa
       v             v
  +--------------------------------------------+
  |            EmisoraBLE / ServicioEnEmisora  |
  |                  GTI-David                |
  +--------------------------------------------+
                    |
                    |  Beacon iBeacon
                    |  major = (id << 8) + contador
                    |  minor = valor
                    v
              TELEFONO ANDROID
```

### Ficheros

| Fichero | Responsabilidad |
|---|---|
| `HolaMundoIBeacon.ino` | Punto de entrada: `setup()` y `loop()`. |
| `Medidor.h` | Lee el sensor. **Aquí está la medida ficticia.** |
| `Publicador.h` | Convierte la medida en una trama iBeacon y la anuncia. |
| `EmisoraBLE.h` | Arranca y gestiona el adaptador Bluetooth LE. |
| `ServicioEnEmisora.h` | Define el servicio BLE que se anuncia. |
| `LED.h` | Enciende y apaga el LED de la placa. |
| `PuertoSerie.h` | Escribe mensajes de depuración. |

Los siete ficheros viven juntos dentro de la carpeta del boceto:

```
src/
└── arduino/
    └── HolaMundoIBeacon/
        ├── HolaMundoIBeacon.ino
        ├── EmisoraBLE.h
        ├── Medidor.h
        ├── Publicador.h
        ├── ServicioEnEmisora.h
        ├── LED.h
        └── PuertoSerie.h
```

> **Por qué hay esa carpeta intermedia:** Arduino IDE exige que un boceto `.ino` esté dentro de una carpeta que lleve **el mismo nombre que el boceto**. Sin ella, el IDE avisa y quiere mover el fichero por su cuenta, lo que separaría el `.ino` de sus cabeceras `#include`. Al estar los siete juntos en `HolaMundoIBeacon/`, el IDE abre el boceto directamente y todos los incluidos se resuelven.

---

## 6.2. Aclaraciones de diseño

### ¿Por qué iBeacon y no un servicio BLE normal?

Porque el iBeacon es el formato que **cualquier teléfono detecta sin conectarse a la placa**. Basta con estar escuchando. Es justo lo que hace el escáner de Android.

### ¿Por qué la medida se reparte entre `major` y `minor`?

Porque el iBeacon sólo tiene esos dos campos de 16 bits para datos propios. Repartirlos permite enviar el **tipo de medición y el contador** (`major`) junto con el **valor** (`minor`), sin tener que inventar una trama propia.

### ¿Por qué no se lee el sensor de verdad?

El objetivo del Sprint 0 es **probar la cadena entera** de principio a fin. Una medida fija la hace reproducible: quien eche un vistazo sabe que debe ver exactamente `123`. La medida real se añadirá en el siguiente sprint.

### La placa

El código usa `#include <bluefruit.h>`, que es de la librería **Adafruit nRF52**. Sirve para las placas **Feather nRF52840 Express** y **ItsyBitsy nRF52840**. Hay que instalar esa librería desde el gestor de librerías de Arduino IDE.

### Ingeniería inversa

El diseño de este componente se ha obtenido **por ingeniería inversa** del código de partida y se ha actualizado con respecto a la implementación actual: la medida ficticia `123` y el nombre de emisora `GTI-David` son posteriores al código original.

---

## 6.3. Reglas generales

* **Lenguaje de programación:** **C++** (Arduino / Adafruit nRF52).
* **Cabeceras de fichero:** todos los ficheros del boceto (`HolaMundoIBeacon.ino` y los `.h`) llevan cabecera con **nombre, descripción, copyright, fecha, autor y aportación**.
* **Cabeceras de función:** cada cabecera de clase lleva su diseño lógico dentro de un bloque delimitado por líneas `--------------------`.
* **Legibilidad:** una clase por responsabilidad. Nombres en castellano (`Medidor`, `Publicador`, `EmisoraBLE`).
* **Pruebas automatizadas:** no hay tests automáticos para la placa. La comprobación es visual: encender la placa y ver por el puerto serie que anuncia `GTI-David`. La medida ficticia es la que recoge el test de funcionamiento de la rúbrica.
* **Idempotencia:** cada `loop()` genera una medida y la publica, sin dejar estado acumulado.
