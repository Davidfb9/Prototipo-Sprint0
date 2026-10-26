// -----------------------------------------------------------------
// 
// Fichero:     PeticionarioREST.java
// Descripción: Cliente HTTP generico basado en AsyncTask.
// Copyright:   (c) 2026 David Ferra
// Fecha:       2026-10-15
// Autor:       David Ferra
// Aportación:  Se adapto para enviar las medidas al servidor REST del proyecto.
// 
// -----------------------------------------------------------------

package org.jordi.pruebarest1;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;

// ------------------------------------------------------------------------
// Clase: cliente HTTP genérico basado en AsyncTask.
//
// Ejecuta cada petición en un hilo aparte para no bloquear la interfaz
// y entrega el resultado por la interfaz RespuestaREST.
// ------------------------------------------------------------------------
public class PeticionarioREST extends AsyncTask<Void, Void, Boolean> {

    // --------------------------------------------------------------------
// Estado de la petición en curso: método, destino, cuerpo
// y la interfaz que recibirá la respuesta.
    // --------------------------------------------------------------------
    private String elMetodo;
    private String urlDestino;
    private String elCuerpo = null;
    private RespuestaREST laRespuesta;
    private int codigoRespuesta;
    private String cuerpoRespuesta = "";

    // --------------------------------------------------------------------
// --> PeticionarioREST() --> void
//
// Crea el cliente sin parámetros. Lo demás se fija en cada petición
// con hacerPeticionREST().
    // --------------------------------------------------------------------
    public PeticionarioREST() {
        Log.d("clienterestandroid", "constructor()");
    }

    // --------------------------------------------------------------------
// método:Text, urlDestino:Text, cuerpo:Text, laRespuesta:Callback
//   --> hacerPeticionREST() --> void
//
// Guarda los datos de la petición y la lanza en un hilo aparte.
// El resultado llega a laRespuesta.callback(código, cuerpo).
    // --------------------------------------------------------------------
    public void hacerPeticionREST(String metodo, String urlDestino, String cuerpo, RespuestaREST laRespuesta) {
        this.elMetodo = metodo;
        this.urlDestino = urlDestino;
        this.elCuerpo = cuerpo;
        this.laRespuesta = laRespuesta;

        this.execute(); // otro thread ejecutará doInBackground()
    }

    // --------------------------------------------------------------------
// parámetros:Z[] --> doInBackground() --> B
//
// Abre la conexión, escribe la petición y lee la respuesta.
// Devuelve true si la petición ha terminado bien.
    // --------------------------------------------------------------------
    @Override
    protected Boolean doInBackground(Void... params) {
        Log.d("clienterestandroid", "doInBackground()");

        try {

            // envio la peticion

            // pagina web para hacer pruebas: URL url = new URL("https://httpbin.org/html");
            // ordinador del despatx 158.42.144.126 // OK URL url = new URL("http://158.42.144.126:8080");

            Log.d("clienterestandroid", "doInBackground() me conecto a >" + urlDestino + "<");

            URL url = new URL(urlDestino);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; charset-utf-8");
            connection.setRequestMethod(this.elMetodo);
            // connection.setRequestProperty("Accept", "*/*);

            // connection.setUseCaches(false);
            connection.setDoInput(true);

            if (!this.elMetodo.equals("GET") && this.elCuerpo != null) {
                Log.d("clienterestandroid", "doInBackground(): no es get, pongo cuerpo");
                connection.setDoOutput(true);
                // si no es GET, pongo el cuerpo que me den en la peticin
                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                dos.writeBytes(this.elCuerpo);
                dos.flush();
                dos.close();
            }

            // ya he enviado la peticin
            Log.d("clienterestandroid", "doInBackground(): peticin enviada ");

            // ahora obtengo la respuesta

            int rc = connection.getResponseCode();
            String rm = connection.getResponseMessage();
            String respuesta = "" + rc + " : " + rm;
            Log.d("clienterestandroid", "doInBackground() recibo respuesta = " + respuesta);
            this.codigoRespuesta = rc;

            try {

                InputStream is = connection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                Log.d("clienterestandroid", "leyendo cuerpo");
                StringBuilder acumulador = new StringBuilder();
                String linea;
                while ((linea = br.readLine()) != null) {
                    Log.d("clienterestandroid", linea);
                    acumulador.append(linea);
                }
                Log.d("clienterestandroid", "FIN leyendo cuerpo");

                this.cuerpoRespuesta = acumulador.toString();
                Log.d("clienterestandroid", "cuerpo recibido=" + this.cuerpoRespuesta);

                connection.disconnect();

            } catch (IOException ex) {
                // dispara excepcin cuando la respuesta REST no tiene cuerpo y yo intento getInputStream()
                Log.d("clienterestandroid", "doInBackground() : parece que no hay cuerpo en la respuesta");
            }

            return true; // doInBackground() termina bien

        } catch (Exception ex) {
            Log.d("clienterestandroid", "doInBackground(): ocurrio alguna otra excepcion: " + ex.getMessage());
        }

        return false; // doInBackground() NO termina bien
    } // ()

    // --------------------------------------------------------------------
// comoFue:B --> onPostExecute() --> void
//
// Se ejecuta al terminar doInBackground() y devuelve el código
// y el cuerpo de la respuesta a la interfaz RespuestaREST.
    // --------------------------------------------------------------------
    protected void onPostExecute(Boolean comoFue) {
        // llamado tras doInBackground()
        Log.d("clienterestandroid", "onPostExecute() comoFue = " + comoFue);
        this.laRespuesta.callback(this.codigoRespuesta, this.cuerpoRespuesta);
    }

    // --------------------------------------------------------------------
// Interfaz de devolución de la petición REST.
//
// código:N, cuerpo:Text --> callback() --> void
    // --------------------------------------------------------------------
    public interface RespuestaREST {
        void callback(int codigo, String cuerpo);
    }

} // class


