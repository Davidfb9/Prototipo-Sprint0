<?php

// ----------------------------------------------------------------
//
// Fichero:  test_logica_negocio.php
// Proyecto: Sprint 0 - PBTIO 3A
// Autor:    David Ferra
// Fecha:    2026-10-15
//
// Descripción: pruebas automatizadas de la lógica de negocio.
//
//   Ejecución:  C:\xampp\php\php.exe tests\test_logica_negocio.php
//   Salida:     0 si todas pasan, 1 si alguna falla.
//
//   INDEPENDIENTES: cada test crea y borra sus propios datos.
//   IDEMPOTENTES:   al terminar, la tabla tiene el mismo número de
//                   filas que al empezar.
//
// ----------------------------------------------------------------

require_once(__DIR__ . '/../src/business_logic/medidas.php');

// ----------------------------------------------------------------
// mini-marco de pruebas
// ----------------------------------------------------------------
$contPruebas = 0;
$contFallos = 0;

// ----------------------------------------------------------------
// nombre:Texto, condicion:VoF -> comprobar() -> void
// ----------------------------------------------------------------
function comprobar( $nombre, $condicion ) {
  global $contPruebas, $contFallos;

  $contPruebas++;

  if ( $condicion ) {
    echo "  [ OK ] " . $nombre . PHP_EOL;
  } else {
    $contFallos++;
    echo "  [FALLO] " . $nombre . PHP_EOL;
  }
}

// ----------------------------------------------------------------
// antes y después: para demostrar que los tests son idempotentes
// ----------------------------------------------------------------
borrarTodasLasMedidas();
$medidasAlPrincipio = contarMedidas();

echo PHP_EOL . "=== LÓGICA DE NEGOCIO ===" . PHP_EOL . PHP_EOL;

// ----------------------------------------------------------------
// guardarMedida()
// ----------------------------------------------------------------
echo "-- guardarMedida() --" . PHP_EOL;

comprobar(
  "acepta una medida válida de CO2",
  guardarMedida( 'CO2', 450, 1 ) === true
);

comprobar(
  "acepta una medida válida de TEMPERATURA",
  guardarMedida( 'TEMPERATURA', 21, 2 ) === true
);

comprobar(
  "rechaza un sensor que no existe",
  guardarMedida( 'HUMEDAD', 50, 3 ) === false
);

comprobar(
  "rechaza un valor por encima del máximo",
  guardarMedida( 'CO2', 100001, 3 ) === false
);

comprobar(
  "rechaza un valor por debajo del mínimo",
  guardarMedida( 'TEMPERATURA', -1001, 3 ) === false
);

comprobar(
  "rechaza un contador mayor que 255",
  guardarMedida( 'CO2', 450, 256 ) === false
);

comprobar(
  "rechaza un contador negativo",
  guardarMedida( 'CO2', 450, -1 ) === false
);

comprobar(
  "acepta exactamente los extremos permitidos",
  guardarMedida( 'CO2', 100000, 255 ) === true
);

comprobar(
  "acepta un valor negativo de temperatura",
  guardarMedida( 'TEMPERATURA', -5, 0 ) === true
);

// ----------------------------------------------------------------
// guardarMedida() no reutiliza filas: guarda el historial
// ----------------------------------------------------------------
echo PHP_EOL . "-- historial --" . PHP_EOL;

$antes = contarMedidas();
guardarMedida( 'CO2', 451, 9 );
$despues = contarMedidas();

comprobar(
  "cada medida crea una fila nueva (no reutiliza)",
  $despues === $antes + 1
);

// ----------------------------------------------------------------
// consultarUltimaMedida()
// ----------------------------------------------------------------
echo PHP_EOL . "-- consultarUltimaMedida() --" . PHP_EOL;

borrarTodasLasMedidas();

comprobar(
  "devuelve un error de texto si no hay medidas",
  is_string( consultarUltimaMedida() )
);

comprobar(
  "el error es exactamente '" . ERROR_NO_HAY_MEDIDAS . "'",
  consultarUltimaMedida() === ERROR_NO_HAY_MEDIDAS
);

guardarMedida( 'CO2', 450, 1 );
guardarMedida( 'TEMPERATURA', 21, 2 );
guardarMedida( 'CO2', 800, 3 );

$ultima = consultarUltimaMedida();

comprobar( "si hay medidas devuelve un array",      is_array( $ultima ) );
comprobar( "el sensor es el de la última",          $ultima['sensor'] === 'CO2' );
comprobar( "el valor es el de la última",           $ultima['valor'] === 800 );
comprobar( "el contador es el de la última",        $ultima['contador'] === 3 );
comprobar( "la fecha no está vacía",                $ultima['fecha'] !== '' );

// ----------------------------------------------------------------
// los tipos devueltos son los del dominio
// ----------------------------------------------------------------
echo PHP_EOL . "-- tipos --" . PHP_EOL;

comprobar( "valor es un entero (Z)",     is_int( $ultima['valor'] ) );
comprobar( "contador es un entero (N)",  is_int( $ultima['contador'] ) );
comprobar( "sensor es un texto",         is_string( $ultima['sensor'] ) );
comprobar( "fecha es un texto",          is_string( $ultima['fecha'] ) );

// ----------------------------------------------------------------
// el separador decimal de los tests: ¿idempotentes?
// ----------------------------------------------------------------
borrarTodasLasMedidas();
$medidasAlFinal = contarMedidas();

echo PHP_EOL . "-- idempotencia --" . PHP_EOL;

comprobar(
  "al empezar había " . $medidasAlPrincipio . " medidas",
  $medidasAlPrincipio === 0
);

comprobar(
  "al terminar quedan " . $medidasAlFinal . " medidas (igual que al empezar)",
  $medidasAlFinal === $medidasAlPrincipio
);

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
