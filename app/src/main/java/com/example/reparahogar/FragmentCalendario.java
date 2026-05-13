package com.example.reparahogar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageButton;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Muestra el calendario nativo de Android y la lista de servicios actuales del cliente.
 * Al cambiar la fecha seleccionada en el calendario, filtra los servicios de ese día.
 */
public class FragmentCalendario extends Fragment {

    private ServicioViewModel servicioViewModel;
    private MantenimientoAdapter adapter;
    private final List<Servicio> todosServicios = new ArrayList<>();
    private final List<Servicio> serviciosFiltrados = new ArrayList<>();
    private String fechaSeleccionada = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        servicioViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(ServicioViewModel.class);

        CalendarView calendarView             = view.findViewById(R.id.calendarView);
        RecyclerView rvServiciosCalendario    = view.findViewById(R.id.rvServiciosCalendario);
        ImageButton  btnBack                  = view.findViewById(R.id.btnBackCalendar);

        rvServiciosCalendario.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MantenimientoAdapter(serviciosFiltrados, null);
        rvServiciosCalendario.setAdapter(adapter);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        // Fecha inicial = hoy
        fechaSeleccionada = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        calendarView.setOnDateChangeListener((cal, year, month, dayOfMonth) -> {
            fechaSeleccionada = String.format(Locale.getDefault(),
                    "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            filtrarPorFecha();
        });

        // Cargar servicios
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (!uid.isEmpty()) {
            servicioViewModel.getServiciosCliente(uid).observe(getViewLifecycleOwner(), servicios -> {
                todosServicios.clear();
                if (servicios != null) todosServicios.addAll(servicios);
                filtrarPorFecha();
            });
        }
    }

    private void filtrarPorFecha() {
        serviciosFiltrados.clear();
        for (Servicio s : todosServicios) {
            if (fechaSeleccionada.equals(s.getFecha())) {
                serviciosFiltrados.add(s);
            }
        }
        // Si no hay servicios del día, mostrar todos
        if (serviciosFiltrados.isEmpty()) {
            serviciosFiltrados.addAll(todosServicios);
        }
        adapter.notifyDataSetChanged();
    }
}