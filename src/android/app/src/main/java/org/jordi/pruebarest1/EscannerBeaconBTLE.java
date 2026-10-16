package org.jordi.pruebarest1;

import android.Manifest;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

// ---------------------------------------------------------------------------------------
// Fichero:  EscannerBeaconBTLE.java
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10
// Copyright:   (c) 2026 David Ferra
// Aportación:  Implementacion real del escaneo BTLE, con filtrado por nombre de beacon.
//
// Descripción: implementación REAL del contrato EscannerBeacon.
//
//              Busca por Bluetooth LE un beacon con el nombre indicado y decodifica
//              su trama iBeacon. Es el ÚNICO fichero que conoce las clases de
//              Bluetooth LE de Android.
//
//              IMPORTANTE: el Arduino emite TRES iBeacon con el mismo nombre
//              ("GTI-David"): CO2, TEMPERATURA y un beacon libre con la cadena
//              "MolaMola...". Los tres se distinguen por el UUID, así que aquí se
//              filtra por UUID y sólo se avisa del beacon de medidas.
//
//              Además se filtra por NOMBRE, y no es un detalle cosmético: en el
//              aula todos los Arduino llevan la plantilla compartida, así que todos
//              emiten el mismo UUID. El nombre es lo único que distingue "GTI-David"
//              del beacon de un compañero.
//
//              Ninguno de los dos filtros se delega a un ScanFilter de Android:
//              el nombre viaja en la respuesta de escaneo y los móviles con MIUI
//              lo resuelven mal en el firmware y no devuelven ningún resultado.
//              Se escanea sin filtros y se filtra todo aquí, en tratarResultado().
//
// Diseño: doc/android_design.md
// ---------------------------------------------------------------------------------------
public class EscannerBeaconBTLE implements EscannerBeacon {

    private static final String ETIQUETA_LOG = ">>>>>";

    // ------------------------------------------------------------------------------------
    // UUID del beacon de medidas. Tiene que coincidir con Publicador.h:
    //
    //     uint8_t beaconUUID[16] = { 'E','P','S','G','-','G','T','I',
    //                                 '-','P','R','O','Y','-','3','A' };
    //
    // El beacon libre que emite el Arduino usa "MolaMolaMolaMola" como UUID, así que
    // con este filtro queda descartado automáticamente.
    // ------------------------------------------------------------------------------------
    private static final String UUID_EMISORA_ESPERADO = "EPSG-GTI-PROY-3A";

    // ATENCION: se usa Context y no AppCompatActivity porque este componente lo
    //           instancia el SERVICIO en segundo plano, que no es una actividad.
    //           Context basta para comprobar los permisos.
    private final Context elContexto;
    private final BluetoothLeScanner elEscanner;

    private ScanCallback callbackDelEscaneo = null;

    // ------------------------------------------------------------------------------------
    // El escaneo sin filtros ve TODOS los Bluetooth LE del entorno: en el aula son
    // decenas y cada uno se anuncia unas 15 veces por segundo. Si se avisara de cada
    // paquete descartado, el logcat se llenaría a razón de cientos de líneas por
    // segundo y no se vería nada.
    //
    // Por eso los descartes se avisAN, pero como mucho una vez cada
    // INTERVALO_AVISO_MS. Los beacons que SÍ interesan se avisan SIEMPRE.
    // ------------------------------------------------------------------------------------
    private static final long INTERVALO_AVISO_MS = 3000;
    private long ultimaVezQueSeAvisoMs = 0;
    private int descartesNoAvisados = 0;

    // ------------------------------------------------------------------------------------
    // contexto:Text elEscanner:Text --> EscannerBeaconBTLE() -->
    // ------------------------------------------------------------------------------------
    public EscannerBeaconBTLE(Context contexto, BluetoothLeScanner escanner) {
        this.elContexto = contexto;
        this.elEscanner = escanner;
    } // ()

    // ------------------------------------------------------------------------------------
    // nombreBuscado:Text cb:Deteccion --> empezarABuscar() --> void
    // ------------------------------------------------------------------------------------
    @Override
    public void empezarABuscar(final String nombreBuscado, final Deteccion cb) {

        Log.d(ETIQUETA_LOG, " empezarABuscar(): " + nombreBuscado);

        if (this.elEscanner == null) {
            cb.error("No se ha obtenido el escaner Bluetooth LE");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this.elContexto,
                Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            cb.error("Falta el permiso de Bluetooth. Concedelo en Ajustes.");
            return;
        }

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                tratarResultado(nombreBuscado, resultado, cb);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);

                // en modo de bajo consumo Android agrupa los resultados
                Log.v(ETIQUETA_LOG, " onBatchScanResults(): recibe " + results.size());

                for (ScanResult r : results) {
                    tratarResultado(nombreBuscado, r, cb);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, " onScanFailed(): codigo = " + errorCode);
                cb.error("El escaneo ha fallado, codigo " + errorCode);
            }
        };

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        // -----------------------------------------------------------------------
        // ATENCION: NO se pone aqui un ScanFilter.setDeviceName(nombreBuscado).
        //
        // El nombre "GTI-David" viaja en la RESPUESTA de escaneo, no en el paquete
        // de anuncio, y el filtro de nombre de Android lo resuelve el firmware en
        // muchos moviles (Xiaomi/MIUI incluido), que lo aplican mal sobre la
        // respuesta de escaneo y no devuelven NINGUN resultado.
        //
        // Se lanza el escaneo sin filtros (lista vacia) y el filtro REAL se aplica
        // aqui mismo, en tratarResultado(), por nombre y por UUID. Es lo que hace
        // la app de referencia que funciona (ProyectoAndreu).
        //
        // El filtro por nombre es NECESARIO, no solo el de UUID: en el aula todos
        // los Arduino llevan la misma plantilla y por tanto el MISMO UUID
        // ("EPSG-GTI-PROY-3A"); sin el nombre, la app cazaría los beacons de los
        // compañeros.
        // -----------------------------------------------------------------------
        ArrayList<ScanFilter> filtros = new ArrayList<ScanFilter>();

        this.elEscanner.startScan(filtros, settings, this.callbackDelEscaneo);

        Log.d(ETIQUETA_LOG, " empezarABuscar(): escaneo lanzado");
    } // ()

    // ------------------------------------------------------------------------------------
    // --> detenerBusqueda() --> void
    // ------------------------------------------------------------------------------------
    @Override
    public void detenerBusqueda() {

        if (this.callbackDelEscaneo == null) {
            Log.d(ETIQUETA_LOG, " detenerBusqueda(): no habia escaneo activo");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this.elContexto,
                Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, " detenerBusqueda(): no tengo el permiso BLUETOOTH_SCAN");
            return;
        }

        this.elEscanner.stopScan(this.callbackDelEscaneo);
        this.callbackDelEscaneo = null;

        Log.d(ETIQUETA_LOG, " detenerBusqueda(): escaneo detenido");
    } // ()

    // ------------------------------------------------------------------------------------
    // motivo:Text --> avisarDescarte() --> void
    //
    // Avisa de un descarte, pero sin saturar el logcat: máximo un aviso cada
    // INTERVALO_AVISO_MS. Lo que se suprime entre aviso y aviso se acumula en
    // descartesNoAvisados y se cuenta en el siguiente, para no perder la
    // información de que estaba pasando.
    // ------------------------------------------------------------------------------------
    private void avisarDescarte(String motivo) {

        long ahora = System.currentTimeMillis();

        if ((ahora - this.ultimaVezQueSeAvisoMs) < INTERVALO_AVISO_MS) {
            this.descartesNoAvisados++;
            return;
        }

        String extra = "";

        if (this.descartesNoAvisados > 0) {
            extra = " (+" + this.descartesNoAvisados + " descartes desde el aviso anterior)";
            this.descartesNoAvisados = 0;
        }

        this.ultimaVezQueSeAvisoMs = ahora;

        Log.d(ETIQUETA_LOG, " tratarResultado(): " + motivo + extra);
    } // ()

    // ------------------------------------------------------------------------------------
    // nombreBuscado:Text resultado:ScanResult cb:Deteccion --> tratarResultado() --> void
    // ------------------------------------------------------------------------------------
    private void tratarResultado(String nombreBuscado, ScanResult resultado, Deteccion cb) {

        // ---------------------------------------------------------------------------
        // getScanRecord() puede ser null si Android aún no ha completado el anuncio.
        // Antes esto no se miraba y la app se caía con un NullPointerException.
        // ---------------------------------------------------------------------------
        if (resultado.getScanRecord() == null) {
            return;
        }

        byte[] bytes = resultado.getScanRecord().getBytes();

        if (bytes == null || bytes.length < 30) {
            // no es una trama iBeacon completa (un anuncio normal tiene menos bytes)
            return;
        }

        // antes se volcaba aquí el hex completo de cada paquete; con el escaneo sin
        // filtros eso son cientos de líneas por segundo, así que ya no se hace
        Log.v(ETIQUETA_LOG, " tratarResultado(): bytes = " + Utilidades.bytesToHexString(bytes));

        TramaIBeacon tib = new TramaIBeacon(bytes);

        // ---------------------------------------------------------------------------
        // getDevice().getName() da null a menudo porque el nombre viene en la
        // respuesta de escaneo. getScanRecord().getDeviceName() lo lee del anuncio,
        // que es como lo hace nRF Connect ("Complete Local Name").
        // ---------------------------------------------------------------------------
        String nombre = resultado.getScanRecord().getDeviceName();

        if (nombre == null) {
            nombre = resultado.getDevice().getName();
        }

        if (nombre == null) {
            nombre = "(sin nombre)";
        }

        // ---------------------------------------------------------------------------
        // 1) tiene que ser iBeacon de verdad (Apple 0x004C, tipo 0x02, longitud 21)
        // ---------------------------------------------------------------------------
        if (!tib.esIBeacon()) {
            avisarDescarte("anuncio descartado, no es iBeacon");
            return;
        }

        // ---------------------------------------------------------------------------
        // 2) tiene que ser NUESTRO beacon y no el de un compañero.
        //
        //    En el aula TODOS los Arduino llevan la misma plantilla, asi que TODOS
        //    emiten el MISMO UUID ("EPSG-GTI-PROY-3A"). Por eso el filtro por
        //    nombre no es un extra: es lo único que distingue "GTI-David" de
        //    "Andreutale" o del beacon de quien sea.
        //
        //    Este filtro se aplica AQUI y no con un ScanFilter de Android porque
        //    el nombre viaja en la respuesta de escaneo y los moviles con MIUI lo
        //    resuelven mal en el firmware.
        // ---------------------------------------------------------------------------
        if (nombreBuscado != null && !nombreBuscado.equals(nombre)) {
            avisarDescarte("descartado, nombre = [" + nombre + "]");
            return;
        }

        // ---------------------------------------------------------------------------
        // 3) además, el Arduino emite un beacon libre con el UUID "MolaMolaMolaMola"
        //    y el mismo nombre: si no se filtra por UUID, la app lo caza y falla al
        //    interpretar el major (0x4D6F = 77).
        // ---------------------------------------------------------------------------
        String uuidRecibido = tib.getUUIDComoTexto();

        if (!UUID_EMISORA_ESPERADO.equals(uuidRecibido)) {
            avisarDescarte("descartado, UUID = [" + uuidRecibido + "]");
            return;
        }

        // ---------------------------------------------------------------------------
        // el Arduino codifica asi la medicion (ver Publicador.h):
        //     major = (identificadorDeMedicion << 8) + contador
        //     minor = valor medido
        // ---------------------------------------------------------------------------
        int major = Utilidades.bytesToInt(tib.getMajor());
        int minor = Utilidades.bytesToInt(tib.getMinor());

        int identificadorDeMedicion = (major >> 8) & 0xFF;
        int contador = major & 0xFF;
        int valorMedido = minor;

        Log.d(ETIQUETA_LOG, " tratarResultado(): nombre=" + nombre
                + " tipo=" + identificadorDeMedicion
                + " contador=" + contador + " valor=" + valorMedido);

        cb.beaconDetectado(nombre, identificadorDeMedicion, valorMedido, contador);
    } // ()

} // class
