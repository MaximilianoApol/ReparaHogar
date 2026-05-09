package com.example.reparahogar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

public class VerificacionFragment extends Fragment {

    private MaterialCheckBox checkUbicacion;
    private MaterialButton btnEnviar;
    private AutoCompleteTextView spinnerServicios;
    private AppDatabase db;
    private FusedLocationProviderClient fusedLocationClient;

    // Este objeto se encarga de mostrar la ventanita de "Permitir"
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineLocationGranted != null && fineLocationGranted) {
                    obtenerUbicacionReal();
                } else {
                    Toast.makeText(getContext(), "Permiso denegado. No podemos verificar tu zona de trabajo.", Toast.LENGTH_SHORT).show();
                    checkUbicacion.setChecked(false);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflamos el layout que creaste
        View view = inflater.inflate(R.layout.fragment_verificacion, container, false);

        // Inicializamos la base de datos y los componentes
        db = AppDatabase.getDatabase(getContext());
        checkUbicacion = view.findViewById(R.id.checkPermisoUbicacion);
        btnEnviar = view.findViewById(R.id.btnEnviarVerificacion);
        spinnerServicios = view.findViewById(R.id.spinnerServicios);

        // El botón empieza desactivado hasta que marquen el checkbox
        btnEnviar.setEnabled(false);

        // Lógica del Checkbox para habilitar el botón
        checkUbicacion.setOnCheckedChangeListener((button, isChecked) -> {
            btnEnviar.setEnabled(isChecked);
        });

        // Lógica del Botón Enviar
        btnEnviar.setOnClickListener(v -> {
            registrarTecnico();
        });

        return view; // ¡ESTE ES EL RETURN QUE FALTABA!
    }
    private double latReal = 0.0, lngReal = 0.0;

    private void obtenerUbicacionReal() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    latReal = location.getLatitude();
                    lngReal = location.getLongitude();
                    Toast.makeText(getContext(), "Ubicación detectada con éxito", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void registrarTecnico() {
        String servicioSeleccionado = spinnerServicios.getText().toString();

        if (servicioSeleccionado.isEmpty()) {
            Toast.makeText(getContext(), "Por favor selecciona un servicio", Toast.LENGTH_SHORT).show();
            return;
        }


        new Thread(() -> {
            // Creamos el objeto Proveedor con los nuevos campos
            Proveedor nuevoTecnico = new Proveedor();
            nuevoTecnico.setNombre("Técnico de Prueba");
            nuevoTecnico.setServicios(servicioSeleccionado);
            nuevoTecnico.setLatitud(latReal);
            nuevoTecnico.setLongitud(lngReal);
            nuevoTecnico.setVerificado(false);

            // Guardamos en la base de datos (Room)
            db.appDao().insertarProveedor(nuevoTecnico);

            // Regresamos al hilo principal para mostrar el mensaje
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Registro guardado en base de datos local", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}