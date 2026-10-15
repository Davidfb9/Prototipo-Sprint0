package org.jordi.pruebarest1;

// ---------------------------------------------------------------------------------------
// Fichero:  EscannerBeacon.java
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10
//
// Descripción: contrato que debe cumplir cualquier buscador de beacons del teléfono.
//
//              La lógica de negocio del teléfono sólo conoce este contrato, por lo
//              que puede funcionar con Bluetooth LE real (EscannerBeaconBTLE) o
//              con un escáner simulado (EscannerBeaconFake).
//
// Diseño: doc/android_design.md
// ---------------------------------------------------------------------------------------
public interface EscannerBeacon {

    // ------------------------------------------------------------------------------------
    // Notificación de la detección.
    // ------------------------------------------------------------------------------------
    interface Deteccion {

        // ya está anunciando el beacon buscado
        // nombre:Texto tipoDeMedicion:N valor:Z contador:N --> beaconDetectado() -->
        void beaconDetectado(String nombre, int tipoDeMedicion, int valor, int contador);

        // algo ha ido mal (no hay Bluetooth, falta permiso...)
        // mensaje:Texto --> error() -->
        void error(String mensaje);
    }

    // ------------------------------------------------------------------------------------
    // nombreBuscado:Texto cb:Deteccion --> empezarABuscar() --> void
    // ------------------------------------------------------------------------------------
    void empezarABuscar(String nombreBuscado, Deteccion cb);

    // ------------------------------------------------------------------------------------
    // --> detenerBusqueda() --> void
    // ------------------------------------------------------------------------------------
    void detenerBusqueda();
}
