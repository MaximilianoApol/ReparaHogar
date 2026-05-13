package com.example.reparahogar.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.reparahogar.model.Calificacion;
import com.example.reparahogar.repository.CalificacionRepository;


public class CalificacionViewModel extends AndroidViewModel {

    private final CalificacionRepository calificacionRepository;

    private final MutableLiveData<Boolean> cargando          = new MutableLiveData<>(false);
    private final MutableLiveData<String>  errorMensaje      = new MutableLiveData<>();
    private final MutableLiveData<Boolean> calificacionGuardada = new MutableLiveData<>();

    public CalificacionViewModel(@NonNull Application application) {
        super(application);
        calificacionRepository = new CalificacionRepository(application);
    }


    public void guardarCalificacion(String servicioId, String proveedorUid,
                                    String clienteUid, int puntuacion) {

        if (!validar(servicioId, proveedorUid, clienteUid, puntuacion)) return;

        cargando.setValue(true);

        Calificacion calificacion = new Calificacion(
                "",           // ID lo genera el repository desde Firestore
                servicioId,
                proveedorUid,
                clienteUid,
                puntuacion
        );

        calificacionRepository.guardar(calificacion,
                new CalificacionRepository.OnResultListener() {
                    @Override
                    public void onSuccess() {
                        cargando.setValue(false);
                        calificacionGuardada.setValue(true);
                    }

                    @Override
                    public void onError(String mensaje) {
                        cargando.setValue(false);
                        errorMensaje.setValue(mensaje);
                    }
                });
    }

    private boolean validar(String servicioId, String proveedorUid,
                            String clienteUid, int puntuacion) {
        if (servicioId == null || servicioId.isEmpty()) {
            errorMensaje.setValue("Servicio no identificado");
            return false;
        }
        if (proveedorUid == null || proveedorUid.isEmpty()) {
            errorMensaje.setValue("Proveedor no identificado");
            return false;
        }
        if (clienteUid == null || clienteUid.isEmpty()) {
            errorMensaje.setValue("Usuario no identificado");
            return false;
        }
        if (puntuacion < 1 || puntuacion > 5) {
            errorMensaje.setValue("La calificación debe ser entre 1 y 5");
            return false;
        }
        return true;
    }

    public LiveData<Boolean> getCargando()             { return cargando; }
    public LiveData<String>  getErrorMensaje()         { return errorMensaje; }
    public LiveData<Boolean> getCalificacionGuardada() { return calificacionGuardada; }

    public void limpiarError()            { errorMensaje.setValue(null); }
    public void limpiarCalificacion()     { calificacionGuardada.setValue(null); }
}