package org.jordi.pruebarest1;

import android.util.Log;

// ---------------------------------------------------------------------------------------
// Fichero:  ClienteRESTFake.java
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10
// Copyright:   (c) 2026 David Ferra
// Aportación:  Implementacion simulada del contrato ClienteREST, sin red.
//
// Descripción: versión SIMULADA del contrato ClienteREST.
//
//              No hace ninguna petición HTTP. Devuelve siempre una respuesta
//              inventada. Sirve para probar toda la lógica del teléfono sin
//              necesidad de tener el servidor Apache encendido.
//
//              Se sustituye por ClienteRESTHttp en un solo punto de la aplicación
//              (donde se crea LogicaTelefono). Ninguna otra clase cambia.
//
// Diseño: doc/android_design.md
// ---------------------------------------------------------------------------------------
public class ClienteRESTFake implements ClienteREST {

    private static final String ETIQUETA_LOG = ">>>>";

    // si se pone a true, el fake simula que el servidor está caído
    private boolean elServidorEstaCaido = false;

    // ------------------------------------------------------------------------------------
    // --> setElServidorEstaCaido() -->
    //
    // Permite probar también el caminho de error.
    // ------------------------------------------------------------------------------------
    public void setElServidorEstaCaido(boolean caido) {
        this.elServidorEstaCaido = caido;
    } // ()

    // ------------------------------------------------------------------------------------
    // sensor:Text valor:Z contador:N cb:Respuesta --> guardarMedida() --> void
    // ------------------------------------------------------------------------------------
    @Override
    public void guardarMedida(String sensor, int valor, int contador, Respuesta cb) {

        Log.d(ETIQUETA_LOG, " [FAKE] guardarMedida(): " + sensor + " = " + valor
                + " (contador " + contador + ")");

        if (this.elServidorEstaCaido) {
            cb.error(500, "{\"error\":\"el servidor esta caido (fake)\"}");
            return;
        }

        cb.ok("{\"resultado\":true}");
    } // ()

} // class
