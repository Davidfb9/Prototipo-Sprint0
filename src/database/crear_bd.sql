-- ===============================================================
--
-- Fichero:  crear_bd.sql
-- Proyecto: Sprint 0 - PBTIO 3A
-- Autor:    David Ferra
-- Fecha:    2026-10-15
-- Descripción: crea la base de datos bd_beacon y la tabla MEDIDAS,
--              que guarda cada medida que envía la placa Arduino.
--
-- Ejecución:  mysql -u root < src/database/crear_bd.sql
--             o bien desde phpMyAdmin -> Importar
--
-- ===============================================================

DROP DATABASE IF EXISTS bd_beacon;

CREATE DATABASE bd_beacon
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bd_beacon;

-- ---------------------------------------------------------------
--
-- Crear tabla MEDIDAS
--
-- sensor:Texto, valor:Z, contador:N -> crearTabla() ->
--
-- ---------------------------------------------------------------
CREATE TABLE MEDIDAS (

  id               INT           NOT NULL AUTO_INCREMENT,
  sensor           VARCHAR(20)   NOT NULL,
  valor            INT           NOT NULL,
  contador         INT           NOT NULL,
  fecha_recepcion  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),

  CONSTRAINT ck_valor_rango      CHECK ( valor BETWEEN -1000 AND 100000 ),
  CONSTRAINT ck_contador_rango   CHECK ( contador BETWEEN 0 AND 255 ),
  CONSTRAINT ck_sensor_conocido  CHECK ( sensor IN ('CO2','TEMPERATURA') )

) ENGINE = InnoDB;

-- ---------------------------------------------------------------
-- La base de datos no decide nada de la aplicación: sólo almacena
-- y mantiene las restricciones.
-- ---------------------------------------------------------------
