package com.example.reparahogar;

import android.content.Context;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DataRepository {
    private AppDao appDao;
    private FirebaseHelper firebaseHelper;
    private Executor executor = Executors.newSingleThreadExecutor();

    public DataRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        appDao = db.appDao();
        firebaseHelper = new FirebaseHelper();
    }

    // --- OPERACIONES DE USUARIO ---

    public void registrarUsuarioCompleto(Usuario usuario) {
        // 1. Guardar en Local (Room)
        executor.execute(() -> {
            appDao.insertarUsuario(usuario);
        });

        // 2. Guardar en Nube (Firestore)
        firebaseHelper.guardarUsuarioNube(usuario);
    }

    // --- OPERACIONES DE CITAS ---

    public void crearNuevaCita(Cita cita) {
        // 1. Guardar en Local inmediatamente para soporte offline
        executor.execute(() -> {
            appDao.nuevaCita(cita);
        });

        // 2. Sincronizar con la nube
        firebaseHelper.guardarCitaNube(cita);
    }

    public List<Cita> obtenerCalendarioLocal() {
        // Esto lo usaremos para llenar el CalendarView rápido
        return appDao.obtenerTodasLasCitas();
    }
}