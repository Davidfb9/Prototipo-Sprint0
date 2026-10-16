<?php

// ----------------------------------------------------------------
//
// Fichero:     test_ui.php
// Descripción: pruebas automatizadas de la interfaz gráfica y de su
//              lógica fake del navegador.
// Copyright:   (c) 2026 David Ferra
// Fecha:       2026-10-15
// Autor:       David Ferra
// Aportación:  Batería de pruebas de UI: separación de la GUI,
//              encapsulación de la comunicación e identidad de firmas.
//
//   Comprueba la REGLA MÁS FRÁGIL de la arquitectura: que la interfaz
//   no se comunique por su cuenta y que la lógica del navegador
//   ofrezca exactamente la misma firma que la lógica del backend.
//
//   Es una prueba ESTÁTICA sobre el código fuente: no necesita
//   servidor Apache, ni navegador, ni base de datos. Por eso es
//   independiente y se puede ejecutar tantas veces como se quiera.
//
//   Ejecución:  C:\xampp\php\php.exe test\test_ui.php
//   Salida:     0 si todas pasan, 1 si alguna falla.
//
// ----------------------------------------------------------------

$raiz = dirname( __DIR__ );

$contPruebas = 0;
$contFallos  = 0;

function comprobar( $nombre, $condicion ) {
  global $contPruebas, $contFallos;
  $contPruebas++;
  if ( $condicion ) { echo "  [ OK ] " . $nombre . PHP_EOL; }
  else { $contFallos++; echo "  [FALLO] " . $nombre . PHP_EOL; }
}

// ----------------------------------------------------------------
// ruta:Text --> leer() --> Text
//   Devuelve el contenido del fichero, o '' si no existe.
// ----------------------------------------------------------------
function leer( $ruta ) {
  if ( !is_file( $ruta ) ) { return ''; }
  return file_get_contents( $ruta );
}

$rutaGui   = $raiz . '/src/gui/Aplicacion.html';
$rutaFake  = $raiz . '/src/frontend_business_logic/consultarUltimaMedida.js';
$rutaBack  = $raiz . '/src/business_logic/medidas.php';

$gui  = leer( $rutaGui );
$fake = leer( $rutaFake );
$back = leer( $rutaBack );

if ( $gui === '' ) {
  echo PHP_EOL . "No se encuentra src/gui/Aplicacion.html." . PHP_EOL;
  exit( 1 );
}

echo PHP_EOL . "=== INTERFAZ GRAFICA Y LOGICA DEL NAVEGADOR ===" . PHP_EOL . PHP_EOL;

// ----------------------------------------------------------------
// la interfaz sólo pinta
// ----------------------------------------------------------------
echo "-- la interfaz no se comunica por su cuenta --" . PHP_EOL;

comprobar( "existe src/gui/Aplicacion.html",              true );
comprobar( "es un documento HTML5",                       stripos( $gui, '<!DOCTYPE html>' ) !== false );
comprobar( "declara el idioma español",                   strpos( $gui, 'lang="es"' ) !== false );
comprobar( "declara viewport (es responsiva)",            strpos( $gui, 'viewport' ) !== false );
comprobar( "cierra la etiqueta </html>",                  stripos( $gui, '</html>' ) !== false );

comprobar( "NO usa XMLHttpRequest",                       stripos( $gui, 'XMLHttpRequest' ) === false );
comprobar( "NO usa fetch(",                               stripos( $gui, 'fetch(' ) === false );
comprobar( "NO usa WebSocket",                            stripos( $gui, 'WebSocket' ) === false );
comprobar( "NO abre ninguna URL http/https",              stripos( $gui, 'http://' ) === false
                                                        && stripos( $gui, 'https://' ) === false );
comprobar( "NO contiene SQL",                             stripos( $gui, 'SELECT' ) === false
                                                        && stripos( $gui, 'INSERT' ) === false );
comprobar( "NO contiene PHP",                             stripos( $gui, '<?php' ) === false );

// ----------------------------------------------------------------
// la interfaz sí llama a la logica fake
// ----------------------------------------------------------------
echo PHP_EOL . "-- la interfaz usa la logica fake --" . PHP_EOL;

comprobar( "incluye el script de la logica fake",         strpos( $gui, 'consultarUltimaMedida.js' ) !== false );
comprobar( "llama a consultarUltimaMedida",               strpos( $gui, 'consultarUltimaMedida(' ) !== false );
comprobar( "tiene el id de la zona del valor",            strpos( $gui, 'id="zonaValor"' ) !== false );
comprobar( "tiene el id de la zona de detalle",           strpos( $gui, 'id="zonaDetalle"' ) !== false );

// ----------------------------------------------------------------
// la logica fake encapsula la comunicacion y no toca la interfaz
// ----------------------------------------------------------------
echo PHP_EOL . "-- la logica del navegador --" . PHP_EOL;

comprobar( "existe el fichero de logica fake",            $fake !== '' );
comprobar( "define la funcion consultarUltimaMedida",     preg_match( '/function\s+consultarUltimaMedida\s*\(/', $fake ) === 1 );
comprobar( "lleva su diseno logico entre lineas ----",    strpos( $fake, '------' ) !== false );
comprobar( "encapsula la peticion HTTP dentro de si",     stripos( $fake, 'XMLHttpRequest' ) !== false );
comprobar( "NO toca el DOM de la interfaz",               stripos( $fake, 'document.' ) === false );
comprobar( "NO consulta la base de datos",                stripos( $fake, 'SELECT' ) === false );

// ----------------------------------------------------------------
// identidad de firmas con el backend
// ----------------------------------------------------------------
echo PHP_EOL . "-- identidad de firmas --" . PHP_EOL;

comprobar( "existe la logica de negocio del backend",     $back !== '' );
comprobar( "el backend define la misma funcion",          preg_match( '/function\s+consultarUltimaMedida\s*\(/', $back ) === 1 );
comprobar( "ambas reciben un unico callback",             preg_match( '/function\s+consultarUltimaMedida\s*\(\s*\$?cb/', $fake ) !== false );

// ----------------------------------------------------------------
// los documentos de diseno existen
// ----------------------------------------------------------------
echo PHP_EOL . "-- documentos de diseno --" . PHP_EOL;

comprobar( "existe doc/gui_design.md",                    is_file( $raiz . '/doc/gui_design.md' ) );
comprobar( "existe doc/frontend_business_logic_design.md", is_file( $raiz . '/doc/frontend_business_logic_design.md' ) );

// ----------------------------------------------------------------
// resumen
// ----------------------------------------------------------------
echo PHP_EOL . "----------------------------------------------------------" . PHP_EOL;
echo "  Pruebas: " . $contPruebas . "   Fallos: " . $contFallos . PHP_EOL;
echo "----------------------------------------------------------" . PHP_EOL . PHP_EOL;

exit( $contFallos === 0 ? 0 : 1 );
