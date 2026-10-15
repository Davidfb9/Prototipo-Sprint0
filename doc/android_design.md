# 7. Diseño del cliente Android

**Componente:** `src/android/`
**Autor:** David Ferra · **Fecha:** 2026-10-15

---

## 7.1. Diseño del componente

### Qué se quiere conseguir

El teléfono debe **escuchar** los beacons que emite la placa Arduino y **enviar** cada medida al servidor REST para que quede guardada en la base de datos.

Se implementará en **Java** para Android (mínimo SDK 28, es decir Android 9).

### Estados del teléfono

El funcionamiento se divide en cuatro estados:

```
DETENIDO
    |
    v
ESCANEANDO
    |
    v
DETECTADO
    |
    v
ENVIADO
```

* **DETENIDO**: no se está buscando ningún beacon.
* **ESCANEANDO**: el teléfono está esperando una detección.
* **DETECTADO**: el escáner ha proporcionado una medida.
* **ENVIADO**: la medida se ha enviado correctamente al servidor.

Existe además un estado **ERROR** para cuando algo falla de verdad (sin permisos, escáner no disponible, servidor caído).

Los beacons de un tipo que no interesa —por ejemplo el beacon libre— **no** son un error: simplemente se ignoran y el escaneo continúa.

### Componentes

Se utilizarán estas clases e interfaces:

| Fichero | Tipo | Responsabilidad |
|---|---|---|
| `LogicaTelefono.java` | clase | Lógica de negocio. Coordina escáner y cliente. **No conoce ni Bluetooth ni HTTP.** |
| `EscannerBeacon.java` | interfaz | Contrato de búsqueda de beacons. |
| `EscannerBeaconBTLE.java` | clase | Implementación real, con Bluetooth LE. |
| `EscannerBeaconFake.java` | clase | Implementación simulada, sin Bluetooth. |
| `ClienteREST.java` | interfaz | Contrato de envío al servidor. |
| `ClienteRESTHttp.java` | clase | Implementación real, con peticiones HTTP. |
| `ClienteRESTFake.java` | clase | Implementación simulada, sin servidor. |
| `TramaIBeacon.java` | clase | Decodifica la trama iBeacon y valida que sea del proyecto. |
| `Utilidades.java` | clase | Utilidades de conversión de bytes. |
| `PeticionarioREST.java` | clase | Cliente HTTP bajo nivel. |
| `MainActivity.java` | clase | **Sólo la parte visual**, los permisos y el enganche con el servicio. |
| `ServicioEscaneoBeacon.java` | clase | **Servicio de primer plano.** Dueño de `LogicaTelefono`; mantiene el escaneo vivo en segundo plano. |

### Diagrama de clases

```
+------------------------------------------------------------+
|                      MainActivity                          |
|         (sólo interfaz gráfica, permisos y binder)         |
|                                                            |
|   onCreate()  --> pide permisos                            |
|   boton EMPEZAR --> startForegroundService() + bind        |
|   boton PARAR   --> servicio.pararEscaneo()                |
|   cambioDeEstado()  --> pinta                              |
+---------------------------+--------------------------------+
                            | se engancha (ServiceConnection)
                            v
+------------------------------------------------------------+
|                   ServicioEscaneoBeacon                    |
|              (SERVICIO DE PRIMER PLANO / FOREGROUND)       |
|                                                            |
|   onStartCommand() --> busca el beacon                     |
|   cambioDeEstado() --> notificación + reenvía a la UI      |
|   pararEscaneo()   --> stopSelf()                          |
|                                                            |
|   Mantiene el escaneo vivo aunque la app pase a 2º plano   |
+---------------------------+--------------------------------+
                            | observa
                            v
+------------------------------------------------------------+
|                      LogicaTelefono                        |
|               (LÓGICA DE NEGOCIO DEL TELÉFONO)             |
|                                                            |
|   buscarBeacon(nombreBuscado)                              |
|   detenerBusqueda()                                        |
|   guardarMedida(sensor, valor, contador)                   |
|   nombreDelSensor(idDeMedicion)                            |
|                                                            |
|   Estados: DETENIDO, ESCANEANDO, DETECTADO, ENVIADO, ERROR |
+----------------------+-------------------+-----------------+
                       |                   |
        implementa     |                   |     implementa
                       v                   v
+---------------------------+   +---------------------------+
|       EscannerBeacon      |   |        ClienteREST        |
+---------------------------+   +---------------------------+
| empezarABuscar(nombre,cb) |   | guardarMedida(            |
| detenerBusqueda()         |   |   sensor, valor, contador,|
+-------------+-------------+   |   cb)                     |
              |                 +-------------+-------------+
     +--------+--------+                   |           |
     |                 |                   |           |
     v                 v                   v           v
+-----------+   +---------------+   +-----------+  +-------------+
| BTLE real |   |  escáner fake |   | HTTP real |  | cliente fake|
+-----------+   +---------------+   +-----------+  +-------------+
```

### Filtrado del beacon

La placa emite **tres** iBeacon con el mismo nombre `GTI-David`:

| Beacon | UUID | major |
|---|---|---|
| CO2 | `EPSG-GTI-PROY-3A` | `(11 << 8) + contador` |
| Temperatura | `EPSG-GTI-PROY-3A` | `(12 << 8) + contador` |
| Libre (`MolaMola...`) | `MolaMolaMolaMola` | `0x4D6F` |

`EscannerBeaconBTLE` descarta por **UUID**: sólo se queda con `EPSG-GTI-PROY-3A`. Sin ese filtro el beacon libre se interpreta como el tipo de medición `77`, que no existe, y el proceso se detenía con error.

### Identidad de firmas con el servidor

La función de guardar medida del teléfono es la **misma** que la del servidor:

```
TELÉFONO:  sensor:Texto, valor:Z, contador:N -> guardarMedida() -> VoF
SERVIDOR:  sensor:Texto, valor:Z, contador:N -> guardarMedida() -> VoF
```

Sólo cambia que en el teléfono la respuesta es asíncrona y llega por el observador.

---

## 7.2. Aclaraciones de diseño

### ¿Por qué hay una interfaz y dos implementaciones?

Para poder **probar toda la lógica del teléfono sin placa y sin servidor**. Cambiando dos líneas en `crearEscanner()` y `crearClienteREST()` se pasa de la demo real a una simulación completa, y ninguna otra clase se modifica.

Esto es exactamente lo que pide la rúbrica: *"Existe una lógica fake en el cliente Android y en el cliente web"* y *"la lógica fake está separada en una clase independiente"*.

### ¿Dónde se cambia la IP del servidor?

En **un solo fichero**: `ClienteRESTHttp.java`, en la constante

```java
private static final String URL_BASE_SERVIDOR = "http://172.31.151.124/Prototipo/src";
```

Hay que sustituirla por la IP del ordenador (`ipconfig` → IPv4).

Dos detalles fáciles de olvidar:

* **El puerto no se pone.** XAMPP escucha en el **80**, que es el predeterminado de HTTP. Poner `:8080` da un fallo de conexión.
* **Hay que añadir `/Prototipo/src`.** El proyecto está desplegado dentro de `htdocs/Prototipo`, así que la ruta real es
  `http://<IP>/Prototipo/src/communication/guardarMedida.php`.

Comprobación rápida desde el propio ordenador:

```
curl http://172.31.151.124/Prototipo/src/communication/consultarUltimaMedida.php
```

### ¿Y si quiero probar sin servidor?

En `MainActivity.java` se pone `USAR_COMPONENTES_FAKE = true`. La aplicación usa `EscannerBeaconFake` + `ClienteRESTFake` y simula una medida de CO2 con valor `123`, igual que la placa.

### ¿Por qué la lógica no conoce Bluetooth?

Porque así se puede probar sin hardware. `LogicaTelefono` sólo trabaja con `EscannerBeacon` y `ClienteREST`. Si mañana el beacon llega por NFC o por Wi-Fi, se añade otra implementación y la lógica no se toca.

### ¿Por qué `MainActivity` no decide nada?

Porque es la parte que más cambia (permisos, versión de Android, diseño de la pantalla). Mantenerla vacía de lógica evita tener que reescribirla cuando cambie el negocio.

### ¿Por qué hay un servicio de primer plano?

Android **mata** los procesos que pasan a segundo plano. Si el escaneo viviera en la actividad, se cortaría en cuanto el usuario cambiara de app o girara el móvil.

`ServicioEscaneoBeacon` es un *foreground service*: mantiene una **notificación permanente** visible y, mientras esté, el sistema no mata el proceso. Es la única forma fiable de escanear en segundo plano.

La actividad no es dueña de nada: al pulsar EMPEZAR lanza el servicio y se **engancha** con un `ServiceConnection`; al destruirse se **desengancha** pero **no para el servicio**, que es justamente el objetivo.

```
EMPEZAR  --> startForegroundService() + bindService()
PARAR    --> servicio.pararEscaneo()  --> stopSelf()
```

El servicio además puede ser **recreado solo** por Android (`START_STICKY`) si el sistema llega a matarlo.

### ¿Por qué no se pone la bandera `neverForLocation`?

Es una bandera que se declara en `BLUETOOTH_SCAN` para decirle a Android *"no uso el Bluetooth para localización"*. El sistema se lo toma en serio y **elimina de los resultados todo anuncio que sirva para ubicar**… y los iBeacon de Apple (`0x004C`) son exactamente eso.

Con esa bandera el beacon es **invisible** para la aplicación, aunque `nRF Connect` lo muestre perfectamente. Por eso no se usa.

### ¿Y entonces hace falta permiso de localización? **SÍ, siempre**

Es la contrapartida, y es el fallo más trágico porque no da ningún error. Al no usar `neverForLocation`, Android exige `ACCESS_FINE_LOCATION`. Sin él:

* el escáner **se registra perfectamente** (`onScannerRegistered() - status=0`)
* el escaneo **se lanza sin problemas**
* pero el sistema **descarta todos los resultados en silencio**

La aplicación no se queja de nada: se queda escuchando para siempre. Por eso `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` **no llevan `android:maxSdkVersion="30"`**, sino que se piden en todas las versiones.

Documentación oficial (`BluetoothLeScanner`):

> *"In addition, this requires the `ACCESS_FINE_LOCATION` permission. For apps targeting Android 12 or higher, an **alternative** is declaring `usesPermissionFlags="neverForLocation"`."*

Es decir: o `neverForLocation` (y los iBeacon quedan filtrados), o permiso de localización (y el iBeacon se ve). No hay tercera vía.

### Y además el **interruptor** de localización tiene que estar encendido

El permiso no basta: el **GPS del teléfono tiene que estar ENCENDIDO**. Si está apagado, Android devuelve cero resultados de escaneo BLE sin dar ningún error. `MainActivity` lo comprueba antes de arrancar el servicio y avisa en pantalla si está apagado.

### ¿Cómo se evita inundar el servidor?

Un iBeacon se recibe **decenas de veces por segundo**. `LogicaTelefono` sólo envía una medida cuando:

1. cambia el **contador** que envía el Arduino, y además
2. han pasado al menos **5 segundos** desde el último envío **de ese mismo sensor**.

Sin eso la aplicación lanzaría cientos de peticiones REST por segundo.

> **ATENCIÓN: el límite tiene que ser POR SENSOR, no global.**
>
> Con una única marca de tiempo compartida entre todos los sensores, el CO2 se comía siempre el turno: en el `loop()` del Arduino el CO2 se publica **antes** que la temperatura, y como el periodo del bucle (~7,6 s) ya supera los 5 segundos, cuando llegaba la temperatura el cupo ya estaba gastado. El resultado, medido de verdad en la base de datos, fue **22 medidas de CO2 y 1 sola de temperatura**: la temperatura no llegaba nunca.
>
> Guardando el contador y la hora del último envío **por separado para cada sensor**, los dos envían una vez por vuelta del bucle (~7,6 s), lo cual respeta el mínimo de 5 segundos.

### ¿Y el logcat, que se llena?

El escaneo se lanza **sin filtros** (ver §7.1), así que ve todos los Bluetooth LE del entorno. En el aula son decenas de emisoras anunciándose ~15 veces por segundo cada una.

Para que siga siendo legible:

- Los paquetes **descartados** se avisan como mucho una vez cada **3 s**, y el aviso indica cuántos se suprimieron en medio (`+N descartes desde el aviso anterior`).
- El volcado en hexadecimal de cada paquete pasó a `Log.v`, que no sale por defecto.
- El estado `DETECTADO` sólo se emite cuando la medida **pasa todos los filtros y se va a enviar**, no por cada paquete recibido.

De forma normal el logcat muestra un puñado de líneas cada 5 segundos, una por medida enviada.

### La medida ficticia

`EscannerBeaconFake` devuelve `CO2 = 123`, que es **exactamente el mismo valor** que emite `Medidor.h` en la placa. Así la simulación y la demo real son intercambiables.

---

## 7.3. Reglas generales

| Regla | Aplicación en este componente |
|---|---|
| **Lenguaje de programación** | **Java 17**. Proyecto **Gradle** 9.6 / AGP 9.4.1 / AndroidX. Sin Kotlin. |
| **Cabeceras** | Cabecera en todos los ficheros con fichero, proyecto, autor, fecha y descripción. Cada función lleva su diseño lógico entre líneas `--------------------`. |
| **Legibilidad** | Una clase por responsabilidad. Interfaces con nombres de dominio (`EscannerBeacon`, `ClienteREST`). Nombres en castellano. |
| **Segundo plano** | El escaneo vive en `ServicioEscaneoBeacon`, un **foreground service** con notificación permanente, declarado en el manifiesto con `foregroundServiceType="connectedDevice"`. |
| **Permisos** | `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` desde Android 12 y `BLUETOOTH`/`BLUETOOTH_ADMIN` antes. `ACCESS_FINE_LOCATION` **siempre**, porque no se usa `neverForLocation`. Sin `neverForLocation`. El GPS del teléfono debe estar encendido. |
| **Pruebas automatizadas** | La comprobación automática del código Java es que **compile**: `gradlew assembleDebug`. Las pruebas de comportamiento se hacen con los fakes. |
| **Idempotencia** | El teléfono no escribe directamente en la base de datos, sólo envía medidas al servidor, y nunca repite la misma medida. |
