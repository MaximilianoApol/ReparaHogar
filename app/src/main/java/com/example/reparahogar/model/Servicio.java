package com.example.reparahogar.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;


@Entity(tableName = "servicios")
public class Servicio {

    public static final String ESTADO_PENDIENTE   = "PENDIENTE";
    public static final String ESTADO_CONFIRMADO  = "CONFIRMADO";
    public static final String ESTADO_TERMINADO   = "TERMINADO";
    public static final String CAT_PLOMERIA      = "PLOMERIA";
    public static final String CAT_ELECTRICIDAD  = "ELECTRICIDAD";
    public static final String CAT_GAS           = "GAS";

    @PrimaryKey
    @NonNull
    private String id;
    private String clienteUid;
    private String proveedorUid;

    private String categoria;
    private String detalle;
    private String estado;
    private String direccion;
    private String fecha;
    private String hora;

    private long timestampCreacion;


    public Servicio() {}

    public Servicio(@NonNull String id, String clienteUid, String proveedorUid,
                    String categoria, String detalle, String direccion,
                    String fecha, String hora) {
        this.id = id;
        this.clienteUid = clienteUid;
        this.proveedorUid = proveedorUid;
        this.categoria = categoria;
        this.detalle = detalle;
        this.direccion = direccion;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = ESTADO_PENDIENTE;
        this.timestampCreacion = System.currentTimeMillis();
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getClienteUid() { return clienteUid; }
    public void setClienteUid(String clienteUid) { this.clienteUid = clienteUid; }

    public String getProveedorUid() { return proveedorUid; }
    public void setProveedorUid(String proveedorUid) { this.proveedorUid = proveedorUid; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public long getTimestampCreacion() { return timestampCreacion; }
    public void setTimestampCreacion(long timestampCreacion) { this.timestampCreacion = timestampCreacion; }
}