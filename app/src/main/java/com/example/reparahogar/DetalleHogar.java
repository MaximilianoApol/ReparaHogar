package com.example.reparahogar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.adapter.MantenimientoAdapter;
import com.example.reparahogar.model.Servicio;
import com.example.reparahogar.viewmodel.ServicioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla principal del cliente (dueño de casa).
 *
 * Responsabilidades:
 *  1. Chips de categoría → FragmentSeleccionEspecificacion
 *  2. Lista de mantenimientos solicitados (Room → Firestore en tiempo real)
 *  3. BottomNav: Inicio | Calendario | Notificaciones
 *  4. Botón de perfil → FragmentPerfil
 *  5. Botón de logout → MainActivity
 */
public class DetalleHogar extends AppCompatActivity {

    private ServicioViewModel servicioViewModel;
    private MantenimientoAdapter adapter;
    private final List<Servicio> listaServicios = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_hogar);

        // ── ViewModels ────────────────────────────────────────────────────────
        servicioViewModel = new ViewModelProvider(
                this,
                new ViewModelFactory(getApplication())
        ).get(ServicioViewModel.class);

        // ── Toolbar ───────────────────────────────────────────────────────────
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> cerrarSesion());

        // ── Botón perfil ──────────────────────────────────────────────────────
        ImageButton btnPerfil = findViewById(R.id.btnPerfil);
        if (btnPerfil != null) {
            btnPerfil.setOnClickListener(v ->
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.mi_hogar, new FragmentPerfil())
                            .addToBackStack(null)
                            .commit()
            );
        }

        // ── RecyclerView mantenimientos ───────────────────────────────────────
        RecyclerView rvMantenimientos = findViewById(R.id.rvMantenimientos);
        if (rvMantenimientos != null) {
            rvMantenimientos.setLayoutManager(new LinearLayoutManager(this));
            adapter = new MantenimientoAdapter(listaServicios, this::abrirCalificacion);
            rvMantenimientos.setAdapter(adapter);
        }

        // ── Chips de categoría ────────────────────────────────────────────────
        findViewById(R.id.chipPlomeria)
                .setOnClickListener(v -> irASeleccion("Agua / Plomería"));
        findViewById(R.id.chipElectricidad)
                .setOnClickListener(v -> irASeleccion("Electricidad"));
        findViewById(R.id.chipGas)
                .setOnClickListener(v -> irASeleccion("Gas"));

        // ── Bottom Navigation ─────────────────────────────────────────────────
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.page_1) {
                    // Inicio: limpiar back-stack de fragments
                    getSupportFragmentManager().popBackStack(
                            null,
                            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    return true;
                } else if (id == R.id.page_2) {
                    abrirFragment(new FragmentCalendario());
                    return true;
                } else if (id == R.id.page_3) {
                    abrirFragment(new FragmentNotificaciones());
                    return true;
                }
                return false;
            });
        }

        // ── Cargar servicios del cliente ──────────────────────────────────────
        cargarServicios();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void cargarServicios() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (!uid.isEmpty()) {
            servicioViewModel.getServiciosCliente(uid).observe(this, servicios -> {
                listaServicios.clear();
                if (servicios != null) listaServicios.addAll(servicios);
                if (adapter != null) adapter.notifyDataSetChanged();
            });
        }
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(DetalleHogar.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void irASeleccion(String categoria) {
        FragmentSeleccionEspecificacion fragment = new FragmentSeleccionEspecificacion();
        Bundle args = new Bundle();
        args.putString("categoria", categoria);
        fragment.setArguments(args);
        abrirFragment(fragment);
    }

    private void abrirCalificacion(Servicio servicio) {
        FragmentCalificacion fragment = new FragmentCalificacion();
        Bundle args = new Bundle();
        args.putString("servicioId",   servicio.getId());
        args.putString("proveedorUid", servicio.getProveedorUid());
        fragment.setArguments(args);
        abrirFragment(fragment);
    }

    private void abrirFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mi_hogar, fragment)
                .addToBackStack(null)
                .commit();
    }
}