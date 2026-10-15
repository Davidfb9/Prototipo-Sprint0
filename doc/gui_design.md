# 5. Diseño de la interfaz gráfica y de usuario

**Componente:** `src/gui/`
**Autor:** David Ferra · **Fecha:** 2026-10-15

---

## 5.1. Diseño del componente

### Objetivo

Crear la página que utilizará el usuario para ver **la última medida** que ha recibido el sistema.

La pantalla debe centrarse únicamente en mostrar el estado de la consulta y la medida obtenida. No se necesitan menús ni otras funcionalidades.

### Diseño gráfico

Al abrir la página, mientras consulta:

```
+----------------------------------------------------------+
|                                                          |
|                  ÚLTIMA MEDIDA RECIBIDA                  |
|                                                          |
|                      Consultando...                      |
|                                                          |
|                                                          |
+----------------------------------------------------------+
```

Cuando hay una medida:

```
+----------------------------------------------------------+
|                                                          |
|                  ÚLTIMA MEDIDA RECIBIDA                  |
|                                                          |
|                         123 ppm                          |
|                                                          |
|                      Sensor: CO2                         |
|                  Contador de envío: 5                    |
|                  Recibida: 2026-10-15 19:55:22           |
|                                                          |
+----------------------------------------------------------+
```

Y si no hay ninguna medida todavía:

```
+----------------------------------------------------------+
|                                                          |
|                  ÚLTIMA MEDIDA RECIBIDA                  |
|                                                          |
|            no hay medidas guardadas                      |
|                                                          |
+----------------------------------------------------------+
```

### Esquema de la estructura

```
+--------------------------------------------------+
|  gui/Aplicacion.html                             |
|                                                  |
|   <div class="pantalla">                         |
|     <p class="titulo">Ultima medida recibida</p> |
|     <p class="estado"   id="zonaValor">          |
|     <p class="detalle"  id="zonaDetalle">        |
|   </div>                                         |
|                                                  |
|   <script src=                                   |
|     "../frontend_business_logic/                 |
|      consultarUltimaMedida.js">                  |
|                                                  |
|   <script>  refrescar(); setInterval(...)  </script>
+--------------------------------------------------+
```

### Diagrama de la interfaz

```
   +---------------------------------------------------+
   |                  PANTALLA                          |
   |                                                   |
   |   titulo       -> "Ultima medida recibida"        |
   |   zonaValor    -> el numero grande                |
   |   zonaDetalle  -> sensor, contador, fecha         |
   |                                                   |
   |   refrescar()                                     |
   |     |                                             |
   |     |  consultarUltimaMedida(cb)                  |
   |     v                                             |
   |   Lógica fake del navegador                       |
   +---------------------------------------------------+
```

---

## 5.2. Aclaraciones de diseño

### ¿Por qué no hay botón?

Porque la página se **refresca sola cada 3 segundos**. El usuario no tiene que hacer nada: deja la página abierta y ve cómo aparece la medida cuando la placa emite.

### ¿Por qué la pantalla no pide la medida directamente con `fetch`?

**Porque la interfaz gráfica no debe tener lógica.** La pantalla llama a `consultarUltimaMedida(cb)`, que está en `frontend_business_logic/`. Si mañana cambia la API REST, sólo se toca el JavaScript de la lógica fake y la página sigue funcionando igual.

Este es el punto que distingue una interfaz de una interfaz con lógica incrustada.

### ¿Cómo distingue la pantalla entre éxito y error?

**Por el primer argumento del callback.** Si llega `null` hay medida; si llega un texto, es un error. La pantalla no interpreta códigos de estado.

### Las unidades

Se muestran según el sensor: `ppm` para `CO2` y `°C` para `TEMPERATURA`. La decisión se toma leyendo `medida.sensor`, no con un número de versión ni con un código.

### Accesibilidad y legibilidad

* El número de la medida es grande y centrado, para que se lea desde la última fila de la sala durante la demostración.
* Los textos alternativos (`Consultando...`, mensajes de error) están en castellano.
* El `viewport` está declarado, por lo que la página se ve bien en el móvil.

---

## 5.3. Reglas generales

| Regla | Aplicación en este componente |
|---|---|
| **Lenguaje de programación** | **HTML5** y **CSS3** embebido. El comportamiento está en `frontend_business_logic/`, no aquí. |
| **Cabeceras** | Comentario `Lógica fake del navegador` sobre el `script` que llama a la capa, y comentario `Esta parte sólo pinta` sobre el script de la pantalla. |
| **Legibilidad** | Nombres de clase descriptivos (`pantalla`, `zonaValor`, `zonaDetalle`). Un solo `id` por elemento que se va a tocar desde JavaScript. |
| **Pruebas automatizadas** | Se comprueba abriendo `http://localhost/Prototipo/src/gui/Aplicacion.html`. |
| **Idempotencia** | La página sólo lee, nunca escribe. |
