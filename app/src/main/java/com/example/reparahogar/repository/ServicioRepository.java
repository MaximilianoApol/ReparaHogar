package com.example.reparahogar.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.reparahogar.dao.ServicioDao;
import com.example.reparahogar.database.AppDatabase;
import com.example.reparahogar.model.Servicio;
import com.example.reparahogar.utils.ExecutorUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;


public class ServicioRepository {

    private static final String COLECCION = "servicios";

    private final ServicioDao servicioDao;
    private final FirebaseFirestore firestore;

    // Guardamos la referencia para poder detener el listener cuando la UI muera
    private ListenerRegistration listenerCliente;
    private ListenerRegistration listenerProveedor;

    public ServicioRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.servicioDao = db.servicioDao();
        this.firestore   = FirebaseFirestore.getInstance();
    }

    public LiveData<List<Servicio>> obtenerServiciosCliente(String clienteUid) {
        iniciarListenerCliente(clienteUid);
        return servicioDao.obtenerPorCliente(clienteUid);
    }

    private void iniciarListenerCliente(String clienteUid) {
        // Detener listener anterior si existía
        if (listenerCliente != null) listenerCliente.remove();

        listenerCliente = firestore.collection(COLECCION)
                .whereEqualTo("clienteUid", clienteUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    List<Servicio> servicios = snapshots.toObjects(Servicio.class);
                    ExecutorUtils.getExecutor().execute(() ->
                            servicioDao.insertarLista(servicios));
                });
    }

    public LiveData<List<Servicio>> obtenerServiciosProveedor(String proveedorUid) {
        iniciarListenerProveedor(proveedorUid);
        return servicioDao.obtenerPorProveedor(proveedorUid);
    }

    public LiveData<List<Servicio>> obtenerPendientesHoy(String proveedorUid, String fechaHoy) {
        return servicioDao.obtenerPendientesHoy(proveedorUid, fechaHoy);
    }

    private void iniciarListenerProveedor(String proveedorUid) {
        if (listenerProveedor != null) listenerProveedor.remove();

        listenerProveedor = firestore.collection(COLECCION)
                .whereEqualTo("proveedorUid", proveedorUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    List<Servicio> servicios = snapshots.toObjects(Servicio.class);
                    ExecutorUtils.getExecutor().execute(() ->
                            servicioDao.insertarLista(servicios));
                });
    }


    public void agendar(Servicio servicio, OnResultListener listener) {
        // Generamos el ID desde Firestore para que sea el mismo en Room
        String nuevoId = firestore.collection(COLECCION).document().getId();
        servicio.setId(nuevoId);

        firestore.collection(COLECCION)
                .document(nuevoId)
                .set(servicio)
                .addOnSuccessListener(unused -> {
                    ExecutorUtils.getExecutor().execute(() ->
                            servicioDao.insertar(servicio));
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void cambiarEstado(String servicioId, String nuevoEstado,
                              OnResultListener listener) {
        firestore.collection(COLECCION)
                .document(servicioId)
                .update("estado", nuevoEstado)
                .addOnSuccessListener(unused -> {
                    ExecutorUtils.getExecutor().execute(() ->
                            servicioDao.actualizarEstado(servicioId, nuevoEstado));
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void detenerListeners() {
        if (listenerCliente   != null) listenerCliente.remove();
        if (listenerProveedor != null) listenerProveedor.remove();
    }

    public interface OnResultListener {
        void onSuccess();
        void onError(String mensaje);
    }
}