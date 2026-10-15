package org.jordi.pruebarest1;

import java.util.Arrays;

// -----------------------------------------------------------------------------------
// @author: Jordi Bataller i Mascarell
// -----------------------------------------------------------------------------------
public class TramaIBeacon {
    private byte[] prefijo = null; // 9 bytes
    private byte[] uuid = null; // 16 bytes
    private byte[] major = null; // 2 bytes
    private byte[] minor = null; // 2 bytes
    private byte txPower = 0; // 1 byte

    private byte[] losBytes;

    private byte[] advFlags = null; // 3 bytes
    private byte[] advHeader = null; // 2 bytes
    private byte[] companyID = new byte[2]; // 2 bytes
    private byte iBeaconType = 0 ; // 1 byte
    private byte iBeaconLength = 0 ; // 1 byte

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public byte[] getPrefijo() {
        return prefijo;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public byte[] getUUID() {
        return uuid;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public byte[] getMajor() {
        return major;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public byte[] getMinor() {
        return minor;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public byte getTxPower() {
        return txPower;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public byte[] getLosBytes() {
        return losBytes;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public byte[] getAdvFlags() {
        return advFlags;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public byte[] getAdvHeader() {
        return advHeader;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public byte[] getCompanyID() {
        return companyID;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public byte getiBeaconType() {
        return iBeaconType;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public byte getiBeaconLength() {
        return iBeaconLength;
    }

    // -------------------------------------------------------------------------------
    // Devuelve el UUID como texto plano, por ejemplo "EPSG-GTI-PROY-3A".
    //
    // Es la forma de distinguir el beacon de medidas (UUID fijo del proyecto) de
    // cualquier otro iBeacon que ande por ahí, como el beacon libre que emite el
    // Arduino con la cadena "MolaMola...".
    // -------------------------------------------------------------------------------
    public String getUUIDComoTexto() {
        return Utilidades.bytesToString( this.uuid );
    }

    // -------------------------------------------------------------------------------
    // Comprueba que los bytes recibidos son realmente una trama iBeacon bien formada:
    //
    //   - al menos 30 bytes (cabecera + UUID + major + minor + txPower)
    //   - companyID = Apple (0x004C, en little-endian)
    //   - tipo de beacon = 0x02
    //   - longitud de la carga = 21 bytes
    //
    // Sin esto, si llega un anuncio normal (con nombre pero sin trama iBeacon) los
    // indices de TramaIBeacon se salen de rango y la app revienta.
    // -------------------------------------------------------------------------------
    public boolean esIBeacon() {

        if ( this.losBytes == null || this.losBytes.length < 30 ) {
            return false;
        }

        if ( this.companyID == null || this.companyID.length < 2 ) {
            return false;
        }

        return ( ( this.companyID[ 0 ] & 0xFF ) == 0x4C )    // Apple, byte bajo
                && ( ( this.companyID[ 1 ] & 0xFF ) == 0x00 ) // Apple, byte alto
                && ( this.iBeaconType == 0x02 )
                && ( this.iBeaconLength == 21 );
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public TramaIBeacon(byte[] bytes ) {
        this.losBytes = bytes;

        prefijo = Arrays.copyOfRange(losBytes, 0, 8+1 ); // 9 bytes
        uuid = Arrays.copyOfRange(losBytes, 9, 24+1 ); // 16 bytes
        major = Arrays.copyOfRange(losBytes, 25, 26+1 ); // 2 bytes
        minor = Arrays.copyOfRange(losBytes, 27, 28+1 ); // 2 bytes
        txPower = losBytes[ 29 ]; // 1 byte

        advFlags = Arrays.copyOfRange( prefijo, 0, 2+1 ); // 3 bytes
        advHeader = Arrays.copyOfRange( prefijo, 3, 4+1 ); // 2 bytes
        companyID = Arrays.copyOfRange( prefijo, 5, 6+1 ); // 2 bytes
        iBeaconType = prefijo[ 7 ]; // 1 byte
        iBeaconLength = prefijo[ 8 ]; // 1 byte

    } // ()
} // class
// -----------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------

