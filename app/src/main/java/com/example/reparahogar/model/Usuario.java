package com.example.reparahogar.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Representa a un usuario registrado.
 * Puede ser CLIENTE o PROVEEDOR (campo tipoUsuario).
 * Se almacena localmente en Room y se sincroniza con Firestore (colección "users").
 */
@Entity(tableName = "usuarios")
public class Usuario {

    @PrimaryKey
    @NonNull
    private String uid;          // UID de Firebase Auth (mismo en Firestore)

    private String nombre;
    private String correo;
    private String telefono;
    private String fotoUrl;      // URL en Firebase Storage (puede ser null)
    private String tipoUsuario;  // "CLIENTE" o "PROVEEDOR"
    private boolean verificado;
    private double latitud;
    private double longitud;// Solo relevante para PROVEEDOR

    // ── Constructor vacío requerido por Room ──
    public Usuario() {}

    public Usuario(@NonNull String uid, String nombre, String correo,
                   String telefono, String tipoUsuario) {
        this.uid = uid;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.tipoUsuario = tipoUsuario;
        this.verificado = false;
    }

    // ── Getters y Setters ──

    @NonNull
    public String getUid() { return uid; }
    public void setUid(@NonNull String uid) { this.uid = uid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public boolean isVerificado() { return verificado; }
    public void setVerificado(boolean verificado) { this.verificado = verificado; }
    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
}