<?php

// ---------------------------------------------------------------
//
// Fichero:  medidas.php
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10-15
// Descripción: lógica de negocio de las medidas que envía la
//              placa Arduino.
//
//              ESTE FICHERO NO CONTIENE NADA DE HTTP: no hay rutas,
//              ni códigos de estado, ni JSON, ni peticiones. Sólo
//              reglas del dominio y acceso a la base de datos
//              descrita en doc/database_design.md.
//
// Diseño: doc/business_logic_design.md
//
// ---------------------------------------------------------------

require_once(__DIR__ . '/config.php');

// ---------------------------------------------------------------
// Tipos del dominio
//
//   Sensor  = { CO2, TEMPERATURA }
//   Medida  = ( sensor: Texto, valor: Z, contador: N, fecha: Texto )
//
// Límites del dominio (coinciden con las restricciones de la BD)
// ---------------------------------------------------------------
define( 'SENSOR_CO2', 'CO2' );
define( 'SENSOR_TEMPERATURA', 'TEMPERATURA' );

define( 'VALOR_MINIMO', -1000 );
define( 'VALOR_MAXIMO', 100000 );

define( 'CONTADOR_MINIMO', 0 );
define( 'CONTADOR_MAXIMO', 255 );

define( 'ERROR_NO_HAY_MEDIDAS', 'no hay medidas guardadas' );


// ---------------------------------------------------------------
//
// s:Texto -> esSensorValido() -> VoF
//
// Comprueba que el nombre del sensor pertenece a Sensor.
//
// ---------------------------------------------------------------
function esSensorValido( $s ) {
  return ( $s === SENSOR_CO2 || $s === SENSOR_TEMPERATURA );
}

// ---------------------------------------------------------------
//
// v:Z -> valorEnRango() -> VoF
//
// ---------------------------------------------------------------
function valorEnRango( $v ) {
  return ( $v >= VALOR_MINIMO && $v <= VALOR_MAXIMO );
}

// ---------------------------------------------------------------
//
// c:N -> contadorEnRango() -> VoF
//
// ---------------------------------------------------------------
function contadorEnRango( $c ) {
  return ( $c >= CONTADOR_MINIMO && $c <= CONTADOR_MAXIMO );
}

// ---------------------------------------------------------------
//
// sensor:Texto, valor:Z, contador:N -> guardarMedida() -> VoF
//
// Almacena una medida como una fila NUEVA. No reutiliza filas
// anteriores: se conserva el historial.
//
// Devuelve true  si la medida se ha guardado.
// Devuelve false si no se puede almacenar (parámetros fuera de
//                dominio o fallo de la base de datos).
//
// ---------------------------------------------------------------
function guardarMedida( $sensor, $valor, $contador ) {

  if ( ! esSensorValido( $sensor ) )  { return false; }
  if ( ! valorEnRango( $valor ) )     { return false; }
  if ( ! contadorEnRango( $contador ) ) { return false; }

  $conexion = obtenerConexion();

  if ( esErrorDeConexion( $conexion ) ) {
    return false;
  }

  try {
    $sentencia = $conexion->prepare(
      "INSERT INTO MEDIDAS ( sensor, valor, contador, fecha_recepcion )
       VALUES ( :sensor, :valor, :contador, CURRENT_TIMESTAMP )"
    );

    $sentencia->execute( array(
      'sensor'   => $sensor,
      'valor'    => $valor,
      'contador' => $contador
    ));

    $conexion = null;
    return true;

  } catch ( PDOException $e ) {
    // una restricción ha impedido la inserción
    return false;
  }
}

// ---------------------------------------------------------------
//
// -> consultarUltimaMedida() -> (sensor:Texto, valor:Z, contador:N, fecha:Texto) | error:Texto
//
// Recupera la medida más reciente: la fila de id más alto.
//
// ---------------------------------------------------------------
function consultarUltimaMedida() {

  $conexion = obtenerConexion();

  if ( esErrorDeConexion( $conexion ) ) {
    return ERROR_NO_HAY_MEDIDAS;
  }

  $sentencia = $conexion->query(
    "SELECT sensor, valor, contador, fecha_recepcion
     FROM MEDIDAS
     ORDER BY id DESC
     LIMIT 1"
  );

  $fila = $sentencia->fetch();

  $conexion = null;

  if ( $fila === false ) {
    return ERROR_NO_HAY_MEDIDAS;
  }

  return array(
    'sensor'   => $fila['sensor'],
    'valor'    => intval( $fila['valor'] ),
    'contador' => intval( $fila['contador'] ),
    'fecha'    => $fila['fecha_recepcion']
  );
}

// ---------------------------------------------------------------
//
// -> contarMedidas() -> N
//
// Número de medidas almacenadas. Útil para los tests.
//
// ---------------------------------------------------------------
function contarMedidas() {
  $conexion = obtenerConexion();

  if ( esErrorDeConexion( $conexion ) ) {
    return 0;
  }

  $n = $conexion->query( "SELECT COUNT(*) FROM MEDIDAS" )->fetchColumn();

  $conexion = null;

  return intval( $n );
}

// ---------------------------------------------------------------
//
// -> borrarTodasLasMedidas() -> VoF
//
// Vacía la tabla. Lo usan los tests para que sean idempotentes:
// al terminar, el estado debe ser el mismo que al empezar.
//
// ---------------------------------------------------------------
function borrarTodasLasMedidas() {
  $conexion = obtenerConexion();

  if ( esErrorDeConexion( $conexion ) ) {
    return false;
  }

  $conexion->exec( "DELETE FROM MEDIDAS" );

  $conexion = null;

  return true;
}

?>
