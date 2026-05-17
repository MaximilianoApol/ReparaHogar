package com.example.reparahogar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.adapter.MantenimientoAdapter;
import com.example.reparahogar.model.Servicio;
import com.example.reparahogar.viewmodel.ServicioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;


public class FragmentNotificaciones extends Fragment {

    private ServicioViewModel servicioViewModel;
    private MantenimientoAdapter adapter;
    private final List<Servicio> listaServicios = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,       @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notificaciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        servicioViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(ServicioViewModel.class);

        RecyclerView rvNotificaciones = view.findViewById(R.id.rvNotificaciones);
        rvNotificaciones.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MantenimientoAdapter(listaServicios, this::abrirCalificacion);
        rvNotificaciones.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (!uid.isEmpty()) {
            servicioViewModel.getServiciosCliente(uid).observe(getViewLifecycleOwner(), servicios -> {
                listaServicios.clear();
                if (servicios != null) listaServicios.addAll(servicios);
                adapter.notifyDataSetChanged();
            });
        }
    }

    private void abrirCalificacion(Servicio servicio) {
        FragmentCalificacion fragment = new FragmentCalificacion();
        Bundle args = new Bundle();
        args.putString("servicioId",   servicio.getId());
        args.putString("proveedorUid", servicio.getProveedorUid());
        fragment.setArguments(args);

        // Contenedor principal de DetalleHogar
        getParentFragmentManager().beginTransaction()
                .replace(R.id.mi_hogar, fragment)
                .addToBackStack(null)
                .commit();
    }
}