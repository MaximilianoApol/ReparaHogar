package com.example.reparahogar.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.reparahogar.model.Calificacion;

@Dao
public interface CalificacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Calificacion calificacion);

    /** Verifica si ya existe calificación para un servicio (evita duplicados). */
    @Query("SELECT COUNT(*) FROM calificaciones WHERE servicioId = :servicioId")
    int contarPorServicio(String servicioId);

    @Query("SELECT * FROM calificaciones WHERE servicioId = :servicioId LIMIT 1")
    Calificacion obtenerPorServicio(String servicioId);
}