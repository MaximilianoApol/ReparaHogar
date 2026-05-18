package com.example.reparahogar.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.reparahogar.dao.CalificacionDao;
import com.example.reparahogar.dao.ProveedorDao;
import com.example.reparahogar.database.AppDatabase;
import com.example.reparahogar.model.Calificacion;
import com.example.reparahogar.utils.ExecutorUtils;
import com.google.firebase.firestore.FirebaseFirestore;

public class CalificacionRepository {

    private static final String COLECCION = "calificaciones";

    private final CalificacionDao calificacionDao;
    private final ProveedorDao proveedorDao;
    private final FirebaseFirestore firestore;
    private final ProveedorRepository proveedorRepository;

    // Handler para regresar al hilo principal
    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public CalificacionRepository(Context context) {

        AppDatabase db = AppDatabase.getInstance(context);

        this.calificacionDao = db.calificacionDao();
        this.proveedorDao = db.proveedorDao();

        this.firestore = FirebaseFirestore.getInstance();

        this.proveedorRepository =
                new ProveedorRepository(context);
    }

    public void guardar(Calificacion calificacion, OnResultListener listener) {
        ExecutorUtils.getExecutor().execute(() -> {

            // Primero verificar en Firestore (fuente de verdad), no solo en Room
            firestore.collection(COLECCION)
                    .whereEqualTo("servicioId", calificacion.getServicioId())
                    .whereEqualTo("clienteUid", calificacion.getClienteUid())
                    .get()
                    .addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            // Ya existe en Firestore → sincronizar a Room y bloquear
                            Calificacion existente = query.toObjects(Calificacion.class).get(0);
                            ExecutorUtils.getExecutor().execute(() ->
                                    calificacionDao.insertar(existente));

                            if (listener != null) {
                                mainHandler.post(() ->
                                        listener.onError("Este servicio ya fue calificado"));
                            }
                            return;
                        }

                        // No existe → guardar normalmente
                        String nuevoId = firestore.collection(COLECCION).document().getId();
                        calificacion.setId(nuevoId);

                        firestore.collection(COLECCION)
                                .document(nuevoId)
                                .set(calificacion)
                                .addOnSuccessListener(unused -> {
                                    ExecutorUtils.getExecutor().execute(() ->
                                            calificacionDao.insertar(calificacion));
                                    recalcularPromedio(calificacion.getProveedorUid(), listener);
                                })
                                .addOnFailureListener(e -> {
                                    if (listener != null) {
                                        mainHandler.post(() -> listener.onError(e.getMessage()));
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) {
                            mainHandler.post(() ->
                                    listener.onError("Error al verificar calificación: " + e.getMessage()));
                        }
                    });
        });
    }
    private void recalcularPromedio(String proveedorUid,
                                    OnResultListener listener) {

        firestore.collection(COLECCION)
                .whereEqualTo("proveedorUid", proveedorUid)
                .get()

                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {

                        if (listener != null) {

                            mainHandler.post(listener::onSuccess);
                        }

                        return;
                    }

                    float suma = 0;

                    int total = query.size();

                    for (Calificacion c :
                            query.toObjects(Calificacion.class)) {

                        suma += c.getPuntuacion();
                    }

                    float promedio = suma / total;

                    proveedorRepository.actualizarCalificacion(
                            proveedorUid,
                            promedio,
                            total
                    );

                    if (listener != null) {

                        mainHandler.post(listener::onSuccess);
                    }
                })

                .addOnFailureListener(e -> {

                    // La calificación sí se guardó
                    if (listener != null) {

                        mainHandler.post(listener::onSuccess);
                    }
                });
    }


    public interface OnResultListener {

        void onSuccess();

        void onError(String mensaje);
    }
}