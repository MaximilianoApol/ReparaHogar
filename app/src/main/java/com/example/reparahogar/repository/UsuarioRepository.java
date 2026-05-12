package com.example.reparahogar.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.reparahogar.dao.UsuarioDao;
import com.example.reparahogar.database.AppDatabase;
import com.example.reparahogar.model.Usuario;
import com.example.reparahogar.utils.ExecutorUtils;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Fuente única de verdad para Usuario.
 *
 * Estrategia:
 *  - Lectura: Room (LiveData, reactivo). Firestore alimenta Room en background.
 *  - Escritura: primero Firestore, luego Room si tiene éxito.
 */
public class UsuarioRepository {

    private static final String COLECCION = "users";

    private final UsuarioDao usuarioDao;
    private final FirebaseFirestore firestore;

    public UsuarioRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.usuarioDao = db.usuarioDao();
        this.firestore  = FirebaseFirestore.getInstance();
    }

    // ── Lectura ──────────────────────────────────────────────────────────────

    /**
     * Observa el usuario local. La UI se actualiza automáticamente
     * cuando Room recibe cambios (por ejemplo, tras sincronizar con Firestore).
     */
    public LiveData<Usuario> obtenerUsuario(String uid) {
        // Dispara la carga desde Firestore en paralelo
        cargarDesdeFirestore(uid);
        return usuarioDao.obtenerPorUid(uid);
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    /**
     * Guarda un usuario nuevo (registro).
     * Escribe en Firestore y, si tiene éxito, en Room.
     */
    public void guardar(Usuario usuario, OnResultListener listener) {
        firestore.collection(COLECCION)
                .document(usuario.getUid())
                .set(usuario)
                .addOnSuccessListener(unused -> {
                    // Guarda en Room en background
                    ExecutorUtils.getExecutor().execute(() ->
                            usuarioDao.insertar(usuario));
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    /**
     * Actualiza datos del perfil (nombre, teléfono, foto, ubicación…).
     */
    public void actualizar(Usuario usuario, OnResultListener listener) {
        firestore.collection(COLECCION)
                .document(usuario.getUid())
                .set(usuario)
                .addOnSuccessListener(unused -> {
                    ExecutorUtils.getExecutor().execute(() ->
                            usuarioDao.actualizar(usuario));
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    // ── Sincronización desde Firestore → Room ────────────────────────────────

    private void cargarDesdeFirestore(String uid) {
        firestore.collection(COLECCION)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Usuario usuario = doc.toObject(Usuario.class);
                        if (usuario != null) {
                            ExecutorUtils.getExecutor().execute(() ->
                                    usuarioDao.insertar(usuario));
                        }
                    }
                });
        // Si falla la red, Room ya tiene el último dato cacheado — no hay problema.
    }

    // ── Callback simple ──────────────────────────────────────────────────────

    public interface OnResultListener {
        void onSuccess();
        void onError(String mensaje);
    }
}