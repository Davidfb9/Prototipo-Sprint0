package org.jordi.pruebarest1;

import android.util.Log;

// ---------------------------------------------------------------------------------------
// Fichero:  EscannerBeaconFake.java
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10
//
// Descripción: versión SIMULADA del contrato EscannerBeacon.
//
//              No usa Bluetooth. Devuelve de golpe varias medidas inventadas,
//              que es lo que la placa Arduino habría emitido. Permite probar
//              toda la cadena sin tener la placa a mano.
//
//              En la demo real se usa EscannerBeaconBTLE. Aquí se cambia en un
//              solo punto y ninguna otra clase se modifica.
//
// Diseño: doc/android_design.md
// ---------------------------------------------------------------------------------------
public class EscannerBeaconFake implements EscannerBeacon {

    private static final String ETIQUETA_LOG = ">>>>";

    // identificadores de medicion, igual que en Publicador.h
    private static final int MEDICION_CO2 = 11;
    private static final int MEDICION_TEMPERATURA = 12;

    // ------------------------------------------------------------------------------------
    // nombreBuscado:Texto cb:Deteccion --> empezarABuscar() --> void
    // ------------------------------------------------------------------------------------
    @Override
    public void empezarABuscar(String nombreBuscado, Deteccion cb) {

        Log.d(ETIQUETA_LOG, " [FAKE] empezarABuscar(): " + nombreBuscado);

        // la medida ficticia que también emite el Arduino
        cb.beaconDetectado(nombreBuscado, MEDICION_CO2, 123, 1);
    } // ()

    // ------------------------------------------------------------------------------------
    // --> detenerBusqueda() --> void
    // ------------------------------------------------------------------------------------
    @Override
    public void detenerBusqueda() {
        Log.d(ETIQUETA_LOG, " [FAKE] detenerBusqueda()");
    } // ()

} // class
