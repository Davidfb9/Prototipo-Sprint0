<?php

// ---------------------------------------------------------------
//
// Fichero:  config.php
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10-15
// Copyright:   (c) 2026 David Ferra
// Aportación:  Capa de persistencia: configuracion y conexion PDO con bd_beacon.
// Descripción: configuración y conexión con la base de datos.
//              Forma parte de la capa de persistencia descrita en
//              doc/database_design.md.
//
// ---------------------------------------------------------------

// ---------------------------------------------------------------
//
// --> obtenerConexion() --> PDO | error:Text
//
// Devuelve la conexión a la base de datos bd_beacon.
//
// ---------------------------------------------------------------
function obtenerConexion() {

  $servidor = "localhost";
  $baseDeDatos = "bd_beacon";
  $usuario = "root";
  $clave = ""; // XAMPP por defecto: root sin contraseña

  try {
    $conexion = new PDO(
      "mysql:host=" . $servidor . ";dbname=" . $baseDeDatos . ";charset=utf8mb4",
      $usuario,
      $clave
    );

    // las violaciones de restricción deben saltar como excepciones
    $conexion->setAttribute( PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION );
    // las filas se devuelven como arrays asociativos
    $conexion->setAttribute( PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC );

    return $conexion;

  } catch ( PDOException $e ) {
    return $e; // el llamador comprobará si es un PDOException
  }
}

// ---------------------------------------------------------------
//
// e:Text --> esErrorDeConexion() --> B
//
// Comprueba si obtenerConexion() ha fallado.
//
// ---------------------------------------------------------------
function esErrorDeConexion( $conexion ) {
  return ( $conexion instanceof PDOException );
}

?>
