package com.example.reparahogar.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.reparahogar.model.Servicio;
import java.util.List;

@Dao
public interface ServicioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Servicio servicio);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarLista(List<Servicio> servicios);

    @Update
    void actualizar(Servicio servicio);

    /** Servicios del cliente, ordenados del más reciente al más antiguo. */
    @Query("SELECT * FROM servicios WHERE clienteUid = :clienteUid ORDER BY timestampCreacion DESC")
    LiveData<List<Servicio>> obtenerPorCliente(String clienteUid);

    /** Servicios del proveedor, ordenados del más reciente al más antiguo. */
    @Query("SELECT * FROM servicios WHERE proveedorUid = :proveedorUid ORDER BY timestampCreacion DESC")
    LiveData<List<Servicio>> obtenerPorProveedor(String proveedorUid);

    /** Pendientes de HOY para el proveedor (para el contador en Home). */
    @Query("SELECT * FROM servicios WHERE proveedorUid = :proveedorUid AND estado = 'PENDIENTE' AND fecha = :fecha")
    LiveData<List<Servicio>> obtenerPendientesHoy(String proveedorUid, String fecha);

    @Query("SELECT * FROM servicios WHERE id = :id LIMIT 1")
    LiveData<Servicio> obtenerPorId(String id);

    @Query("SELECT * FROM servicios WHERE id = :id LIMIT 1")
    Servicio obtenerPorIdSync(String id);

    /** Actualiza solo el estado — útil para el proveedor. */
    @Query("UPDATE servicios SET estado = :estado WHERE id = :id")
    void actualizarEstado(String id, String estado);

    @Query("DELETE FROM servicios WHERE clienteUid = :clienteUid")
    void eliminarPorCliente(String clienteUid);
}