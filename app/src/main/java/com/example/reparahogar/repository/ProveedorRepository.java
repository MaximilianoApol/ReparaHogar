package com.example.reparahogar.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.reparahogar.dao.ProveedorDao;
import com.example.reparahogar.database.AppDatabase;
import com.example.reparahogar.model.Proveedor;
import com.example.reparahogar.utils.ExecutorUtils;
import com.example.reparahogar.utils.UbicacionUtils;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;


public class ProveedorRepository {

    private static final String COLECCION = "proveedores";

    private final ProveedorDao proveedorDao;
    private final FirebaseFirestore firestore;

    public ProveedorRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.proveedorDao = db.proveedorDao();
        this.firestore    = FirebaseFirestore.getInstance();
    }

    // ── Búsqueda por cercanía ─────────────────────────────────────────────────

    /**
     * Busca proveedores cercanos al cliente.
     *
     * @param tipoServicio  "PLOMERIA", "ELECTRICIDAD" o "GAS"
     * @param clienteLat    Latitud del cliente
     * @param clienteLon    Longitud del cliente
     * @return LiveData con la lista filtrada por distancia
     */
    public LiveData<List<Proveedor>> buscarCercanos(String tipoServicio,
                                                    double clienteLat,
                                                    double clienteLon) {
        MutableLiveData<List<Proveedor>> resultado = new MutableLiveData<>();

        firestore.collection(COLECCION)
                .whereEqualTo("tipoServicio", tipoServicio)
                .get()
                .addOnSuccessListener(query -> {
                    List<Proveedor> todos = query.toObjects(Proveedor.class);
                    List<Proveedor> cercanos = filtrarPorRadio(
                            todos, clienteLat, clienteLon,
                            UbicacionUtils.RADIO_KM_INICIAL);

                    // Si no hay nadie en 10 km, ampliar a 25 km
                    if (cercanos.isEmpty()) {
                        cercanos = filtrarPorRadio(
                                todos, clienteLat, clienteLon,
                                UbicacionUtils.RADIO_KM_EXTENDIDO);
                    }

                    List<Proveedor> resultadoFinal = cercanos;

                    // Cachear en Room
                    ExecutorUtils.getExecutor().execute(() ->
                            proveedorDao.insertarLista(resultadoFinal));

                    resultado.postValue(resultadoFinal);
                })
                .addOnFailureListener(e -> {
                    // Sin red: devolver caché local por tipo de servicio
                    // Room devuelve LiveData; aquí necesitamos el valor sync
                    ExecutorUtils.getExecutor().execute(() -> {
                        // No podemos hacer un get() sync de LiveData aquí,
                        // así que emitimos lista vacía y la UI muestra el caché
                        // que ya tiene el RecyclerView de Room.
                        resultado.postValue(new ArrayList<>());
                    });
                });

        return resultado;
    }

    private List<Proveedor> filtrarPorRadio(List<Proveedor> todos,
                                            double clienteLat, double clienteLon,
                                            double radioKm) {
        List<Proveedor> filtrados = new ArrayList<>();
        for (Proveedor p : todos) {
            double distancia = UbicacionUtils.calcularDistanciaKm(
                    clienteLat, clienteLon, p.getLatitud(), p.getLongitud());
            if (distancia <= radioKm) {
                filtrados.add(p);
            }
        }
        return filtrados;
    }

    // ── Lectura por UID ───────────────────────────────────────────────────────

    public LiveData<Proveedor> obtenerProveedor(String uid) {
        cargarDesdeFirestore(uid);
        return proveedorDao.obtenerPorUid(uid);
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    /**
     * Guarda el perfil del proveedor (tras verificación).
     */
    public void guardar(Proveedor proveedor, OnResultListener listener) {
        firestore.collection(COLECCION)
                .document(proveedor.getUid())
                .set(proveedor)
                .addOnSuccessListener(unused -> {
                    ExecutorUtils.getExecutor().execute(() ->
                            proveedorDao.insertar(proveedor));
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    /**
     * Actualiza la ubicación del proveedor en Firestore y Room.
     * Se llama cada vez que el proveedor abre la app.
     */
    public void actualizarUbicacion(String uid, double latitud, double longitud) {
        // Firestore: actualización parcial del documento (solo los dos campos)
        firestore.collection(COLECCION)
                .document(uid)
                .update("latitud", latitud, "longitud", longitud);

        // Room: leer, modificar y guardar en background
        ExecutorUtils.getExecutor().execute(() -> {
            Proveedor p = proveedorDao.obtenerPorUidSync(uid);
            if (p != null) {
                p.setLatitud(latitud);
                p.setLongitud(longitud);
                proveedorDao.actualizar(p);
            }
        });
    }

    /**
     * Recalcula y guarda el promedio de calificación del proveedor.
     * Se llama desde CalificacionRepository tras guardar una nueva calificación.
     */
    public void actualizarCalificacion(String uid, float nuevoPromedio, int totalServicios) {
        firestore.collection(COLECCION)
                .document(uid)
                .update("calificacionPromedio", nuevoPromedio,
                        "totalServicios", totalServicios);

        ExecutorUtils.getExecutor().execute(() -> {
            Proveedor p = proveedorDao.obtenerPorUidSync(uid);
            if (p != null) {
                p.setCalificacionPromedio(nuevoPromedio);
                p.setTotalServicios(totalServicios);
                proveedorDao.actualizar(p);
            }
        });
    }

    // ── Sincronización desde Firestore → Room ────────────────────────────────

    private void cargarDesdeFirestore(String uid) {
        firestore.collection(COLECCION)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Proveedor p = doc.toObject(Proveedor.class);
                        if (p != null) {
                            ExecutorUtils.getExecutor().execute(() ->
                                    proveedorDao.insertar(p));
                        }
                    }
                });
    }

    // ── Callback ──────────────────────────────────────────────────────────────

    public interface OnResultListener {
        void onSuccess();
        void onError(String mensaje);
    }
}