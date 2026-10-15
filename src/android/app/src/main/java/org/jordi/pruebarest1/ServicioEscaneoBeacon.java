package org.jordi.pruebarest1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

// ---------------------------------------------------------------------------------------
// Fichero:  ServicioEscaneoBeacon.java
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10
//
// Descripción: SERVICIO DE PRIMER PLANO (foreground service).
//
//              Mantiene el escaneo del beacon vivo aunque el usuario saque la app
//              a segundo plano o gire la pantalla. Android mata con facilidad los
//              procesos de segundo plano, pero NO mata a un servicio de primer
//              plano mientras su notificación esté visible.
//
//              El servicio es el dueño de la lógica de negocio (LogicaTelefono).
//              La actividad solo se engancha con el binder para pintar el estado.
//
// Diseño: doc/android_design.md
// ---------------------------------------------------------------------------------------
public class ServicioEscaneoBeacon extends Service implements LogicaTelefono.Observador {

    private static final String ETIQUETA_LOG = ">>>>>";

    private static final String ID_CANAL = "canal_escaneo_beacon";
    private static final String NOMBRE_CANAL = "Escaneo de beacon";
    private static final int ID_NOTIFICACION = 1001;

    // el nombre de la emisora tiene que coincidir con Publicador.h
    public static final String NOMBRE_EMISORA_BUSCADA = "GTI-David";

    private final IBinder elBinder = new LocalBinder();

    private LogicaTelefono laLogica = null;
    private BluetoothAdapter elAdaptadorBluetooth = null;

    // la actividad se registra aquí para reflejar el estado en pantalla
    private LogicaTelefono.Observador elObservadorExterno = null;

    // ------------------------------------------------------------------------------------
    // Binder local: la actividad lo usa para engancharse y recibir los estados.
    // ------------------------------------------------------------------------------------
    public class LocalBinder extends Binder {

        public ServicioEscaneoBeacon getService() {
            return ServicioEscaneoBeacon.this;
        }
    } // class

    // ---------------------------------------------------------------------------------------
    // --> onCreate() -->
    //
    // Se ejecuta UNA sola vez, cuando Android crea el servicio.
    // ---------------------------------------------------------------------------------------
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(ETIQUETA_LOG, " SERVICIO onCreate(): empieza");

        crearCanalDeNotificacion();

        inicializarBluetooth();

        // aquí se montan las piezas, igual que hacía la actividad
        this.laLogica = new LogicaTelefono(
                this,              // el servicio observa y luego lo reenvía a la actividad
                crearEscanner(),
                crearClienteREST()
        );

        Log.d(ETIQUETA_LOG, " SERVICIO onCreate(): termina");
    } // ()

    // ---------------------------------------------------------------------------------------
    // intent:Texto flags:N startId:N --> onStartCommand() --> N
    //
    // Se ejecuta cada vez que alguien llama a startService()/startForegroundService().
    // ---------------------------------------------------------------------------------------
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(ETIQUETA_LOG, " SERVICIO onStartCommand(): empieza");

        // esto hay que hacerlo ANTES de empezar a trabajar, si no Android aborta
        empezarComoServicioDePrimerPlano(
                "Escaneando el beacon " + NOMBRE_EMISORA_BUSCADA + " ...");

        if (this.laLogica != null) {
            this.laLogica.buscarBeacon(NOMBRE_EMISORA_BUSCADA);
        }

        // START_STICKY: si Android mata el proceso, lo vuelve a crear solo
        return START_STICKY;
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> onDestroy() -->
    //
    // Android mata el servicio o alguien llama a stopSelf()/stopService().
    // ---------------------------------------------------------------------------------------
    @Override
    public void onDestroy() {
        Log.d(ETIQUETA_LOG, " SERVICIO onDestroy(): empieza");

        if (this.laLogica != null) {
            this.laLogica.detenerBusqueda();
        }

        super.onDestroy();
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> onBind() --> IBinder
    // ---------------------------------------------------------------------------------------
    @Override
    public IBinder onBind(Intent intent) {
        return this.elBinder;
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> onUnbind() --> VoF
    //
    // La actividad se ha ido (por ejemplo, han cerrado la app). El escaneo SIGUE,
    // que es justo para lo que está el servicio.
    // ---------------------------------------------------------------------------------------
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(ETIQUETA_LOG, " SERVICIO onUnbind(): la actividad se ha desenganchado");
        this.elObservadorExterno = null;
        return true;
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> onRebind() --> void
    //
    // Una actividad se vuelve a enganchar.
    // ---------------------------------------------------------------------------------------
    @Override
    public void onRebind(Intent intent) {
        Log.d(ETIQUETA_LOG, " SERVICIO onRebind(): una actividad se ha enganchado");
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> pararEscaneo() --> void
    //
    // Lo llama la actividad al pulsar PARAR.
    //
    // ATENCION: aquí NO basta con stopSelf().
    //
    //           stopSelf() pide destruir el servicio, pero un servicio QUE ESTÁ
    //           ENLAZADO (bindService) no se destruye hasta que todos sus
    //           clientes se desenlacen. Como la actividad sigue enlazada,
    //           onDestroy() — el único sitio donde se llamaba a
    //           detenerBusqueda() — no se ejecutaba NUNCA y el escaneo BLE
    //           seguía corriendo y la notificación seguía ahí.
    //
    //           Por eso el escaneo se para AQUÍ, de forma explícita, y luego
    //           se retira la notificación de primer plano y se pide la parada.
    // ---------------------------------------------------------------------------------------
    public void pararEscaneo() {
        Log.d(ETIQUETA_LOG, " SERVICIO pararEscaneo()");

        // 1. parar el escaneo BLE de verdad
        if (this.laLogica != null) {
            this.laLogica.detenerBusqueda();
        }

        // 2. retirar la notificación de primer plano (STOP_FOREGROUND_REMOVE la
        //    borra; sin ella el servicio se quedaría comoForeground sin motivo)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);

        // 3. pedir la destrucción: surtirá efecto cuando la actividad se desenlace
        stopSelf();
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> estaEscaneando() --> VoF
    // ---------------------------------------------------------------------------------------
    public boolean estaEscaneando() {
        return this.laLogica != null
                && this.laLogica.getEstadoActual() == LogicaTelefono.Estado.ESCANEANDO;
    } // ()

    // ---------------------------------------------------------------------------------------
    // o:Texto --> setObservador() --> void
    //
    // La actividad se registra para recibir los cambios de estado.
    // ---------------------------------------------------------------------------------------
    public void setObservador(LogicaTelefono.Observador o) {
        this.elObservadorExterno = o;
    } // ()

    // ---------------------------------------------------------------------------------------
    // estado:Texto mensaje:Texto --> cambioDeEstado() --> void
    //
    // Implementación del contrato Observador. El servicio reparte el estado a la
    // notificación y a la actividad, si está enganchada.
    // ---------------------------------------------------------------------------------------
    @Override
    public void cambioDeEstado(final LogicaTelefono.Estado estado, final String mensaje) {

        Log.d(ETIQUETA_LOG, " SERVICIO cambioDeEstado(): " + estado + " -> " + mensaje);

        actualizarNotificacion(mensaje);

        if (this.elObservadorExterno != null) {
            this.elObservadorExterno.cambioDeEstado(estado, mensaje);
        }
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> crearCanalDeNotificacion() -->
    //
    // Obligatorio desde Android 8 (API 26): sin canal no se ve ninguna notificación.
    // ---------------------------------------------------------------------------------------
    private void crearCanalDeNotificacion() {

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return; // en versiones antiguas no hacen falta canales
        }

        NotificationChannel canal = new NotificationChannel(
                ID_CANAL,
                NOMBRE_CANAL,
                NotificationManager.IMPORTANCE_LOW); // sin sonido ni vibración

        canal.setDescription("Mantiene el escaneo del beacon activo en segundo plano");
        canal.setShowBadge(false);

        NotificationManager gestor = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (gestor != null) {
            gestor.createNotificationChannel(canal);
        }
    } // ()

    // ---------------------------------------------------------------------------------------
    // texto:Texto --> empezarComoServicioDePrimerPlano() -->
    //
    // Hay que llamar a startForeground() muy pronto tras onStartCommand(), si no
    // Android lanza una excepción y mata la app.
    // ---------------------------------------------------------------------------------------
    private void empezarComoServicioDePrimerPlano(String texto) {

        Notification notificacion = construirNotificacion(texto);

        try {
            ServiceCompat.startForeground(
                    this,
                    ID_NOTIFICACION,
                    notificacion,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } catch (Exception e) {
            // por si el sistema no admite el tipo connectedDevice
            Log.d(ETIQUETA_LOG, " SERVICIO startForeground(): reintentando sin tipo -> " + e);
            startForeground(ID_NOTIFICACION, notificacion);
        }
    } // ()

    // ---------------------------------------------------------------------------------------
    // texto:Texto --> actualizarNotificacion() -->
    // ---------------------------------------------------------------------------------------
    private void actualizarNotificacion(String texto) {

        NotificationManager gestor = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (gestor != null) {
            gestor.notify(ID_NOTIFICACION, construirNotificacion(texto));
        }
    } // ()

    // ---------------------------------------------------------------------------------------
    // texto:Texto --> construirNotificacion() -> Notification
    // ---------------------------------------------------------------------------------------
    private Notification construirNotificacion(String texto) {

        return new NotificationCompat.Builder(this, ID_CANAL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Sprint 0 - Beacon")
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setOngoing(true)                  // no se puede descartar con el dedo
                .setOnlyAlertOnce(true)            // solo avisa la primera vez
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> inicializarBluetooth() -->
    // ---------------------------------------------------------------------------------------
    private void inicializarBluetooth() {
        this.elAdaptadorBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (this.elAdaptadorBluetooth == null) {
            Log.d(ETIQUETA_LOG, " SERVICIO inicializarBluetooth(): el telefono no tiene BTLE");
        }
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> crearEscanner() -> EscannerBeacon
    // ---------------------------------------------------------------------------------------
    private EscannerBeacon crearEscanner() {

        if (this.elAdaptadorBluetooth == null) {
            Log.d(ETIQUETA_LOG, " SERVICIO crearEscanner(): no hay adaptador BTLE");
            return new EscannerBeaconFake();
        }

        BluetoothLeScanner elEscanner = this.elAdaptadorBluetooth.getBluetoothLeScanner();

        if (elEscanner == null) {
            Log.d(ETIQUETA_LOG, " SERVICIO crearEscanner(): no se ha obtenido el escaner");
            return new EscannerBeaconFake();
        }

        // el servicio es un Context, no una actividad
        return new EscannerBeaconBTLE(this, elEscanner);
    } // ()

    // ---------------------------------------------------------------------------------------
    // --> crearClienteREST() -> ClienteREST
    // ---------------------------------------------------------------------------------------
    private ClienteREST crearClienteREST() {
        return new ClienteRESTHttp();
    } // ()

} // class
