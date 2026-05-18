package com.example.reparahogar.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.reparahogar.model.Calificacion;

import java.util.List;

@Dao
public interface CalificacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Calificacion calificacion);


    @Query("SELECT COUNT(*) FROM calificaciones WHERE servicioId = :servicioId")
    int contarPorServicio(String servicioId);

    @Query("SELECT * FROM calificaciones WHERE servicioId = :servicioId LIMIT 1")
    Calificacion obtenerPorServicio(String servicioId);
    @Query("SELECT servicioId FROM calificaciones WHERE clienteUid = :clienteUid")
    List<String> obtenerIdsCalificadosPorCliente(String clienteUid);

}