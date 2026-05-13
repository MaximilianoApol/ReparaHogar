package com.example.reparahogar.repository;

import android.content.Context;

import com.example.reparahogar.dao.CalificacionDao;
import com.example.reparahogar.dao.ProveedorDao;
import com.example.reparahogar.database.AppDatabase;
import com.example.reparahogar.model.Calificacion;
import com.example.reparahogar.model.Proveedor;
import com.example.reparahogar.utils.ExecutorUtils;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;

public class CalificacionRepository {

    private static final String COLECCION = "calificaciones";

    private final CalificacionDao calificacionDao;
    private final ProveedorDao    proveedorDao;
    private final FirebaseFirestore firestore;
    private final ProveedorRepository proveedorRepository;

    public CalificacionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.calificacionDao       = db.calificacionDao();
        this.proveedorDao          = db.proveedorDao();
        this.firestore             = FirebaseFirestore.getInstance();
        this.proveedorRepository   = new ProveedorRepository(context);
    }

    /**
     * Guarda la calificación y actualiza el promedio del proveedor.
     *
     * @param calificacion Objeto con puntuacion, proveedorUid, clienteUid, servicioId
     * @param listener     Resultado de la operación
     */
    public void guardar(Calificacion calificacion, OnResultListener listener) {
        // Verificar si ya existe una calificación para este servicio (Room es rápido)
        ExecutorUtils.getExecutor().execute(() -> {
            int yaCalificado = calificacionDao.contarPorServicio(calificacion.getServicioId());
            if (yaCalificado > 0) {
                if (listener != null) listener.onError("Este servicio ya fue calificado");
                return;
            }

            // Generar ID desde Firestore
            String nuevoId = firestore.collection(COLECCION).document().getId();
            calificacion.setId(nuevoId);

            // Guardar en Firestore
            firestore.collection(COLECCION)
                    .document(nuevoId)
                    .set(calificacion)
                    .addOnSuccessListener(unused -> {
                        // Guardar en Room
                        ExecutorUtils.getExecutor().execute(() ->
                                calificacionDao.insertar(calificacion));

                        // Recalcular promedio desde Firestore
                        recalcularPromedio(calificacion.getProveedorUid(), listener);
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) listener.onError(e.getMessage());
                    });
        });
    }

    /**
     * Recalcula el promedio consultando todas las calificaciones del proveedor.
     * Usa una query de agregación de Firestore (no trae todos los documentos).
     */
    private void recalcularPromedio(String proveedorUid, OnResultListener listener) {
        firestore.collection(COLECCION)
                .whereEqualTo("proveedorUid", proveedorUid)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) return;

                    // Calcular promedio manualmente (Firestore no tiene AVG en SDK Android)
                    float suma = 0;
                    int total = query.size();
                    for (Calificacion c : query.toObjects(Calificacion.class)) {
                        suma += c.getPuntuacion();
                    }
                    float promedio = suma / total;

                    // Actualizar proveedor con nuevo promedio
                    proveedorRepository.actualizarCalificacion(proveedorUid, promedio, total);

                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    // Calificación guardada aunque falle el recálculo
                    if (listener != null) listener.onSuccess();
                });
    }

    // ── Callback ──────────────────────────────────────────────────────────────

    public interface OnResultListener {
        void onSuccess();
        void onError(String mensaje);
    }
}