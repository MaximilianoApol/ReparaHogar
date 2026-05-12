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

/**
 * Maneja los servicios tanto del CLIENTE como del PROVEEDOR.
 *
 * CLIENTE usa:
 *   - getServiciosCliente()   → lista de sus servicios (Home + Notificaciones)
 *   - agendarServicio()       → agendar con el proveedor elegido
 *
 * PROVEEDOR usa:
 *   - getServiciosProveedor() → agenda completa (DetalleProveedor / rvAgendaProveedor)
 *   - getPendientesHoy()      → contador "4 Pendientes" del tvCantidadServicios
 *   - confirmarServicio()     → botón "Confirmar llegada"
 *   - terminarServicio()      → botón "Finalizar Servicio"
 *
 * Reemplaza el acceso directo a Firestore que tenía DetalleProveedor.java
 * y fragment_servicio_confirmado.java (colección "citas" → ahora "servicios").
 */
public class ServicioViewModel extends AndroidViewModel {

    private final ServicioRepository servicioRepository;

    // ── Estados observables ───────────────────────────────────────────────────
    private final MutableLiveData<Boolean> cargando     = new MutableLiveData<>(false);
    private final MutableLiveData<String>  errorMensaje = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operacionOk  = new MutableLiveData<>();

    // Guardamos los LiveData para no crear múltiples observers al rotar pantalla
    private LiveData<List<Servicio>> serviciosCliente;
    private LiveData<List<Servicio>> serviciosProveedor;
    private LiveData<List<Servicio>> pendientesHoy;

    public ServicioViewModel(@NonNull Application application) {
        super(application);
        servicioRepository = new ServicioRepository(application);
    }

    // ── CLIENTE — Lectura ─────────────────────────────────────────────────────

    /**
     * Lista de servicios del cliente.
     * Usada en DetalleHogar (rvMantenimientos) y en NotificacionesFragment.
     * El listener en tiempo real actualiza automáticamente cuando el proveedor
     * cambia el estado.
     */
    public LiveData<List<Servicio>> getServiciosCliente(String clienteUid) {
        if (serviciosCliente == null) {
            serviciosCliente = servicioRepository.obtenerServiciosCliente(clienteUid);
        }
        return serviciosCliente;
    }

    // ── PROVEEDOR — Lectura ───────────────────────────────────────────────────

    /**
     * Agenda completa del proveedor.
     * Usada en DetalleProveedor → rvAgendaProveedor.
     */
    public LiveData<List<Servicio>> getServiciosProveedor(String proveedorUid) {
        if (serviciosProveedor == null) {
            serviciosProveedor = servicioRepository.obtenerServiciosProveedor(proveedorUid);
        }
        return serviciosProveedor;
    }

    /**
     * Servicios PENDIENTES de hoy.
     * Alimenta el tvCantidadServicios: "4 Pendientes" en activity_detalle_proveedor.xml.
     */
    public LiveData<List<Servicio>> getPendientesHoy(String proveedorUid) {
        if (pendientesHoy == null) {
            String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date());
            pendientesHoy = servicioRepository.obtenerPendientesHoy(proveedorUid, fechaHoy);
        }
        return pendientesHoy;
    }

    // ── CLIENTE — Agendar ─────────────────────────────────────────────────────

    /**
     * Agenda un nuevo servicio.
     * Llamado desde fragment_mi_hogar.xml → btnConfirmar "Agendar Servicio".
     *
     * @param clienteUid    UID del cliente autenticado
     * @param proveedorUid  UID del técnico elegido en fragment_tecnicos_disponibles
     * @param categoria     "PLOMERIA" | "ELECTRICIDAD" | "GAS"
     * @param detalle       Especificación elegida en FragmentSeleccionEspecificacion
     *                      (ej: "Fuga de agua", "Cortocircuito"…)
     * @param direccion     Texto del txtDireccionCompleta
     * @param fecha         Del etFechaCita (formato "yyyy-MM-dd")
     * @param hora          Del chipGroupHorarios seleccionado (ej: "09:00")
     */
    public void agendarServicio(String clienteUid, String proveedorUid,
                                String categoria, String detalle,
                                String direccion, String fecha, String hora) {

        if (!validarAgendamiento(direccion, fecha, hora)) return;

        cargando.setValue(true);

        Servicio nuevoServicio = new Servicio(
                "",           // ID lo genera el repository desde Firestore
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

    // ── PROVEEDOR — Cambiar estado ────────────────────────────────────────────

    /**
     * Proveedor confirma llegada al servicio.
     * Corresponde a optionConfirmar en layout_cambio_estado.xml
     * y al botón "Confirmar" en ProveedorAdapter.
     */
    public void confirmarServicio(String servicioId) {
        cambiarEstado(servicioId, Servicio.ESTADO_CONFIRMADO);
    }

    /**
     * Proveedor marca el servicio como terminado.
     * Corresponde a optionFinalizar en layout_cambio_estado.xml
     * y al btnGuardarEstado en fragment_servicio_confirmado2.xml.
     */
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

    // ── Validaciones ──────────────────────────────────────────────────────────

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

    // ── Getters LiveData ──────────────────────────────────────────────────────

    public LiveData<Boolean> getCargando()     { return cargando; }
    public LiveData<String>  getErrorMensaje() { return errorMensaje; }
    public LiveData<Boolean> getOperacionOk()  { return operacionOk; }

    public void limpiarError()       { errorMensaje.setValue(null); }
    public void limpiarOperacionOk() { operacionOk.setValue(null); }

    // ── Limpieza ──────────────────────────────────────────────────────────────

    @Override
    protected void onCleared() {
        super.onCleared();
        // Detiene los listeners de Firestore cuando el ViewModel muere
        servicioRepository.detenerListeners();
    }
}