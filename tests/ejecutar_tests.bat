@echo off
REM ----------------------------------------------------------------
REM
REM Fichero:  ejecutar_tests.bat
REM Proyecto: Sprint 0 - PBTIO 3A
REM Autor:    David Ferra
REM Fecha:    2026-10-15
REM
REM Descripción: ejecuta TODOS los tests del proyecto.
REM
REM   PRECONDICIÓN: XAMPP arrancado (Apache y MariaDB).
REM
REM   Salida: 0 si todo pasa, 1 si algo falla.
REM
REM ----------------------------------------------------------------

cd /d "%~dp0"

set PHP=C:\xampp\php\php.exe

echo.
echo ==========================================================
echo   TESTS DEL SPRINT 0 - PBTIO 3A
echo ==========================================================
echo.

echo [1/3] Tests de la BASE DE DATOS
"%PHP%" test_bd.php
if errorlevel 1 goto :fallo

echo [2/3] Tests de la LOGICA DE NEGOCIO
"%PHP%" test_logica_negocio.php
if errorlevel 1 goto :fallo

echo [3/3] Tests del SERVIDOR REST
"%PHP%" test_rest.php
if errorlevel 1 goto :fallo

echo ==========================================================
echo   TODOS LOS TESTS HAN PASADO
echo ==========================================================
echo.
exit /b 0

:fallo
echo.
echo ==========================================================
echo   HAY TESTS FALLIDOS (ver arriba)
echo ==========================================================
echo.
exit /b 1
