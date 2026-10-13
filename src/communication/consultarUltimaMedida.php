<?php

// ----------------------------------------------------------------
//
// Fichero:  consultarUltimaMedida.php
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10-15
//
// GET ../communication/consultarUltimaMedida.php
//
// -> ( sensor:Texto, valor:Z, contador:N, fecha:Texto )
//  | ( error: Texto )
//
// Devuelve al navegador la última medida almacenada. No recibe
// parámetros.
//
// Ejemplo:
//   curl http://localhost/Prototipo/src/communication/consultarUltimaMedida.php
//
// ----------------------------------------------------------------

require_once(__DIR__ . '/../business_logic/medidas.php');

header( 'Content-Type: application/json; charset=utf-8' );

$objetoResultado = new stdClass;

// --------------------------------------------------------------
// el método debe ser GET
// --------------------------------------------------------------
if ( $_SERVER['REQUEST_METHOD'] !== 'GET' ) {
  http_response_code( 405 );
  $objetoResultado->error = "se esperaba una peticion GET";
  echo json_encode( $objetoResultado );
  return;
}

// --------------------------------------------------------------
// llamada a la verdadera función de la lógica de negocio
// --------------------------------------------------------------
$medida = consultarUltimaMedida();

// --------------------------------------------------------------
// si es un texto es que ha habido un error
// --------------------------------------------------------------
if ( is_string( $medida ) ) {
  http_response_code( 404 );
  $objetoResultado->error = $medida;
  echo json_encode( $objetoResultado );
  return;
}

// echo == devolver
echo json_encode( $medida );

?>
