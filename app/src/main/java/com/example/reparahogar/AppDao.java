package com.example.reparahogar;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AppDao {
    // Para el Login y Perfil
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarUsuario(Usuario usuario);

    @Query("SELECT * FROM usuarios WHERE id = :userId")
    Usuario obtenerUsuario(String userId);

    // Para las Citas y Calendario
    @Insert
    void nuevaCita(Cita cita);

    @Query("SELECT * FROM citas ORDER BY fecha DESC")
    List<Cita> obtenerTodasLasCitas();

    @Update
    void actualizarCita(Cita cita);

    @Insert
    void insertarProveedor(Proveedor proveedor);
}
