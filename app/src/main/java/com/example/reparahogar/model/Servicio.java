package com.example.reparahogar.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Representa un servicio de mantenimiento solicitado.
 * Firestore: colección "servicios".
 *
 * Estados posibles: "PENDIENTE" → "CONFIRMADO" → "TERMINADO"
 */
@Entity(tableName = "servicios")
public class Servicio {

    // Estados (constantes para evitar strings sueltos en el código)
    public static final String ESTADO_PENDIENTE   = "PENDIENTE";
    public static final String ESTADO_CONFIRMADO  = "CONFIRMADO";
    public static final String ESTADO_TERMINADO   = "TERMINADO";

    // Categorías
    public static final String CAT_PLOMERIA      = "PLOMERIA";
    public static final String CAT_ELECTRICIDAD  = "ELECTRICIDAD";
    public static final String CAT_GAS           = "GAS";

    @PrimaryKey
    @NonNull
    private String id;            // ID generado por Firestore (push ID)

    private String clienteUid;    // UID del cliente que solicita
    private String proveedorUid;  // UID del proveedor asignado

    private String categoria;     // CAT_PLOMERIA / CAT_ELECTRICIDAD / CAT_GAS
    private String detalle;       // Descripción específica del problema
    private String estado;        // PENDIENTE / CONFIRMADO / TERMINADO

    private String direccion;     // Dirección donde se realizará el servicio
    private String fecha;         // Formato: "2024-06-15"
    private String hora;          // Formato: "10:00"

    private long timestampCreacion; // Epoch ms — para ordenar la lista

    // ── Constructor vacío requerido por Room ──
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

    // ── Getters y Setters ──

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