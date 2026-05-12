package com.example.reparahogar.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pool de hilos compartido para operaciones en background (Room, Firestore).
 * Un solo ExecutorService para toda la app evita crear hilos innecesarios.
 */
public class ExecutorUtils {

    // 4 hilos es suficiente para operaciones de BD y red
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    private ExecutorUtils() {}

    public static ExecutorService getExecutor() {
        return executor;
    }
}