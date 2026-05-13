package com.example.reparahogar;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.reparahogar.model.Servicio;
import com.example.reparahogar.viewmodel.ServicioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.Locale;

/**
 * Pantalla donde el cliente confirma:
 *  - Dirección del servicio
 *  - Fecha (DatePicker)
 *  - Hora (chips de horario)
 * y presiona "Agendar Servicio".
 */
public class FragmentAgendarServicio extends Fragment {

    private ServicioViewModel servicioViewModel;

    private String categoria;
    private String especificacion;
    private String proveedorUid;
    private String proveedorNombre;
    private String horaSeleccionada = "";

    private TextInputEditText etDireccion;
    private TextInputEditText etFechaCita;
    private ChipGroup chipGroupHorarios;
    private MaterialButton btnConfirmar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agendar_servicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Argumentos recibidos
        if (getArguments() != null) {
            categoria       = getArguments().getString("categoria", "");
            especificacion  = getArguments().getString("especificacion", "");
            proveedorUid    = getArguments().getString("proveedorUid", "");
            proveedorNombre = getArguments().getString("proveedorNombre", "");
        }

        // ViewModel
        servicioViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(ServicioViewModel.class);

        // Vistas
        ImageButton btnBack     = view.findViewById(R.id.btnBack);
        etDireccion             = view.findViewById(R.id.txtDireccionCompleta);
        etFechaCita             = view.findViewById(R.id.etFechaCita);
        chipGroupHorarios       = view.findViewById(R.id.chipGroupHorarios);
        btnConfirmar            = view.findViewById(R.id.btnConfirmar);

        // Titulo opcional con nombre del proveedor
        TextView txtDirLabel = view.findViewById(R.id.txtDireccionLabel);
        if (txtDirLabel != null && !proveedorNombre.isEmpty()) {
            txtDirLabel.setText("Técnico: " + proveedorNombre);
        }

        // Selección de hora con chips
        chipGroupHorarios.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) horaSeleccionada = chip.getText().toString();
            } else {
                horaSeleccionada = "";
            }
        });

        // DatePicker al tocar campo de fecha
        etFechaCita.setOnClickListener(v -> mostrarDatePicker());
        etFechaCita.setFocusable(false);

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnConfirmar.setOnClickListener(v -> agendarServicio());

        // Observar ViewModel
        servicioViewModel.getOperacionOk().observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Toast.makeText(getContext(), "¡Servicio agendado con éxito!", Toast.LENGTH_SHORT).show();
                servicioViewModel.limpiarOperacionOk();
                // Regresar a pantalla principal
                getParentFragmentManager().popBackStack(null,
                        androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });

        servicioViewModel.getErrorMensaje().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                servicioViewModel.limpiarError();
            }
        });

        servicioViewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            btnConfirmar.setEnabled(!cargando);
            btnConfirmar.setText(cargando ? "Agendando..." : "Agendar Servicio");
        });
    }

    private void mostrarDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (datePicker, year, month, day) -> {
            String fecha = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            etFechaCita.setText(fecha);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void agendarServicio() {
        String direccion = etDireccion != null && etDireccion.getText() != null
                ? etDireccion.getText().toString().trim() : "";
        String fecha = etFechaCita != null && etFechaCita.getText() != null
                ? etFechaCita.getText().toString().trim() : "";

        if (direccion.isEmpty()) {
            Toast.makeText(getContext(), "Ingresa la dirección", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fecha.isEmpty()) {
            Toast.makeText(getContext(), "Selecciona la fecha", Toast.LENGTH_SHORT).show();
            return;
        }
        if (horaSeleccionada.isEmpty()) {
            Toast.makeText(getContext(), "Selecciona una hora", Toast.LENGTH_SHORT).show();
            return;
        }

        String clienteUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (clienteUid.isEmpty()) {
            Toast.makeText(getContext(), "Sesión expirada, inicia sesión de nuevo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertir categoría UI → constante del modelo
        String catModelo;
        switch (categoria) {
            case "Agua / Plomería": catModelo = Servicio.CAT_PLOMERIA; break;
            case "Electricidad":    catModelo = Servicio.CAT_ELECTRICIDAD; break;
            case "Gas":             catModelo = Servicio.CAT_GAS; break;
            default:                catModelo = categoria.toUpperCase(); break;
        }

        servicioViewModel.agendarServicio(
                clienteUid,
                proveedorUid,
                catModelo,
                especificacion,
                direccion,
                fecha,
                horaSeleccionada
        );
    }
}