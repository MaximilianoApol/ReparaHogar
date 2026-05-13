package com.example.reparahogar.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.reparahogar.model.Proveedor;
import com.example.reparahogar.repository.ProveedorRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.List;

public class ProveedorViewModel extends AndroidViewModel {

    private final ProveedorRepository proveedorRepository;
    private final FusedLocationProviderClient fusedLocationClient;

    // Estados observables
    private final MutableLiveData<Boolean> cargando          = new MutableLiveData<>(false);
    private final MutableLiveData<String>  errorMensaje      = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operacionOk       = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ubicacionObtenida = new MutableLiveData<>();


    private double latitudCliente = 0.0;
    private double longitudCliente = 0.0;

    // LiveData de resultados de búsqueda
    private LiveData<List<Proveedor>> proveedoresCercanos;
    private LiveData<Proveedor> proveedorActual;

    public ProveedorViewModel(@NonNull Application application) {
        super(application);
        proveedorRepository   = new ProveedorRepository(application);
        fusedLocationClient   = LocationServices.getFusedLocationProviderClient(application);
    }

    @SuppressWarnings("MissingPermission")
    public void obtenerUbicacionCliente() {
        cargando.setValue(true);

        // Usamos getCurrentLocation para mayor precisión que getLastLocation
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.getToken()
        ).addOnSuccessListener(location -> {
            cargando.setValue(false);
            if (location != null) {
                latitudCliente  = location.getLatitude();
                longitudCliente = location.getLongitude();
                ubicacionObtenida.setValue(true);
            } else {
                // Sin ubicación disponible (GPS apagado, emulador sin mock location)
                errorMensaje.setValue("No se pudo obtener la ubicación. Verifica que el GPS esté activo.");
            }
        }).addOnFailureListener(e -> {
            cargando.setValue(false);
            errorMensaje.setValue("Error al obtener ubicación: " + e.getMessage());
        });
    }

    public LiveData<List<Proveedor>> buscarProveedoresCercanos(String tipoServicio) {
        if (latitudCliente == 0.0 && longitudCliente == 0.0) {
            errorMensaje.setValue("Primero detecta tu ubicación");
            // Retornamos un LiveData vacío para no crashear la UI
            MutableLiveData<List<Proveedor>> vacio = new MutableLiveData<>();
            return vacio;
        }

        cargando.setValue(true);
        proveedoresCercanos = proveedorRepository.buscarCercanos(
                tipoServicio, latitudCliente, longitudCliente);
        cargando.setValue(false);
        return proveedoresCercanos;
    }


    public LiveData<Proveedor> getProveedorActual(String uid) {
        if (proveedorActual == null) {
            proveedorActual = proveedorRepository.obtenerProveedor(uid);
        }
        return proveedorActual;
    }


    public void guardarPerfil(Proveedor proveedor) {
        cargando.setValue(true);
        proveedorRepository.guardar(proveedor, new ProveedorRepository.OnResultListener() {
            @Override
            public void onSuccess() {
                cargando.setValue(false);
                operacionOk.setValue(true);
            }

            @Override
            public void onError(String mensaje) {
                cargando.setValue(false);
                errorMensaje.setValue(mensaje);
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    public void actualizarUbicacionProveedor(String uid) {
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.getToken()
        ).addOnSuccessListener(location -> {
            if (location != null) {
                proveedorRepository.actualizarUbicacion(
                        uid,
                        location.getLatitude(),
                        location.getLongitude()
                );
            }
            // Si location es null simplemente no actualizamos — no es crítico
        });
        // No mostramos errores al usuario por esta operación — es silenciosa
    }

    public LiveData<Boolean> getCargando()          { return cargando; }
    public LiveData<String>  getErrorMensaje()      { return errorMensaje; }
    public LiveData<Boolean> getOperacionOk()       { return operacionOk; }
    public LiveData<Boolean> getUbicacionObtenida() { return ubicacionObtenida; }

    public double getLatitudCliente()  { return latitudCliente; }
    public double getLongitudCliente() { return longitudCliente; }

    public void limpiarError()       { errorMensaje.setValue(null); }
    public void limpiarOperacionOk() { operacionOk.setValue(null); }
}