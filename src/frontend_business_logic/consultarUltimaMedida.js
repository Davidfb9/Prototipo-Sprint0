// ----------------------------------------------------------------
//
// Fichero:  consultarUltimaMedida.js
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10-15
// Copyright:   (c) 2026 David Ferra
// Aportación:  Logica fake del navegador: invoca la API REST tras la misma firma que el backend.
// Descripción: lógica fake del navegador.
//
//   ESTE FICHERO ES EL ÚNICO DEL NAVEGADOR QUE SABE QUE EXISTE EL
//   SERVIDOR REST. La interfaz gráfica (src/gui/) no hace ninguna
//   petición HTTP: llama a esta función y nada más.
//
//   Para probar la interfaz sin servidor se sustituye esta capa por
//   una versión fake que responda siempre lo mismo. Por eso está
//   separada en su propio fichero.
//
// Diseño: doc/frontend_business_logic_design.md
//
// ----------------------------------------------------------------

// ----------------------------------------------------------------
//
// cb:Funcion --> consultarUltimaMedida() --> void
//
// Realiza un GET a la ruta REST y entrega el resultado al
// callback. El callback recibe:
//
//   cb(null, medida)              si hay medida
//      donde medida = { sensor, valor, contador, fecha }
//
//   cb(mensajeDeError, null)      si no hay medidas o si falla
//                                 la conexión
//
// ----------------------------------------------------------------
function consultarUltimaMedida( cb ) {

  var url = "../communication/consultarUltimaMedida.php";

  // 1. crear la petición
  var peticion = new XMLHttpRequest();

  // 2. prepararla: GET sobre la ruta
  peticion.open( "GET", url, true );

  // 3. qué hacer cuando el servidor conteste
  peticion.onreadystatechange = function() {

    // sólo me interesa cuando ha terminado
    if ( peticion.readyState !== 4 ) {
      return;
    }

    // 4. comprobar si ha terminado correctamente (200)
    if ( peticion.status !== 200 ) {
      cb( "el servidor ha contestado " + peticion.status, null );
      return;
    }

    // 5. interpretar el cuerpo
    var respuesta;

    try {
      respuesta = JSON.parse( peticion.responseText );
    } catch ( e ) {
      cb( "la respuesta no es un JSON valido", null );
      return;
    }

    // 6. entregar el resultado
    if ( respuesta && respuesta.error ) {
      cb( respuesta.error, null );
      return;
    }

    cb( null, respuesta );
  };

  // 7. lanzar la petición
  peticion.send();
}
