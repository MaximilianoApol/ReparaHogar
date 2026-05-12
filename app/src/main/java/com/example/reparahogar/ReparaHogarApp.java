package com.example.reparahogar;

import android.app.Application;

import com.example.reparahogar.utils.SyncManager;

public class ReparaHogarApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicia el monitor de conectividad para sincronizar offline → Firestore
        SyncManager.iniciar(this);
    }
}