package com.example.reparahogar.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Calificación que el cliente da al proveedor al terminar un servicio.
 * Firestore: colección "calificaciones".
 * Rango de puntuación: 1 – 5.
 */
@Entity(tableName = "calificaciones")
public class Calificacion {

    @PrimaryKey
    @NonNull
    private String id;           // ID generado por Firestore

    private String servicioId;   // Referencia al servicio calificado
    private String proveedorUid; // A quién se califica
    private String clienteUid;   // Quién califica

    private int puntuacion;      // 1 – 5
    private long timestamp;      // Epoch ms

    // ── Constructor vacío requerido por Room ──
    public Calificacion() {}

    public Calificacion(@NonNull String id, String servicioId,
                        String proveedorUid, String clienteUid, int puntuacion) {
        this.id = id;
        this.servicioId = servicioId;
        this.proveedorUid = proveedorUid;
        this.clienteUid = clienteUid;
        this.puntuacion = puntuacion;
        this.timestamp = System.currentTimeMillis();
    }

    // ── Getters y Setters ──

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getServicioId() { return servicioId; }
    public void setServicioId(String servicioId) { this.servicioId = servicioId; }

    public String getProveedorUid() { return proveedorUid; }
    public void setProveedorUid(String proveedorUid) { this.proveedorUid = proveedorUid; }

    public String getClienteUid() { return clienteUid; }
    public void setClienteUid(String clienteUid) { this.clienteUid = clienteUid; }

    public int getPuntuacion() { return puntuacion; }
    public void setPuntuacion(int puntuacion) { this.puntuacion = puntuacion; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}