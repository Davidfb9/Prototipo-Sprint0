package org.jordi.pruebarest1;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// ---------------------------------------------------------------------------------------
// Fichero:  MainActivity.java
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10
// Copyright:   (c) 2026 David Ferra
// Aportación:  Pantalla principal: permisos, interfaz y arranque del servicio de escaneo.
//
// Descripción: pantalla principal. SÓLO se ocupa de la parte visual y de arrancar
//              el servicio. No contiene lógica de negocio.
//
//              El escaneo vive en ServicioEscaneoBeacon (servicio de primer plano),
//              que es el dueño de LogicaTelefono. Aquí sólo se le pide que empiece,
//              que pare, y se escuchan sus estados para pintarlos.
//
//              Así, cuando el usuario pone la app en segundo plano o gira la
//              pantalla, el escaneo SIGUE vivo.
//
// Diseño: doc/android_design.md
// ---------------------------------------------------------------------------------------
public class MainActivity extends AppCompatActivity implements LogicaTelefono.Observador {

    private static final String ETIQUETA_LOG = ">>>>>";
    private static final int CODIGO_PETICION_PERMISOS = 11223344;

    private TextView textoEstado = null;

    // ------------------------------------------------------------------------------------
    // Enganche con el servicio de primer plano.
    // ------------------------------------------------------------------------------------
    private ServicioEscaneoBeacon elServicio = null;

    // hay que distinguir "la peticion de enlace esta lanzada" de "el enlace ha
    // terminado": bindService() es asíncrono. Sin esto, pulsar EMPEZAR dos veces
    // lanzaba dos enlaces y hacían falta dos desenlaces para cerrarlos.
    private boolean elEnlaceLanzado = false;
    private boolean elServicioEnganchado = false;

    private final ServiceConnection laConexionAlServicio = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName nombre, IBinder binder) {

            elServicio = ((ServicioEscaneoBeacon.LocalBinder) binder).getService();

            // a partir de aquí la actividad escucha los estados del servicio
            elServicio.setObservador(MainActivity.this);

            elServicioEnganchado = true;

            Log.d(ETIQUETA_LOG, " onServiceConnected(): actividad enganchada al servicio");

            textoEstado.setText("Servicio enganchado. Escaneando el beacon GTI-David ...");
        }

        @Override
        public void onServiceDisconnected(ComponentName nombre) {
            elServicio = null;
            elServicioEnganchado = false;
            Log.d(ETIQUETA_LOG, " onServiceDisconnected(): se ha perdido el servicio");
        }
    };

    // ---------------------------------------------------------------------------------------
    // entrada:Bundle? --> onCreate() -->
    // ---------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.textoEstado = (TextView) findViewById(R.id.textoEstado);

        Log.d(ETIQUETA_LOG, " onCreate(): empieza");

        this.textoEstado.setText("Pulsa EMPEZAR para escanear el beacon");

        pedirPermisosQueFalten();

        Log.d(ETIQUETA_LOG, " onCreate(): termina");
    } // ()

    // ---------------------------------------------------------------------------------------
    // ATENCION: aquí NO se para el servicio. Ese es justamente el objetivo: que el
    //           escaneo siga vivo aunque la actividad desaparezca.
    // ---------------------------------------------------------------------------------------
    @Override
    protected void onDestroy() {

        if (this.elEnlaceLanzado) {
            unbindService(this.laConexionAlServicio);
            this.elEnlaceLanzado = false;
            this.elServicioEnganchado = false;
            Log.d(ETIQUETA_LOG, " onDestroy(): actividad desenganchada (el servicio sigue)");
        }

        super.onDestroy();
    } // ()

    // ---------------------------------------------------------------------------------------
    // v:Text --> botonBuscarNuestroDispositivoBTLEPulsado() -->
    //
    // Arranca el servicio de primer plano y se engancha a él.
    // ---------------------------------------------------------------------------------------
    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {

        Log.d(ETIQUETA_LOG, " boton EMPEZAR pulsado");

        String[] faltan = permisosQueFaltan();

        if (faltan.length > 0) {
            this.textoEstado.setText("Faltan permisos. Concédelos para poder escanear.");
            ActivityCompat.requestPermissions(this, faltan, CODIGO_PETICION_PERMISOS);
            return;
        }

        // ---------------------------------------------------------------------------
        // además del permiso, el interruptor de localización del teléfono tiene
        // que estar ENCENDIDO, si no Android devuelve cero resultados de escaneo
        // sin dar ningún error. Es el fallo más difícil de ver.
        // ---------------------------------------------------------------------------
        if (!estaLaLocalizacionEncendida()) {
            Log.d(ETIQUETA_LOG, " boton EMPEZAR: la localizacion esta APAGADA");
            this.textoEstado.setText(
                    "La localización del teléfono está APAGADA.\n"
                            + "Sin ella Android no devuelve ningún resultado de escaneo BLE.\n"
                            + "Enciéndela en Ajustes y vuelve a pulsar EMPEZAR.");
            return;
        }

        arrancarServicio();
    } // ()

    // ---------------------------------------------------------------------------------------
    // v:Text --> botonDetenerBusquedaDispositivosBTLEPulsado() -->
    //
    // Pide al servicio que se detenga él solo. Mientras el servicio viva, el
    // escaneo sigue en segundo plano.
    // ---------------------------------------------------------------------------------------
    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {

        Log.d(ETIQUETA_LOG, " boton PARAR pulsado");

        if (this.elServicio != null) {

            // el servicio es el dueño del escáner: él se encarga de pararlo y de
            // retirar la notificación de primer plano
            this.elServicio.pararEscaneo();

        } else {

            // por si se pulsa PARAR antes de que bindService() haya terminado
            // (es asíncrono): entonces basta con pedir la parada por Intent
            stopService(new Intent(this, ServicioEscaneoBeacon.class));
        }

        this.textoEstado.setText("Escaneo detenido.");
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> arrancarServicio() -->
    // ---------------------------------------------------------------------------------------
    private void arrancarServicio() {

        Intent i = new Intent(this, ServicioEscaneoBeacon.class);

        // desde Android 8 hay que usar startForegroundService() y el servicio debe
        // llamar a startForeground() enseguida. Esto además re-arranca el escaneo
        // si el servicio ya existía parado (vuelve a llamar a onStartCommand()).
        ContextCompat.startForegroundService(this, i);

        // BIND_AUTO_CREATE: si el servicio no existe, Android lo crea.
        // ATENCION: sólo UNA vez; bindService() es asíncrono y volver a llamar
        //           antes de que acabe enlazaría dos veces.
        if (!this.elEnlaceLanzado) {
            bindService(i, this.laConexionAlServicio, Context.BIND_AUTO_CREATE);
            this.elEnlaceLanzado = true;
        }

        this.textoEstado.setText("Arrancando el servicio de escaneo ...");

        Log.d(ETIQUETA_LOG, " arrancarServicio(): servicio lanzado");
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> pedirPermisosQueFalten() -->
    // ---------------------------------------------------------------------------------------
    private void pedirPermisosQueFalten() {

        String[] faltan = permisosQueFaltan();

        if (faltan.length == 0) {
            Log.d(ETIQUETA_LOG, " pedirPermisosQueFalten(): ya están todos");
            return;
        }

        ActivityCompat.requestPermissions(this, faltan, CODIGO_PETICION_PERMISOS);
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> permisosQueFaltan() --> [Text]
    //
    // Los permisos de Bluetooth cambian según la versión de Android:
    //
    //   - Android 12 (API 31) en adelante: BLUETOOTH_SCAN + BLUETOOTH_CONNECT
    //   - Android 11 (API 30) y anteriores: BLUETOOTH + BLUETOOTH_ADMIN
    //
    // ATENCION: ACCESS_FINE_LOcation hace falta en TODAS las versiones. Es la
    //           contrapartida de no usar la bandera neverForLocation: como no se
    //           afirma que el BLE no sirva para localizar, Android exige el
    //           permiso de localización. Sin él el escáner se registra bien
    //           (status=0) pero devuelve CERO resultados, en silencio.
    // ---------------------------------------------------------------------------------------
    private String[] permisosQueFaltan() {

        List<String> pedidos = new ArrayList<String>();

        // localización: necesaria siempre para escanear BLE
        pedidos.add(Manifest.permission.ACCESS_FINE_LOCATION);
        pedidos.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            pedidos.add(Manifest.permission.BLUETOOTH_SCAN);
            pedidos.add(Manifest.permission.BLUETOOTH_CONNECT);

        } else {

            pedidos.add(Manifest.permission.BLUETOOTH);
            pedidos.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        // desde Android 13 hace falta además pedir permiso para que se vea la
        // notificación del servicio de primer plano
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedidos.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> faltan = new ArrayList<String>();

        for (String p : pedidos) {
            if (ContextCompat.checkSelfPermission(this, p)
                    != PackageManager.PERMISSION_GRANTED) {
                faltan.add(p);
            }
        }

        return faltan.toArray(new String[0]);
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> estaLaLocalizacionEncendida() --> B
    //
    // Además del PERMISO de localización, el interruptor de localización del
    // teléfono (GPS) tiene que estar ENCENDIDO. Si está apagado, Android devuelve
    // cero resultados de escaneo BLE sin dar ningún error.
    //
    // Es el fallo más difícil de ver, porque la app no se queja de nada.
    // ---------------------------------------------------------------------------------------
    private boolean estaLaLocalizacionEncendida() {

        LocationManager gestor = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (gestor == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return gestor.isLocationEnabled();
        }

        return gestor.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || gestor.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    } // ()

    // ---------------------------------------------------------------------------------------
    // estado:Text mensaje:Text --> cambioDeEstado() --> void
    //
    // Es la única función que pinta. Recibe el estado que decide LogicaTelefono,
    // reenviado por el servicio.
    // ---------------------------------------------------------------------------------------
    @Override
    public void cambioDeEstado(final LogicaTelefono.Estado estado, final String mensaje) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textoEstado.setText(
                        String.format(Locale.getDefault(), "[%s]\n%s", estado.name(), mensaje));
            }
        });
    } // ()

    // ---------------------------------------------------------------------------------------
    // requestCode:N permisos:[Text] resultados:[N] --> onRequestPermissionsResult() -->
    // ---------------------------------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != CODIGO_PETICION_PERMISOS) {
            return;
        }

        String[] todaviaFaltan = permisosQueFaltan();

        if (todaviaFaltan.length > 0) {

            Log.d(ETIQUETA_LOG, " onRequestPermissionsResult(): siguen faltando permisos");

            this.textoEstado.setText("Faltan permisos: " + String.join(", ", todaviaFaltan)
                    + "\nConcédelos en Ajustes del teléfono.");
            return;
        }

        Log.d(ETIQUETA_LOG, " onRequestPermissionsResult(): todos los permisos concedidos");

        if (!estaLaLocalizacionEncendida()) {

            this.textoEstado.setText(
                    "Permisos concedidos, pero la localización del teléfono\n"
                            + "está APAGADA. Android no escanea BLE sin ella.\n"
                            + "Enciéndela y pulsa EMPEZAR.");

        } else {

            this.textoEstado.setText("Permisos concedidos. Pulsa EMPEZAR para escanear.");
        }
    } // ()

} // class
