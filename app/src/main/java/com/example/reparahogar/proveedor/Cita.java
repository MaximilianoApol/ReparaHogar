package com.example.reparahogar.proveedor;


public class Cita {

    private String idCita;
    private String categoria;
    private String fecha;
    private String hora;
    private String estado;
    private String idProveedor;
    private String idCliente;
    private String descripcion;

    public Cita() {}

    // ── Getters y Setters ──

    public String getIdCita()     { return idCita; }
    public void setIdCita(String idCita) { this.idCita = idCita; }

    public String getCategoria()  { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getFecha()      { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora()       { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getEstado()     { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getIdProveedor(){ return idProveedor; }
    public void setIdProveedor(String idProveedor) { this.idProveedor = idProveedor; }

    public String getIdCliente()  { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public String getDescripcion(){ return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}