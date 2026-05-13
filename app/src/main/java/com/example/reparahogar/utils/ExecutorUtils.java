package com.example.reparahogar.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ExecutorUtils {

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    private ExecutorUtils() {}

    public static ExecutorService getExecutor() {
        return executor;
    }
}