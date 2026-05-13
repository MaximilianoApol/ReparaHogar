package com.example.reparahogar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.adapter.TecnicoAdapter;
import com.example.reparahogar.model.Proveedor;
import com.example.reparahogar.viewmodel.ProveedorViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;


public class FragmentTecnicosDisponibles extends Fragment {

    private ProveedorViewModel proveedorViewModel;
    private TecnicoAdapter adapter;
    private List<Proveedor> listaTecnicos = new ArrayList<>();
    private Proveedor tecnicoSeleccionado = null;

    private String categoria;
    private String especificacion;

    private MaterialButton btnObtenerUbicacion;
    private MaterialButton btnSolicitar;
    private RecyclerView rvTecnicos;
    private TextView txtEstadoUbicacion;

    // Lanzador de permiso de ubicación
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                    proveedorViewModel.obtenerUbicacionCliente();
                } else {
                    Toast.makeText(getContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tecnicos_disponibles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Recuperar argumentos
        if (getArguments() != null) {
            categoria = getArguments().getString("categoria", "");
            especificacion = getArguments().getString("especificacion", "");
        }

        // ViewModel
        proveedorViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(ProveedorViewModel.class);

        // Vistas
        btnObtenerUbicacion = view.findViewById(R.id.btnObtenerUbicacion);
        btnSolicitar        = view.findViewById(R.id.btnSolicitar);
        rvTecnicos          = view.findViewById(R.id.rvTecnicos);
        ImageButton btnBack = view.findViewById(R.id.btnBack);

        // RecyclerView
        adapter = new TecnicoAdapter(listaTecnicos, proveedor -> {
            tecnicoSeleccionado = proveedor;
            btnSolicitar.setEnabled(true);
            btnSolicitar.setAlpha(1f);
        });
        rvTecnicos.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTecnicos.setAdapter(adapter);

        btnSolicitar.setEnabled(false);
        btnSolicitar.setAlpha(0.5f);

        // Listeners
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnObtenerUbicacion.setOnClickListener(v -> solicitarPermisosUbicacion());

        btnSolicitar.setOnClickListener(v -> {
            if (tecnicoSeleccionado != null) {
                irAAgendarServicio();
            }
        });

        // Observar ViewModel
        observarViewModel();
    }

    private void solicitarPermisosUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            proveedorViewModel.obtenerUbicacionCliente();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void observarViewModel() {
        proveedorViewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            btnObtenerUbicacion.setEnabled(!cargando);
            btnObtenerUbicacion.setText(cargando ? "Detectando..." : "Detectar mi ubicación");
        });

        proveedorViewModel.getUbicacionObtenida().observe(getViewLifecycleOwner(), obtenida -> {
            if (Boolean.TRUE.equals(obtenida)) {
                // Convertir categoría de UI a tipo de servicio del modelo
                String tipoServicio = convertirCategoria(categoria);
                proveedorViewModel.buscarProveedoresCercanos(tipoServicio)
                        .observe(getViewLifecycleOwner(), proveedores -> {
                            listaTecnicos.clear();
                            if (proveedores != null) listaTecnicos.addAll(proveedores);
                            adapter.notifyDataSetChanged();
                            if (listaTecnicos.isEmpty()) {
                                Toast.makeText(getContext(),
                                        "No hay técnicos disponibles cerca",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        proveedorViewModel.getErrorMensaje().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                proveedorViewModel.limpiarError();
            }
        });
    }

    /** Convierte el nombre de categoría de UI al tipo guardado en Firestore */
    private String convertirCategoria(String categoria) {
        if (categoria == null) return "PLOMERIA";
        switch (categoria) {
            case "Agua / Plomería": return "PLOMERIA";
            case "Electricidad":   return "ELECTRICIDAD";
            case "Gas":            return "GAS";
            default:               return categoria.toUpperCase();
        }
    }

    private void irAAgendarServicio() {
        FragmentAgendarServicio fragment = new FragmentAgendarServicio();
        Bundle args = new Bundle();
        args.putString("categoria",      categoria);
        args.putString("especificacion", especificacion);
        args.putString("proveedorUid",   tecnicoSeleccionado.getUid());
        args.putString("proveedorNombre",tecnicoSeleccionado.getNombre());
        fragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.mi_hogar, fragment)
                .addToBackStack(null)
                .commit();
    }
}