// -----------------------------------------------------------------
// 
// Fichero:     Utilidades.java
// Descripción: Utilidades de conversion de bytes, UUID y textos.
// Copyright:   (c) 2026 David Ferra
// Fecha:       2026-10-15
// Autor:       Jordi Bataller i Mascarell (codigo de partida)
// Aportación:  Se completaron las ayudas de conversion que necesita el escaneo.
// 
// -----------------------------------------------------------------

package org.jordi.pruebarest1;


import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

// -----------------------------------------------------------------------------------
// @author: Jordi Bataller i Mascarell
// -----------------------------------------------------------------------------------
public class Utilidades {

    // -------------------------------------------------------------------------------
// texto:Text --> stringToBytes() --> byte[]
//
// Convierte un texto en su representación de bytes.
    // -------------------------------------------------------------------------------
    public static byte[] stringToBytes ( String texto ) {
        return texto.getBytes();
        // byte[] b = string.getBytes(StandardCharsets.UTF_8); // Ja
    } // ()

    // -------------------------------------------------------------------------------
// uuid:Text --> stringToUUID() --> UUID
//
// Convierte un texto de 16 caracteres en un UUID.
// Si no tiene 16 caracteres lanza un Error.
    // -------------------------------------------------------------------------------
    public static UUID stringToUUID( String uuid ) {
        if ( uuid.length() != 16 ) {
            throw new Error( "stringUUID: string no tiene 16 caracteres ");
        }
        byte[] comoBytes = uuid.getBytes();

        String masSignificativo = uuid.substring(0, 8);
        String menosSignificativo = uuid.substring(8, 16);
        UUID res = new UUID( Utilidades.bytesToLong( masSignificativo.getBytes() ), Utilidades.bytesToLong( menosSignificativo.getBytes() ) );

        // Log.d( MainActivity.ETIQUETA_LOG, " \n\n***** stringToUUID *** " + uuid  + "=?=" + Utilidades.uuidToString( res ) );

        // UUID res = UUID.nameUUIDFromBytes( comoBytes ); no va como quiero

        return res;
    } // ()

    // -------------------------------------------------------------------------------
// uuid:UUID --> uuidToString() --> Text
//
// Convierte un UUID en su texto de 16 caracteres.
    // -------------------------------------------------------------------------------
    public static String uuidToString ( UUID uuid ) {
        return bytesToString( dosLongToBytes( uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() ) );
    } // ()

    // -------------------------------------------------------------------------------
// uuid:UUID --> uuidToHexString() --> Text
//
// Convierte un UUID en texto hexadecimal legible.
    // -------------------------------------------------------------------------------
    public static String uuidToHexString ( UUID uuid ) {
        return bytesToHexString( dosLongToBytes( uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() ) );
    } // ()

    // -------------------------------------------------------------------------------
// bytes:byte[] --> bytesToString() --> Text
//
// Convierte bytes en texto; los nulos se ignoran.
    // -------------------------------------------------------------------------------
    public static String bytesToString( byte[] bytes ) {
        if (bytes == null ) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append( (char) b );
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------------
// masSignificativos:Z, menosSignificativos:Z --> dosLongToBytes() --> byte[]
//
// Junta dos long de 8 bytes en un solo bloque de 16 bytes.
    // -------------------------------------------------------------------------------
    public static byte[] dosLongToBytes( long masSignificativos, long menosSignificativos ) {
        ByteBuffer buffer = ByteBuffer.allocate( 2 * Long.BYTES );
        buffer.putLong( masSignificativos );
        buffer.putLong( menosSignificativos );
        return buffer.array();
    }

    // -------------------------------------------------------------------------------
    // Convierte bytes a entero SIN signo en orden big-endian.
    //
    // ATENCION: antes aqui se usaba new BigInteger(bytes).intValue(). Eso interpreta
    //           los bytes como complemento a dos, de modo que cualquier valor con el
    //           bit alto a 1 (>= 32768) salia NEGATIVO. El major y el minor del
    //           iBeacon son campos sin signo, asi que aqui se ensambla el entero
    //           byte a byte.
    // -------------------------------------------------------------------------------
    public static int bytesToInt( byte[] bytes ) {

        if ( bytes == null ) {
            return 0;
        }

        int res = 0;

        for( byte b : bytes ) {
            res = (res << 8) + (b & 0xFF); // 0xFF para quedarse con el byte sin signo
        } // for

        return res;
    }

    // -------------------------------------------------------------------------------
// bytes:byte[] --> bytesToLong() --> Z
//
// Interpreta bytes como un long con signo.
    // -------------------------------------------------------------------------------
    public static long bytesToLong( byte[] bytes ) {
        return new BigInteger(bytes).longValue();
    }

    // -------------------------------------------------------------------------------
// bytes:byte[] --> bytesToIntOK() --> N
//
// Interpreta bytes como un entero sin signo.
    // -------------------------------------------------------------------------------
    public static int bytesToIntOK( byte[] bytes ) {
        if (bytes == null ) {
            return 0;
        }

        if ( bytes.length > 4 ) {
            throw new Error( "demasiados bytes para pasar a int ");
        }
        int res = 0;



        for( byte b : bytes ) {
           /*
           Log.d( MainActivity.ETIQUETA_LOG, "bytesToInt(): byte: hex=" + Integer.toHexString( b )
                   + " dec=" + b + " bin=" + Integer.toBinaryString( b ) +
                   " hex=" + Byte.toString( b )
           );
           */
            res =  (res << 8) // * 16
                    + (b & 0xFF); // para quedarse con 1 byte (2 cuartetos) de lo que haya en b
        } // for

        if ( (bytes[ 0 ] & 0x80) != 0 ) {
            // si tiene signo negativo: el bit mas significativo del primer byte
            // (0x80 = 1000 0000). Antes ponia 0x8, que es el bit 3 y no el signo.
            res = -(~(byte)res)-1; // complemento a 2 (~) de res pero como byte, -1
        }
       /*
        Log.d( MainActivity.ETIQUETA_LOG, "bytesToInt(): res = " + res + " ~res=" + (res ^ 0xffff)
                + "~res=" + ~((byte) res)
        );
        */

        return res;
    } // ()

    // -------------------------------------------------------------------------------
// bytes:byte[] --> bytesToHexString() --> Text
//
// Convierte bytes en texto hexadecimal por byte.
    // -------------------------------------------------------------------------------
    public static String bytesToHexString( byte[] bytes ) {

        if (bytes == null ) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
            sb.append(':');
        }
        return sb.toString();
    } // ()
} // class
// -----------------------------------------------------------------------------------
// Fin del fichero: Utilidades.java
// -----------------------------------------------------------------------------------

