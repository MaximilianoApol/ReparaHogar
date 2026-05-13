package com.example.reparahogar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.reparahogar.model.Proveedor;
import com.example.reparahogar.model.Usuario;
import com.example.reparahogar.proveedor.DetalleProveedor;
import com.example.reparahogar.viewmodel.AuthViewModel;
import com.example.reparahogar.viewmodel.ProveedorViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;

public class VerificacionFragment extends Fragment {

    private AuthViewModel      authViewModel;
    private ProveedorViewModel proveedorViewModel;

    private MaterialCheckBox     checkUbicacion;
    private MaterialButton       btnEnviar;
    private AutoCompleteTextView spinnerServicios;

    private FusedLocationProviderClient fusedLocationClient;
    private double latReal = 0.0, lngReal = 0.0;

    private Usuario usuarioCacheado = null;
    private boolean envioEnProceso  = false;

    private static final String[] SERVICIOS = {
            "Agua / Plomería", "Electricidad", "Gas"
    };



    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine   = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION,   false);
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                    obtenerUbicacionReal();
                } else {
                    Toast.makeText(getContext(),
                            "Permiso de ubicación denegado.",
                            Toast.LENGTH_SHORT).show();
                    checkUbicacion.setChecked(false);
                }
            });



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_verificacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        authViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(AuthViewModel.class);

        proveedorViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(ProveedorViewModel.class);

        // Vistas
        spinnerServicios = view.findViewById(R.id.spinnerServicios);
        checkUbicacion   = view.findViewById(R.id.checkPermisoUbicacion);
        btnEnviar        = view.findViewById(R.id.btnEnviarVerificacion);

        // Ocultar sección de foto (no se usa sin Storage)
        View containerSelfie = view.findViewById(R.id.containerSelfie);
        View labelSelfie     = view.findViewById(R.id.textView);
        if (containerSelfie != null) containerSelfie.setVisibility(View.GONE);
        if (labelSelfie     != null) labelSelfie.setVisibility(View.GONE);


        spinnerServicios.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                SERVICIOS));

        // Checkbox habilita el botón y solicita permiso de ubicación
        btnEnviar.setEnabled(false);
        checkUbicacion.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) solicitarPermisosUbicacion();
            else { latReal = 0.0; lngReal = 0.0; }
            actualizarBoton();
        });

        btnEnviar.setOnClickListener(v -> enviarVerificacion());



        // Cachear usuario para no abrir observer nuevo en cada tap
        authViewModel.getUsuarioActual().observe(getViewLifecycleOwner(), usuario -> {
            if (usuario != null) usuarioCacheado = usuario;
        });

        // Éxito → ir directo a DetalleProveedor
        proveedorViewModel.getOperacionOk().observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok) && envioEnProceso) {
                envioEnProceso = false;
                proveedorViewModel.limpiarOperacionOk();
                irADetalleProveedor();
            }
        });

        // Error
        proveedorViewModel.getErrorMensaje().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                envioEnProceso = false;
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                proveedorViewModel.limpiarError();
                actualizarBoton();
            }
        });
    }

    private void actualizarBoton() {
        btnEnviar.setEnabled(checkUbicacion.isChecked() && !envioEnProceso);
        if (!envioEnProceso) btnEnviar.setText("Enviar para verificación");
    }

    // ── Ubicación ─────────────────────────────────────────────────────────────

    private void solicitarPermisosUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionReal();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressWarnings("MissingPermission")
    private void obtenerUbicacionReal() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        latReal = location.getLatitude();
                        lngReal = location.getLongitude();
                        Toast.makeText(getContext(), "Ubicación detectada ✓",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "No se pudo obtener ubicación; activa el GPS.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void enviarVerificacion() {
        if (envioEnProceso) return;

        String tipoServicioUI = spinnerServicios.getText().toString().trim();
        if (tipoServicioUI.isEmpty()) {
            Toast.makeText(getContext(), "Selecciona un tipo de servicio",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (uid.isEmpty()) {
            Toast.makeText(getContext(), "Sesión expirada, inicia sesión de nuevo",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertir categoría
        String tipoModelo;
        switch (tipoServicioUI) {
            case "Agua / Plomería": tipoModelo = "PLOMERIA";     break;
            case "Electricidad":    tipoModelo = "ELECTRICIDAD"; break;
            case "Gas":             tipoModelo = "GAS";          break;
            default:                tipoModelo = tipoServicioUI.toUpperCase();
        }

        // Construir perfil — verificado=true para que pueda operar de inmediato
        Proveedor perfil = new Proveedor();
        perfil.setUid(uid);
        perfil.setTipoServicio(tipoModelo);
        perfil.setLatitud(latReal);
        perfil.setLongitud(lngReal);
        perfil.setVerificado(true);   // ← automático, sin revisión manual

        if (usuarioCacheado != null) {
            perfil.setNombre(usuarioCacheado.getNombre());
            perfil.setTelefono(usuarioCacheado.getTelefono());
            perfil.setCorreo(usuarioCacheado.getCorreo());
        }

        // También marcar el Usuario como verificado en Firestore
        if (usuarioCacheado != null) {
            usuarioCacheado.setVerificado(true);
            new com.example.reparahogar.repository.UsuarioRepository(requireContext())
                    .actualizar(usuarioCacheado, null);
        }

        envioEnProceso = true;
        btnEnviar.setEnabled(false);
        btnEnviar.setText("Guardando...");

        proveedorViewModel.guardarPerfil(perfil);
    }


    private void irADetalleProveedor() {
        Intent intent = new Intent(requireActivity(), DetalleProveedor.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}