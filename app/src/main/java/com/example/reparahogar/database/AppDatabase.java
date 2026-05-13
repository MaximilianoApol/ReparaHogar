package com.example.reparahogar.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.reparahogar.dao.CalificacionDao;
import com.example.reparahogar.dao.ProveedorDao;
import com.example.reparahogar.dao.ServicioDao;
import com.example.reparahogar.dao.UsuarioDao;
import com.example.reparahogar.model.Calificacion;
import com.example.reparahogar.model.Proveedor;
import com.example.reparahogar.model.Servicio;
import com.example.reparahogar.model.Usuario;


@Database(
        entities = {
                Usuario.class,
                Proveedor.class,
                Servicio.class,
                Calificacion.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instancia;
    private static final String NOMBRE_BD = "reparahogar_db";

    // ── DAOs ──
    public abstract UsuarioDao usuarioDao();
    public abstract ProveedorDao proveedorDao();
    public abstract ServicioDao servicioDao();
    public abstract CalificacionDao calificacionDao();


    public static AppDatabase getInstance(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    NOMBRE_BD
                            )
                            .fallbackToDestructiveMigration() // En desarrollo está bien;
                            // en producción usa Migrations.
                            .build();
                }
            }
        }
        return instancia;
    }
}