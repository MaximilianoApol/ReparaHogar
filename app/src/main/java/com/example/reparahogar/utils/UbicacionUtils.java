package com.example.reparahogar.utils;

import android.location.Location;


public class UbicacionUtils {

    // Radio de búsqueda inicial. Si no hay resultados, se usa el extendido.
    public static final double RADIO_KM_INICIAL   = 10.0;
    public static final double RADIO_KM_EXTENDIDO = 25.0;

    private UbicacionUtils() {}

    public static double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        float[] resultado = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, resultado);
        return resultado[0] / 1000.0;
    }
}