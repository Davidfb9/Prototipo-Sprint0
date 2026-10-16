package org.jordi.pruebarest1;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

// ---------------------------------------------------------------------------------------
// Fichero:  LogicaTelefono.java
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10
// Copyright:   (c) 2026 David Ferra
// Aportación:  Logica de negocio del telefono: decide que hacer con cada beacon.
//
// Descripción: lógica de negocio del teléfono.
//
//              Coordina el escáner de beacons y el cliente REST. Esta clase NO
//              conoce ni Bluetooth LE ni HTTP: sólo trabaja con los contratos
//              EscannerBeacon y ClienteREST, y con tipos de dominio
//              (sensor:Text, valor:Z, contador:N).
//
//              Por eso funciona igual con los componentes reales o con los fakes.
//
// Diseño: doc/android_design.md
// ---------------------------------------------------------------------------------------
public class LogicaTelefono {

    private static final String ETIQUETA_LOG = ">>>>";

    // identificadores de medicion, igual que en Publicador.h :: MedicionesID
    public static final int MEDICION_CO2 = 11;
    public static final int MEDICION_TEMPERATURA = 12;

    public static final String SENSOR_CO2 = "CO2";
    public static final String SENSOR_TEMPERATURA = "TEMPERATURA";

    // ------------------------------------------------------------------------------------
    // El iBeacon se repite decenas de veces por segundo (anuncio ~60 veces/seg).
    //
    // Sin esta salvaguarda la app lanzaría cientos de peticiones REST por segundo
    // contra el servidor. Cada sensor se envía como mucho una vez cada 5 segundos,
    // y nunca dos veces la misma medida (mismo sensor y mismo contador).
    //
    // ATENCION: el límite tiene que ser POR SENSOR, no global.
    //
    //           Con una única marca de tiempo compartida, el CO2 se comía siempre el
    //           turno: en el loop() del Arduino el CO2 se publica ANTES que la
    //           temperatura, y como el periodo del loop (~7,6 s) ya supera los 5 s,
    //           cuando llegaba la temperatura el cupo ya estaba gastado. Resultado
    //           real medido en la base de datos: 22 medidas de CO2 y 1 sola de
    //           temperatura. La temperatura no llegaba nunca.
    // ------------------------------------------------------------------------------------
    private static final long INTERVALO_MINIMO_ENTRE_ENVIOS_MS = 5000;

    // estado del último envío, guardado aparte para cada sensor
    private final Map<String, Long> ultimoEnvioMsPorSensor = new HashMap<String, Long>();
    private final Map<String, Integer> ultimoContadorEnviadoPorSensor = new HashMap<String, Integer>();

    // ------------------------------------------------------------------------------------
    // Estados del teléfono: el proceso de principio a fin.
    //
    //     DETENIDO --> ESCANEANDO --> DETECTADO --> ENVIADO
    //
    // ------------------------------------------------------------------------------------
    public enum Estado {
        DETENIDO,
        ESCANEANDO,
        DETECTADO,
        ENVIADO,
        ERROR
    }

    // ------------------------------------------------------------------------------------
    // Notificación hacia la interfaz gráfica.
    // ------------------------------------------------------------------------------------
    public interface Observador {

        // estado:Text mensaje:Text --> cambioDeEstado() -->
        void cambioDeEstado(Estado estado, String mensaje);
    }

    private final Observador elObservador;
    private final EscannerBeacon elEscanner;
    private final ClienteREST elCliente;

    private Estado elEstadoActual = Estado.DETENIDO;

    // ------------------------------------------------------------------------------------
    // o:Text e:Text c:Text --> LogicaTelefono() -->
    // ------------------------------------------------------------------------------------
    public LogicaTelefono(Observador o, EscannerBeacon e, ClienteREST c) {
        this.elObservador = o;
        this.elEscanner = e;
        this.elCliente = c;
    } // ()

    // ------------------------------------------------------------------------------------
    // --> getEstadoActual() --> Estado
    // ------------------------------------------------------------------------------------
    public Estado getEstadoActual() {
        return this.elEstadoActual;
    } // ()

    // ------------------------------------------------------------------------------------
    // nombreBuscado:Text --> buscarBeacon() --> void
    // ------------------------------------------------------------------------------------
    public void buscarBeacon(String nombreBuscado) {

        cambiarEstado(Estado.ESCANEANDO, "Buscando el beacon " + nombreBuscado + " ...");

        this.elEscanner.empezarABuscar(nombreBuscado, new EscannerBeacon.Deteccion() {
            @Override
            public void beaconDetectado(String nombre, int tipoDeMedicion, int valor, int contador) {

                // ATENCION: aquí NO se avisa todavía de que se ha detectado nada.
                //
                //          El beacon se anuncia ~15 veces por segundo, así que si
                //          se avisara aquí el logcat y la pantalla se llenarían a
                //          esa razón. El aviso DETECTADO se emite dentro de
                //          enviarMedida(), y sólo cuando la medida pasa los filtros
                //          y se va a enviar de verdad.
                enviarMedida(nombre, tipoDeMedicion, valor, contador);
            }

            @Override
            public void error(String mensaje) {
                cambiarEstado(Estado.ERROR, mensaje);
            }
        });
    } // ()

    // ------------------------------------------------------------------------------------
    // --> detenerBusqueda() --> void
    // ------------------------------------------------------------------------------------
    public void detenerBusqueda() {
        this.elEscanner.detenerBusqueda();
        cambiarEstado(Estado.DETENIDO, "Búsqueda detenida.");
    } // ()

    // ------------------------------------------------------------------------------------
    // nombre:Text tipoDeMedicion:Z valor:Z contador:N --> enviarMedida() --> void
    //
    // Traduce el identificador numérico que envía Arduino al nombre del sensor.
    //
    // ATENCION: un tipo de medición desconocido NO es un error. El Arduino emite
    //           varios beacons y aquí sólo nos interesan el CO2 y la temperatura,
    //           así que el resto se ignoran sin tumbar el proceso.
    // ------------------------------------------------------------------------------------
    private void enviarMedida(final String nombre, final int tipoDeMedicion,
                              final int valor, final int contador) {

        final String sensor = nombreDelSensor(tipoDeMedicion);

        if (sensor == null) {
            Log.d(ETIQUETA_LOG, " enviarMedida(): tipo de medicion "
                    + tipoDeMedicion + " ignorado");
            return;
        }

        // ---------------------------------------------------------------------------
        // el mismo anuncio llega decenas de veces por segundo: no repetir la MISMA
        // medida (mismo sensor y mismo contador). Se mira solo la contabilidad de
        // ESTE sensor, para que un sensor no bloquee a otro.
        // ---------------------------------------------------------------------------
        Integer contadorEnviadoDeEsteSensor = this.ultimoContadorEnviadoPorSensor.get(sensor);

        if (contadorEnviadoDeEsteSensor != null && contador == contadorEnviadoDeEsteSensor) {
            return;
        }

        // ---------------------------------------------------------------------------
        // además, como mucho una medida de este sensor cada 5 segundos
        // ---------------------------------------------------------------------------
        long ahora = System.currentTimeMillis();
        Long ultimoEnvioDeEsteSensor = this.ultimoEnvioMsPorSensor.get(sensor);

        if (ultimoEnvioDeEsteSensor != null
                && (ahora - ultimoEnvioDeEsteSensor) < INTERVALO_MINIMO_ENTRE_ENVIOS_MS) {
            return;
        }

        this.ultimoContadorEnviadoPorSensor.put(sensor, contador);
        this.ultimoEnvioMsPorSensor.put(sensor, ahora);

        // ---------------------------------------------------------------------------
        // aquí la medida SÍ va a enviarse: éste es el momento de avisar
        // ---------------------------------------------------------------------------
        cambiarEstado(Estado.DETECTADO,
                "Beacon " + nombre + " detectado: " + sensor + " = " + valor);

        guardarMedida(sensor, valor, contador);
    } // ()

    // ------------------------------------------------------------------------------------
    // idDeMedicion:Z --> nombreDelSensor() --> Text | null
    // ------------------------------------------------------------------------------------
    private String nombreDelSensor(int idDeMedicion) {

        if (idDeMedicion == MEDICION_CO2) {
            return SENSOR_CO2;
        }

        if (idDeMedicion == MEDICION_TEMPERATURA) {
            return SENSOR_TEMPERATURA;
        }

        return null;
    } // ()

    // ------------------------------------------------------------------------------------
    // sensor:Text valor:Z contador:N --> guardarMedida() --> void
    //
    // IDENTIDAD DE FIRMAS. Esta cabecera es la MISMA que la de la lógica de
    // negocio del servidor:
    //
    //     sensor:Text, valor:Z, contador:N --> guardarMedida() --> B
    //
    // El cliente y el servidor, por tanto, hablan del mismo dominio.
    //
    // Sólo cambia la respuesta: aquí es asíncrona, así que se notifica por el
    // observador en lugar de devolverla.
    // ------------------------------------------------------------------------------------
    public void guardarMedida(final String sensor, final int valor, final int contador) {

        Log.d(ETIQUETA_LOG, " guardarMedida(): " + sensor + " = " + valor);

        this.elCliente.guardarMedida(sensor, valor, contador, new ClienteREST.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                cambiarEstado(Estado.ENVIADO,
                        "Medida enviada: " + sensor + " = " + valor);
            }

            @Override
            public void error(int codigo, String cuerpo) {
                cambiarEstado(Estado.ERROR,
                        "El servidor ha rechazado la medida (" + codigo + ")");
            }
        });
    } // ()

    // ------------------------------------------------------------------------------------
    // estado:Text mensaje:Text --> cambiarEstado() --> void
    // ------------------------------------------------------------------------------------
    private void cambiarEstado(Estado estado, String mensaje) {

        this.elEstadoActual = estado;

        Log.d(ETIQUETA_LOG, " estado = " + estado + " -> " + mensaje);

        this.elObservador.cambioDeEstado(estado, mensaje);
    } // ()

} // class
