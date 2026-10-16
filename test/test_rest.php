<?php

// ----------------------------------------------------------------
//
// Fichero:  test_rest.php
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10-15
// Copyright:   (c) 2026 David Ferra
// Aportación:  Pruebas automaticas de la API REST, con peticiones curl reales.
//
// Descripción: pruebas automatizadas del servidor REST.
//
//   PRECONDICIÓN: XAMPP debe estar arrancado (Apache en el 80).
//
//   Ejecución:  C:\xampp\php\php.exe test\test_rest.php
//   Salida:     0 si todas pasan, 1 si alguna falla.
//
//   Se pegan peticiones HTTP REALES contra el servidor, igual que
//   lo haría el teléfono Android.
//
//   IDEMPOTENTES: al terminar se borran las medidas creadas.
//
// ----------------------------------------------------------------

require_once(__DIR__ . '/../src/business_logic/medidas.php');

const URL_BASE = "http://localhost/Prototipo/src/communication";

// ----------------------------------------------------------------
// mini-marco de pruebas
// ----------------------------------------------------------------
$contPruebas = 0;
$contFallos = 0;

function comprobar( $nombre, $condicion ) {
  global $contPruebas, $contFallos;
  $contPruebas++;
  if ( $condicion ) { echo "  [ OK ] " . $nombre . PHP_EOL; }
  else { $contFallos++; echo "  [FALLO] " . $nombre . PHP_EOL; }
}

// ----------------------------------------------------------------
// ruta:Text, metodo:Text, cuerpo:Text --> hacerPeticion() --> (codigo:N, cuerpo:Text)
// ----------------------------------------------------------------
function hacerPeticion( $ruta, $metodo, $cuerpo = null ) {

  $peticion = curl_init();

  curl_setopt( $peticion, CURLOPT_URL, URL_BASE . "/" . $ruta );
  curl_setopt( $peticion, CURLOPT_RETURNTRANSFER, true );
  curl_setopt( $peticion, CURLOPT_HEADER, true );
  curl_setopt( $peticion, CURLOPT_CUSTOMREQUEST, $metodo );

  if ( $cuerpo !== null ) {
    curl_setopt( $peticion, CURLOPT_POSTFIELDS, $cuerpo );
  }

  $respuestaCompleta = curl_exec( $peticion );

  $tamanyoCabecera = curl_getinfo( $peticion, CURLINFO_HEADER_SIZE );
  $codigo = curl_getinfo( $peticion, CURLINFO_RESPONSE_CODE );

  curl_close( $peticion );

  $cabecera = substr( $respuestaCompleta, 0, $tamanyoCabecera );
  $cuerpoRespuesta = substr( $respuestaCompleta, $tamanyoCabecera );

  return array( 'codigo' => $codigo, 'cuerpo' => $cuerpoRespuesta );
}

// ----------------------------------------------------------------
borrarTodasLasMedidas();

echo PHP_EOL . "=== SERVIDOR REST ===" . PHP_EOL . PHP_EOL;

// ----------------------------------------------------------------
// comprobar que el servidor está arrancado
// ----------------------------------------------------------------
$prueba = hacerPeticion( 'consultarUltimaMedida.php', 'GET' );

if ( $prueba['codigo'] === 0 || $prueba['codigo'] === 500 ) {
  echo "No se puede conectar con Apache." . PHP_EOL;
  echo "¿Está XAMPP arrancado? (Apache en el puerto 80)" . PHP_EOL . PHP_EOL;
  exit( 1 );
}

// ----------------------------------------------------------------
// GET consultarUltimaMedida.php
// ----------------------------------------------------------------
echo "-- GET consultarUltimaMedida.php --" . PHP_EOL;

$r = hacerPeticion( 'consultarUltimaMedida.php', 'GET' );

comprobar( "con la BD vacía devuelve 404",              $r['codigo'] === 404 );

$json = json_decode( $r['cuerpo'], true );

comprobar( "devuelve un error de texto",                isset( $json['error'] ) );

guardarMedida( 'CO2', 450, 1 );

$r = hacerPeticion( 'consultarUltimaMedida.php', 'GET' );
$json = json_decode( $r['cuerpo'], true );

comprobar( "con medidas devuelve 200",                  $r['codigo'] === 200 );
comprobar( "el sensor es 'CO2'",                        $json['sensor'] === 'CO2' );
comprobar( "el valor es 450",                           $json['valor'] === 450 );
comprobar( "el contador es 1",                          $json['contador'] === 1 );
comprobar( "la fecha no está vacía",                    $json['fecha'] !== '' );

$r = hacerPeticion( 'consultarUltimaMedida.php', 'POST' );

comprobar( "rechaza un POST (405 Method Not Allowed)",  $r['codigo'] === 405 );

// ----------------------------------------------------------------
// POST guardarMedida.php
// ----------------------------------------------------------------
echo PHP_EOL . "-- POST guardarMedida.php --" . PHP_EOL;

$antesDeInsertar = contarMedidas();

$r = hacerPeticion( 'guardarMedida.php', 'POST',
  json_encode( array( 'sensor' => 'CO2', 'valor' => 123, 'contador' => 2 ) ) );
$json = json_decode( $r['cuerpo'], true );

comprobar( "una medida válida devuelve 200",            $r['codigo'] === 200 );
comprobar( "el resultado es true",                      $json['resultado'] === true );
comprobar( "la base de datos ha crecido en 1",          contarMedidas() === $antesDeInsertar + 1 );

$r = hacerPeticion( 'consultarUltimaMedida.php', 'GET' );
$json = json_decode( $r['cuerpo'], true );

comprobar( "la medida guardada es la que se recupera",  $json['valor'] === 123 );

// ----------------------------------------------------------------
// rechazos
// ----------------------------------------------------------------
echo PHP_EOL . "-- rechazos --" . PHP_EOL;

$r = hacerPeticion( 'guardarMedida.php', 'POST',
  json_encode( array( 'sensor' => 'HUMEDAD', 'valor' => 50, 'contador' => 2 ) ) );

comprobar( "un sensor desconocido devuelve 400",        $r['codigo'] === 400 );

$r = hacerPeticion( 'guardarMedida.php', 'POST',
  json_encode( array( 'sensor' => 'CO2', 'valor' => 999999, 'contador' => 2 ) ) );

comprobar( "un valor fuera de rango devuelve 400",      $r['codigo'] === 400 );

$r = hacerPeticion( 'guardarMedida.php', 'POST', 'esto no es json' );

comprobar( "un cuerpo que no es JSON devuelve 400",     $r['codigo'] === 400 );

$r = hacerPeticion( 'guardarMedida.php', 'GET' );

comprobar( "rechaza un GET (405 Method Not Allowed)",   $r['codigo'] === 405 );

// ----------------------------------------------------------------
// idempotencia
// ----------------------------------------------------------------
borrarTodasLasMedidas();

echo PHP_EOL . "-- idempotencia --" . PHP_EOL;

comprobar( "al terminar la tabla queda vacía",          contarMedidas() === 0 );

// ----------------------------------------------------------------
// resumen
// ----------------------------------------------------------------
echo PHP_EOL . "-----------------------------------------------" . PHP_EOL;
echo "Pruebas: " . $contPruebas . "   Fallos: " . $contFallos . PHP_EOL;

if ( $contFallos === 0 ) {
  echo "TODAS LAS PRUEBAS HAN PASADO" . PHP_EOL . PHP_EOL;
  exit( 0 );
}

echo "HAY FALLOS" . PHP_EOL . PHP_EOL;
exit( 1 );

?>
