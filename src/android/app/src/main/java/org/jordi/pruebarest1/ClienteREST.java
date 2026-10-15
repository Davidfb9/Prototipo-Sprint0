package org.jordi.pruebarest1;

// ---------------------------------------------------------------------------------------
// Fichero:  ClienteREST.java
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10
//
// Descripción: contrato que debe cumplir cualquier cliente REST del teléfono.
//
//              La lógica de negocio del teléfono (LogicaTelefono) NO sabe cómo se
//              hace la petición. Sólo conoce esta interfaz. Por eso puede
//              funcionar contra el servidor de verdad (ClienteRESTHttp) o contra
//              una versión simulada (ClienteRESTFake) sin cambiar una sola línea
//              de lógica.
//
// Diseño: doc/android_design.md
// ---------------------------------------------------------------------------------------
public interface ClienteREST {

    // ------------------------------------------------------------------------------------
    // Contrato de la respuesta: el servidor ha contestado.
    // ------------------------------------------------------------------------------------
    interface Respuesta {

        // el servidor ha devuelto 200 y un cuerpo correcto
        // codigo:N cuerpo:Texto --> ok() -->
        void ok(String cuerpo);

        // el servidor ha devuelto un error (400, 404, 405, 500...)
        // codigo:N cuerpo:Texto --> error() -->
        void error(int codigo, String cuerpo);
    }

    // ------------------------------------------------------------------------------------
    // sensor:Texto valor:Z contador:N cb:Funcion --> guardarMedida() --> void
    //
    // La firma de esta función es IDÉNTICA a la de la lógica de negocio del
    // servidor:
    //
    //     sensor:Texto, valor:Z, contador:N -> guardarMedida() -> VoF
    //
    // Sólo cambia el tipo de la respuesta, porque aquí la respuesta llega de forma
    // asíncrona.
    // ------------------------------------------------------------------------------------
    void guardarMedida(String sensor, int valor, int contador, Respuesta cb);
}
