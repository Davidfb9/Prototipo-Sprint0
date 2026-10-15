package org.jordi.pruebarest1;

import android.util.Log;

// ---------------------------------------------------------------------------------------
// Fichero:  ClienteRESTHttp.java
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10
//
// Descripción: implementación REAL del contrato ClienteREST.
//
//              Hace una petición HTTP de verdad contra el servidor Apache/XAMPP.
//              Es el ÚNICO fichero del teléfono que conoce la dirección del
//              servidor. Si cambia la IP, sólo hay que tocar este fichero.
//
// Diseño: doc/android_design.md
// ---------------------------------------------------------------------------------------
public class ClienteRESTHttp implements ClienteREST {

    // ------------------------------------------------------------------------------------
    // ATENCION: esta es la única dirección que hay que cambiar entre ordenadores.
    //
    //   1. Se mira la IP del ordenador:  ipconfig  ->  IPv4
    //   2. El puerto NO se pone: XAMPP escucha en el 80, que es el predeterminado
    //   3. Hay que añadir la ruta hasta src, porque el proyecto está desplegado
    //      dentro de htdocs/Prototipo
    //
    // Con eso la app llama a
    //
    //      http://<IP>/Prototipo/src/communication/guardarMedida.php
    //
    // ------------------------------------------------------------------------------------
    private static final String URL_BASE_SERVIDOR = "http://172.31.151.124/Prototipo/src";

    private static final String ETIQUETA_LOG = ">>>>";

    // ------------------------------------------------------------------------------------
    // sensor:Texto valor:Z contador:N cb:Respuesta --> guardarMedida() --> void
    // ------------------------------------------------------------------------------------
    @Override
    public void guardarMedida(final String sensor, final int valor, final int contador,
                              final Respuesta cb) {

        final String cuerpo = "{ \"sensor\": \"" + sensor
                + "\", \"valor\": " + valor
                + ", \"contador\": " + contador + " }";

        final String urlDestino = URL_BASE_SERVIDOR + "/communication/guardarMedida.php";

        Log.d(ETIQUETA_LOG, " guardarMedida(): POST " + urlDestino + " cuerpo=" + cuerpo);

        PeticionarioREST elPeticionario = new PeticionarioREST();

        elPeticionario.hacerPeticionREST("POST", urlDestino, cuerpo,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpoRespuesta) {

                        Log.d(ETIQUETA_LOG, " guardarMedida(): respuesta codigo=" + codigo);

                        if (codigo == 200) {
                            cb.ok(cuerpoRespuesta);
                        } else {
                            cb.error(codigo, cuerpoRespuesta);
                        }
                    }
                }
        );
    } // ()

} // class
