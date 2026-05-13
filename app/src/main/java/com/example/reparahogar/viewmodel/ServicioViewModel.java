package com.example.reparahogar.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.reparahogar.model.Servicio;
import com.example.reparahogar.repository.ServicioRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ServicioViewModel extends AndroidViewModel {

    private final ServicioRepository servicioRepository;


    private final MutableLiveData<Boolean> cargando     = new MutableLiveData<>(false);
    private final MutableLiveData<String>  errorMensaje = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operacionOk  = new MutableLiveData<>();

    private LiveData<List<Servicio>> serviciosCliente;
    private LiveData<List<Servicio>> serviciosProveedor;
    private LiveData<List<Servicio>> pendientesHoy;

    public ServicioViewModel(@NonNull Application application) {
        super(application);
        servicioRepository = new ServicioRepository(application);
    }


    public LiveData<List<Servicio>> getServiciosCliente(String clienteUid) {
        if (serviciosCliente == null) {
            serviciosCliente = servicioRepository.obtenerServiciosCliente(clienteUid);
        }
        return serviciosCliente;
    }

    public LiveData<List<Servicio>> getServiciosProveedor(String proveedorUid) {
        if (serviciosProveedor == null) {
            serviciosProveedor = servicioRepository.obtenerServiciosProveedor(proveedorUid);
        }
        return serviciosProveedor;
    }


    public LiveData<List<Servicio>> getPendientesHoy(String proveedorUid) {
        if (pendientesHoy == null) {
            String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date());
            pendientesHoy = servicioRepository.obtenerPendientesHoy(proveedorUid, fechaHoy);
        }
        return pendientesHoy;
    }

    public void agendarServicio(String clienteUid, String proveedorUid,
                                String categoria, String detalle,
                                String direccion, String fecha, String hora) {

        if (!validarAgendamiento(direccion, fecha, hora)) return;

        cargando.setValue(true);

        Servicio nuevoServicio = new Servicio(
                "",
                clienteUid,
                proveedorUid,
                categoria,
                detalle,
                direccion,
                fecha,
                hora
        );

        servicioRepository.agendar(nuevoServicio, new ServicioRepository.OnResultListener() {
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

    public void confirmarServicio(String servicioId) {
        cambiarEstado(servicioId, Servicio.ESTADO_CONFIRMADO);
    }

    public void terminarServicio(String servicioId) {
        cambiarEstado(servicioId, Servicio.ESTADO_TERMINADO);
    }

    private void cambiarEstado(String servicioId, String nuevoEstado) {
        cargando.setValue(true);
        servicioRepository.cambiarEstado(servicioId, nuevoEstado,
                new ServicioRepository.OnResultListener() {
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


    private boolean validarAgendamiento(String direccion, String fecha, String hora) {
        if (direccion == null || direccion.trim().isEmpty()) {
            errorMensaje.setValue("Ingresa la dirección del servicio");
            return false;
        }
        if (fecha == null || fecha.isEmpty()) {
            errorMensaje.setValue("Selecciona la fecha del servicio");
            return false;
        }
        if (hora == null || hora.isEmpty()) {
            errorMensaje.setValue("Selecciona la hora del servicio");
            return false;
        }
        return true;
    }



    public LiveData<Boolean> getCargando()     { return cargando; }
    public LiveData<String>  getErrorMensaje() { return errorMensaje; }
    public LiveData<Boolean> getOperacionOk()  { return operacionOk; }

    public void limpiarError()       { errorMensaje.setValue(null); }
    public void limpiarOperacionOk() { operacionOk.setValue(null); }



    @Override
    protected void onCleared() {
        super.onCleared();
        // Detiene los listeners de Firestore cuando el ViewModel muere
        servicioRepository.detenerListeners();
    }
}