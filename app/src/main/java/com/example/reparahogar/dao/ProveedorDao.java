package com.example.reparahogar.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.reparahogar.model.Proveedor;
import java.util.List;

@Dao
public interface ProveedorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Proveedor proveedor);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarLista(List<Proveedor> proveedores);

    @Update
    void actualizar(Proveedor proveedor);

    @Query("SELECT * FROM proveedores WHERE uid = :uid LIMIT 1")
    LiveData<Proveedor> obtenerPorUid(String uid);

    @Query("SELECT * FROM proveedores WHERE uid = :uid LIMIT 1")
    Proveedor obtenerPorUidSync(String uid);

    @Query("SELECT * FROM proveedores WHERE tipoServicio = :tipo")
    LiveData<List<Proveedor>> obtenerPorTipo(String tipo);

    @Query("DELETE FROM proveedores")
    void eliminarTodos();
}