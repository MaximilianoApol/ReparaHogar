package com.example.reparahogar;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        // 1. Configurar la barra de estado ANTES del return
        if (getActivity() != null) {
            Window window = getActivity().getWindow();
            window.setStatusBarColor(Color.parseColor("#00468B"));

            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(window, window.getDecorView());

            if (windowInsetsController != null) {
                windowInsetsController.setAppearanceLightStatusBars(false);
            }
        }

        return inflater.inflate(R.layout.fragment_calendario, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        servicioViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(ServicioViewModel.class);

        CalendarView calendarView = view.findViewById(R.id.calendarView);
        RecyclerView rvServiciosCalendario = view.findViewById(R.id.rvServiciosCalendario);
        ImageButton btnBack = view.findViewById(R.id.btnBackCalendar);

        rvServiciosCalendario.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MantenimientoAdapter(serviciosFiltrados, null);

        rvServiciosCalendario.setAdapter(adapter);

        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    getParentFragmentManager().popBackStack()
            );
        }

        // Fecha actual
        Calendar hoy = Calendar.getInstance();

        // Reiniciar horas/minutos/segundos
        hoy.set(Calendar.HOUR_OF_DAY, 0);
        hoy.set(Calendar.MINUTE, 0);
        hoy.set(Calendar.SECOND, 0);
        hoy.set(Calendar.MILLISECOND, 0);

        // Bloquear fechas pasadas
        calendarView.setMinDate(hoy.getTimeInMillis());

        // Fecha inicial = hoy
        fechaSeleccionada = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        ).format(new Date());

        calendarView.setOnDateChangeListener((cal, year, month, dayOfMonth) -> {

            Calendar fechaElegida = Calendar.getInstance();

            fechaElegida.set(year, month, dayOfMonth, 0, 0, 0);
            fechaElegida.set(Calendar.MILLISECOND, 0);

            // Validar manualmente
            if (fechaElegida.before(hoy)) {

                Toast.makeText(
                        requireContext(),
                        "No puedes seleccionar fechas pasadas",
                        Toast.LENGTH_SHORT
                ).show();

                // Regresar a hoy
                calendarView.setDate(hoy.getTimeInMillis(), true, true);

                return;
            }

            fechaSeleccionada = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    year,
                    month + 1,
                    dayOfMonth
            );

            filtrarPorFecha();
        });

        // Cargar servicios
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (!uid.isEmpty()) {

            servicioViewModel.getServiciosCliente(uid)
                    .observe(getViewLifecycleOwner(), servicios -> {

                        todosServicios.clear();

                        if (servicios != null) {
                            todosServicios.addAll(servicios);
                        }

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

        // Si no hay servicios de esa fecha
        if (serviciosFiltrados.isEmpty()) {
            serviciosFiltrados.addAll(todosServicios);
        }

        adapter.notifyDataSetChanged();
    }
}