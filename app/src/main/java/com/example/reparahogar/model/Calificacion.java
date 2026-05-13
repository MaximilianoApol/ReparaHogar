package com.example.reparahogar.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;


@Entity(tableName = "calificaciones")
public class Calificacion {

    @PrimaryKey
    @NonNull
    private String id;

    private String servicioId;
    private String proveedorUid;
    private String clienteUid;
    private int puntuacion;
    private long timestamp;


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