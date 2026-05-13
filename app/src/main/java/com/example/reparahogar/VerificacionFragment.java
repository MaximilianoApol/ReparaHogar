package com.example.reparahogar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import com.example.reparahogar.viewmodel.AuthViewModel;
import com.example.reparahogar.viewmodel.ProveedorViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Pantalla de verificación del proveedor. Permite:
 *  1. Agregar foto de perfil (galería)
 *  2. Seleccionar tipo de servicio (spinner)
 *  3. Autorizar ubicación en tiempo real
 *  4. Enviar para verificación → guarda en Firestore y Room
 */
public class VerificacionFragment extends Fragment {

    private AuthViewModel     authViewModel;
    private ProveedorViewModel proveedorViewModel;

    private MaterialCheckBox   checkUbicacion;
    private MaterialButton     btnEnviar;
    private AutoCompleteTextView spinnerServicios;
    private ShapeableImageView imgSelfie;

    private FusedLocationProviderClient fusedLocationClient;
    private double latReal = 0.0, lngReal = 0.0;
    private Uri    fotoUri  = null;

    private static final String[] SERVICIOS = {
            "Agua / Plomería", "Electricidad", "Gas"
    };

    // ── Launchers ─────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    fotoUri = uri;
                    imgSelfie.setImageURI(uri);
                }
            });

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine   = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION,   false);
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                    obtenerUbicacionReal();
                } else {
                    Toast.makeText(getContext(),
                            "Permiso de ubicación denegado. No aparecerás en búsquedas cercanas.",
                            Toast.LENGTH_LONG).show();
                    checkUbicacion.setChecked(false);
                }
            });

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

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

        // ViewModels (comparten con MainActivity / AuthViewModel)
        authViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(AuthViewModel.class);

        proveedorViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(ProveedorViewModel.class);

        // Vistas
        imgSelfie       = view.findViewById(R.id.imgSelfie);
        spinnerServicios= view.findViewById(R.id.spinnerServicios);
        checkUbicacion  = view.findViewById(R.id.checkPermisoUbicacion);
        btnEnviar       = view.findViewById(R.id.btnEnviarVerificacion);

        // Spinner con tipos de servicio
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                SERVICIOS);
        spinnerServicios.setAdapter(spinnerAdapter);

        // El botón de envío requiere que el checkbox esté marcado
        btnEnviar.setEnabled(false);
        checkUbicacion.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                solicitarPermisosUbicacion();
            } else {
                latReal = 0.0;
                lngReal = 0.0;
            }
            btnEnviar.setEnabled(checked);
        });

        // Tap en foto → abrir galería
        imgSelfie.setOnClickListener(v ->
                pickImageLauncher.launch("image/*"));

        // Botón enviar
        btnEnviar.setOnClickListener(v -> enviarVerificacion());

        // Observar errores / carga
        proveedorViewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            btnEnviar.setEnabled(!cargando && checkUbicacion.isChecked());
            btnEnviar.setText(cargando ? "Enviando..." : "Enviar para verificación");
        });

        proveedorViewModel.getOperacionOk().observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Toast.makeText(getContext(),
                        "Perfil enviado. Pronto será verificado.", Toast.LENGTH_LONG).show();
                proveedorViewModel.limpiarOperacionOk();
                // Navegar a DetalleProveedor cuando el admin verifique;
                // por ahora regresamos al login limpio
                authViewModel.cerrarSesion();
            }
        });

        proveedorViewModel.getErrorMensaje().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                proveedorViewModel.limpiarError();
            }
        });
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
                        Toast.makeText(getContext(),
                                "Ubicación detectada ✓", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "No se pudo obtener ubicación; activa el GPS.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Envío ──────────────────────────────────────────────────────────────────

    private void enviarVerificacion() {
        String tipoServicioUI = spinnerServicios.getText().toString().trim();
        if (tipoServicioUI.isEmpty()) {
            Toast.makeText(getContext(), "Selecciona un tipo de servicio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fotoUri == null) {
            Toast.makeText(getContext(), "Agrega una foto de perfil", Toast.LENGTH_SHORT).show();
            return;
        }

        String tipoModelo;
        switch (tipoServicioUI) {
            case "Agua / Plomería": tipoModelo = "PLOMERIA";     break;
            case "Electricidad":    tipoModelo = "ELECTRICIDAD"; break;
            case "Gas":             tipoModelo = "GAS";          break;
            default:                tipoModelo = tipoServicioUI.toUpperCase();
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (uid.isEmpty()) {
            Toast.makeText(getContext(), "Sesión expirada", Toast.LENGTH_SHORT).show();
            return;
        }

        Proveedor perfil = new Proveedor();
        perfil.setUid(uid);
        perfil.setTipoServicio(tipoModelo);
        perfil.setLatitud(latReal);
        perfil.setLongitud(lngReal);
        perfil.setVerificado(false);

        authViewModel.getUsuarioActual().observe(getViewLifecycleOwner(), usuario -> {
            if (usuario != null) {
                perfil.setNombre(usuario.getNombre());
                perfil.setTelefono(usuario.getTelefono());
                perfil.setCorreo(usuario.getCorreo());
            }

            // ── Subir foto a Firebase Storage ──
            com.google.firebase.storage.StorageReference ref =
                    com.google.firebase.storage.FirebaseStorage.getInstance()
                            .getReference()
                            .child("fotos_proveedores/" + uid + ".jpg");

            btnEnviar.setEnabled(false);
            btnEnviar.setText("Subiendo foto...");

            ref.putFile(fotoUri)
                    .addOnSuccessListener(task ->
                            ref.getDownloadUrl().addOnSuccessListener(url -> {
                                perfil.setFotoUrl(url.toString());
                                proveedorViewModel.guardarPerfil(perfil);
                            })
                    )
                    .addOnFailureListener(e -> {
                        // Falla la foto → guardar sin foto, no bloqueamos
                        Toast.makeText(getContext(),
                                "No se pudo subir la foto, continuando sin imagen.",
                                Toast.LENGTH_SHORT).show();
                        proveedorViewModel.guardarPerfil(perfil);
                    });
        });
    }
}