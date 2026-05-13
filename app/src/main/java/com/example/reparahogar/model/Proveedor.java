package com.example.reparahogar.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;


@Entity(tableName = "proveedores")
public class Proveedor {

    @PrimaryKey
    @NonNull
    private String uid;

    private String nombre;
    private String descripcion;
    private String telefono;
    private String correo;
    private String fotoUrl;


    private String tipoServicio;

    private float calificacionPromedio;
    private int totalServicios;
    private boolean verificado;

    private double latitud;
    private double longitud;

    public Proveedor() {}

    public Proveedor(@NonNull String uid, String nombre, String descripcion,
                     String telefono, String correo, String tipoServicio) {
        this.uid = uid;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.telefono = telefono;
        this.correo = correo;
        this.tipoServicio = tipoServicio;
        this.calificacionPromedio = 0f;
        this.totalServicios = 0;
        this.verificado = false;

    }


    @NonNull
    public String getUid() { return uid; }
    public void setUid(@NonNull String uid) { this.uid = uid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getTipoServicio() { return tipoServicio; }
    public void setTipoServicio(String tipoServicio) { this.tipoServicio = tipoServicio; }

    public float getCalificacionPromedio() { return calificacionPromedio; }
    public void setCalificacionPromedio(float calificacionPromedio) { this.calificacionPromedio = calificacionPromedio; }

    public int getTotalServicios() { return totalServicios; }
    public void setTotalServicios(int totalServicios) { this.totalServicios = totalServicios; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public boolean isVerificado() { return verificado; }
    public void setVerificado(boolean verificado) { this.verificado = verificado; }
}
