package com.example.reparahogar.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

import com.example.reparahogar.dao.ServicioDao;
import com.example.reparahogar.database.AppDatabase;
import com.example.reparahogar.model.Servicio;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Escucha cambios de conectividad.
 * Cuando el dispositivo recupera la red, sube a Firestore los servicios
 * que fueron guardados solo en Room (modo offline).
 *
 * Uso: llamar SyncManager.iniciar(context) desde Application o MainActivity.
 */
public class SyncManager {

    private static SyncManager instancia;

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final FirebaseFirestore firestore;
    private final ServicioDao servicioDao;

    private SyncManager(Context context) {
        this.context              = context.getApplicationContext();
        this.connectivityManager  = (ConnectivityManager) this.context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        this.firestore            = FirebaseFirestore.getInstance();
        this.servicioDao          = AppDatabase.getInstance(this.context).servicioDao();
    }

    public static void iniciar(Context context) {
        if (instancia == null) {
            instancia = new SyncManager(context);
            instancia.registrarListener();
        }
    }

    private void registrarListener() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(request,
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        // Red disponible: sincronizar
                        sincronizarServiciosPendientes();
                    }
                });
    }

    /**
     * Busca en Room servicios en estado PENDIENTE que no estén en Firestore
     * y los sube. Firestore usa set() con el mismo ID, así que es idempotente
     * (si ya existe, lo sobreescribe sin duplicar).
     *
     * Nota: en esta versión sincronizamos todos los PENDIENTE del usuario actual.
     * Si tu app crece, agrega un campo "sincronizado: boolean" en la entidad
     * para solo subir los que realmente son nuevos.
     */
    private void sincronizarServiciosPendientes() {

        ExecutorUtils.getExecutor().execute(() -> {

            com.google.firebase.auth.FirebaseUser user =
                    com.google.firebase.auth.FirebaseAuth
                            .getInstance()
                            .getCurrentUser();

            if (user == null) return;

            List<Servicio> servicios = AppDatabase.getInstance(context)
                    .servicioDao()
                    .obtenerPorClienteSync(user.getUid());

            if (servicios == null) return;

            for (Servicio s : servicios) {

                if (Servicio.ESTADO_PENDIENTE.equals(s.getEstado())) {
                    subirServicio(s);
                }
            }
        });
    }

    private void subirServicio(Servicio servicio) {
        firestore.collection("servicios")
                .document(servicio.getId())
                .set(servicio);
        // No necesitamos callback: si falla, el próximo reconnect lo reintenta
    }
}