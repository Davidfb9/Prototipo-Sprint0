<?php

// ----------------------------------------------------------------
//
// Fichero:  test_bd.php
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10-15
// Copyright:   (c) 2026 David Ferra
// Aportación:  Pruebas automaticas del esquema y de las restricciones de la base de datos.
//
// Descripción: pruebas automatizadas de la base de datos.
//
//   Comprueba que la BD impone SUS PROPIAS restricciones, es decir,
//   que aunque la lógica de negocio fallara, la base de datos no
//   dejaría guardar algo incorrecto.
//
//   Ejecución:  C:\xampp\php\php.exe test\test_bd.php
//   Salida:     0 si todas pasan, 1 si alguna falla.
//
// ----------------------------------------------------------------

require_once(__DIR__ . '/../src/business_logic/config.php');

$contPruebas = 0;
$contFallos = 0;

function comprobar( $nombre, $condicion ) {
  global $contPruebas, $contFallos;
  $contPruebas++;
  if ( $condicion ) { echo "  [ OK ] " . $nombre . PHP_EOL; }
  else { $contFallos++; echo "  [FALLO] " . $nombre . PHP_EOL; }
}

// ----------------------------------------------------------------
// sql:Text --> intentarEjecutar() --> B
//   Devuelve true si la sentencia ha sido ACEPTADA por la BD.
// ----------------------------------------------------------------
function intentarEjecutar( $sql ) {
  $conexion = obtenerConexion();
  if ( esErrorDeConexion( $conexion ) ) { return false; }

  try {
    $conexion->exec( $sql );
    $conexion->exec( "DELETE FROM MEDIDAS" ); // limpiar el intento
    $conexion = null;
    return true;
  } catch ( PDOException $e ) {
    $conexion = null;
    return false;
  }
}

$conexion = obtenerConexion();

if ( esErrorDeConexion( $conexion ) ) {
  echo PHP_EOL . "No se puede conectar con la base de datos bd_beacon." . PHP_EOL;
  echo "¿Se ha ejecutado src/database/crear_bd.sql?" . PHP_EOL . PHP_EOL;
  exit( 1 );
}

$conexion->exec( "DELETE FROM MEDIDAS" );

echo PHP_EOL . "=== BASE DE DATOS ===" . PHP_EOL . PHP_EOL;

// ----------------------------------------------------------------
// la tabla y su estructura
// ----------------------------------------------------------------
echo "-- estructura --" . PHP_EOL;

$columnas = $conexion->query( "SHOW COLUMNS FROM MEDIDAS" )->fetchAll();

$nombresColumnas = array();
foreach ( $columnas as $c ) { $nombresColumnas[] = $c['Field']; }

comprobar( "existe la columna id",              in_array( 'id', $nombresColumnas ) );
comprobar( "existe la columna sensor",          in_array( 'sensor', $nombresColumnas ) );
comprobar( "existe la columna valor",           in_array( 'valor', $nombresColumnas ) );
comprobar( "existe la columna contador",        in_array( 'contador', $nombresColumnas ) );
comprobar( "existe la columna fecha_recepcion", in_array( 'fecha_recepcion', $nombresColumnas ) );

$columnaId = $conexion->query( "SHOW COLUMNS FROM MEDIDAS WHERE Field = 'id'" )->fetch();

comprobar( "el id es AUTO_INCREMENT",
  stripos( $columnaId['Extra'], 'auto_increment' ) !== false );

// ----------------------------------------------------------------
// restricción: sólo se aceptan estos sensores
// ----------------------------------------------------------------
echo PHP_EOL . "-- restricción CHECK sobre sensor --" . PHP_EOL;

comprobar( "acepta 'CO2'",
  intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('CO2',100,1)" ) );

comprobar( "acepta 'TEMPERATURA'",
  intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('TEMPERATURA',20,1)" ) );

comprobar( "rechaza 'HUMEDAD'",
  ! intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('HUMEDAD',20,1)" ) );

comprobar( "rechaza un sensor vacío",
  ! intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('',20,1)" ) );

// ----------------------------------------------------------------
// restricción: el valor debe estar en rango
// ----------------------------------------------------------------
echo PHP_EOL . "-- restricción CHECK sobre valor --" . PHP_EOL;

comprobar( "acepta el valor mínimo (-1000)",
  intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('CO2',-1000,1)" ) );

comprobar( "acepta el valor máximo (100000)",
  intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('CO2',100000,1)" ) );

comprobar( "rechaza -1001",
  ! intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('CO2',-1001,1)" ) );

comprobar( "rechaza 100001",
  ! intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('CO2',100001,1)" ) );

// ----------------------------------------------------------------
// restricción: el contador debe estar entre 0 y 255
// ----------------------------------------------------------------
echo PHP_EOL . "-- restricción CHECK sobre contador --" . PHP_EOL;

comprobar( "acepta el contador 0",
  intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('CO2',100,0)" ) );

comprobar( "acepta el contador 255",
  intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('CO2',100,255)" ) );

comprobar( "rechaza el contador 256",
  ! intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('CO2',100,256)" ) );

comprobar( "rechaza el contador -1",
  ! intentarEjecutar( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('CO2',100,-1)" ) );

// ----------------------------------------------------------------
// el id es la clave primaria
// ----------------------------------------------------------------
echo PHP_EOL . "-- clave primaria --" . PHP_EOL;

$conexion->exec( "DELETE FROM MEDIDAS" );
$conexion->exec( "INSERT INTO MEDIDAS (id,sensor,valor,contador) VALUES (1,'CO2',100,1)" );

comprobar( "rechaza repetir la clave primaria 1",
  ! intentarEjecutar( "INSERT INTO MEDIDAS (id,sensor,valor,contador) VALUES (1,'CO2',100,1)" ) );

$conexion->exec( "DELETE FROM MEDIDAS" );

// ----------------------------------------------------------------
// fecha_recepcion se rellena sola
// ----------------------------------------------------------------
echo PHP_EOL . "-- fecha_recepcion --" . PHP_EOL;

$conexion->exec( "INSERT INTO MEDIDAS (sensor,valor,contador) VALUES ('CO2',100,1)" );
$fecha = $conexion->query( "SELECT fecha_recepcion FROM MEDIDAS LIMIT 1" )->fetchColumn();

comprobar( "se rellena automáticamente",   $fecha !== null && $fecha !== '' );
comprobar( "no es un campo vacío",         strlen( $fecha ) >= 10 );

$conexion->exec( "DELETE FROM MEDIDAS" );

// ----------------------------------------------------------------
// idempotencia
// ----------------------------------------------------------------
echo PHP_EOL . "-- idempotencia --" . PHP_EOL;

comprobar( "al terminar la tabla queda vacía",
  $conexion->query( "SELECT COUNT(*) FROM MEDIDAS" )->fetchColumn() == 0 );

$conexion = null;

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
