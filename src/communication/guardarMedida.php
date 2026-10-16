<?php

// ----------------------------------------------------------------
//
// Fichero:  guardarMedida.php
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10-15
// Descripción: Ruta POST del servidor REST: recibe y almacena una medida.
// Copyright:   (c) 2026 David Ferra
// Aportación:  Anade la ruta REST sobre la logica de negocio.
//
// POST ../communication/guardarMedida.php
//
// cuerpo: (sensor:Text, valor:Z, contador:N)
// --> ( resultado: B ) | ( error: Text )
//
// Recibe la medida que envía el teléfono Android y la delega en
// la lógica del negocio. ESTE FICHERO NO VALIDA NADA: la
// validación vive en business_logic/medidas.php.
//
// Ejemplo:
//   curl -X POST http://localhost/Prototipo/src/communication/guardarMedida.php ^
//        -d "{\"sensor\":\"CO2\",\"valor\":123,\"contador\":1}"
//
// ----------------------------------------------------------------

require_once(__DIR__ . '/../business_logic/medidas.php');

header( 'Content-Type: application/json; charset=utf-8' );

$objetoResultado = new stdClass;

// --------------------------------------------------------------
// el método debe ser POST
// --------------------------------------------------------------
if ( $_SERVER['REQUEST_METHOD'] !== 'POST' ) {
  http_response_code( 405 );
  $objetoResultado->error = "se esperaba una peticion POST";
  echo json_encode( $objetoResultado );
  return;
}

// --------------------------------------------------------------
// obtengo el cuerpo en crudo
// --------------------------------------------------------------
$cuerpoBruto = file_get_contents( "php://input" );

$datos = json_decode( $cuerpoBruto, true );

if ( $datos === null || ! is_array( $datos ) ) {
  http_response_code( 400 );
  $objetoResultado->error = "el cuerpo no es un JSON valido";
  echo json_encode( $objetoResultado );
  return;
}

// --------------------------------------------------------------
// si falta algún campo, la lógica de negocio lo rechazará
// --------------------------------------------------------------
$sensor   = $datos['sensor']   ?? '';
$valor    = $datos['valor']    ?? null;
$contador = $datos['contador'] ?? null;

// --------------------------------------------------------------
// llamada a la verdadera función de la lógica de negocio
// --------------------------------------------------------------
if ( guardarMedida( $sensor, $valor, $contador ) === true ) {
  $objetoResultado->resultado = true;
} else {
  http_response_code( 400 );
  $objetoResultado->resultado = false;
  $objetoResultado->error = "medida no valida";
}

// echo == devolver
echo json_encode( $objetoResultado );

?>
