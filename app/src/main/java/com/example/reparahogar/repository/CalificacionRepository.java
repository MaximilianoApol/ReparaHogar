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

    public void guardar(Calificacion calificacion,
                        OnResultListener listener) {

        ExecutorUtils.getExecutor().execute(() -> {

            int yaCalificado =
                    calificacionDao.contarPorServicio(
                            calificacion.getServicioId());

            if (yaCalificado > 0) {

                if (listener != null) {

                    mainHandler.post(() ->
                            listener.onError(
                                    "Este servicio ya fue calificado"));
                }

                return;
            }

            String nuevoId = firestore
                    .collection(COLECCION)
                    .document()
                    .getId();

            calificacion.setId(nuevoId);

            firestore.collection(COLECCION)
                    .document(nuevoId)
                    .set(calificacion)

                    .addOnSuccessListener(unused -> {

                        ExecutorUtils.getExecutor().execute(() ->
                                calificacionDao.insertar(calificacion));

                        recalcularPromedio(
                                calificacion.getProveedorUid(),
                                listener);
                    })

                    .addOnFailureListener(e -> {

                        if (listener != null) {

                            mainHandler.post(() ->
                                    listener.onError(e.getMessage()));
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