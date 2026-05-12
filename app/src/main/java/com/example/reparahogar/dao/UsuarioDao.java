package com.example.reparahogar.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.reparahogar.model.Usuario;

/**
 * Operaciones de base de datos local para Usuario.
 * OnConflictStrategy.REPLACE: si el usuario ya existe, lo actualiza.
 */
@Dao
public interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Usuario usuario);

    @Update
    void actualizar(Usuario usuario);

    // LiveData para que la UI se actualice sola cuando cambie el dato
    @Query("SELECT * FROM usuarios WHERE uid = :uid LIMIT 1")
    LiveData<Usuario> obtenerPorUid(String uid);

    @Query("SELECT * FROM usuarios WHERE uid = :uid LIMIT 1")
    Usuario obtenerPorUidSync(String uid);  // Versión síncrona para uso en background

    @Query("DELETE FROM usuarios WHERE uid = :uid")
    void eliminar(String uid);
}