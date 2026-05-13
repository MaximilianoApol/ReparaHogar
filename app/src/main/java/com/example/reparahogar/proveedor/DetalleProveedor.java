package com.example.reparahogar.proveedor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.MainActivity;
import com.example.reparahogar.R;
import com.example.reparahogar.FragmentServicioConfirmado;
import com.example.reparahogar.model.Servicio;
import com.example.reparahogar.viewmodel.ServicioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla principal del proveedor.
 *
 * Muestra:
 *  1. Contador de servicios PENDIENTES de hoy
 *  2. RecyclerView con todos sus servicios
 *  3. Al tocar un servicio vigente → BottomSheet (Confirmar / Finalizar)
 */
public class DetalleProveedor extends AppCompatActivity {

    private ServicioViewModel servicioViewModel;
    private ProveedorServicioAdapter adapter;
    private final List<Servicio> listaServicios = new ArrayList<>();
    private TextView tvCantidadServicios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_proveedor);

        // ── ViewModels ────────────────────────────────────────────────────────
        servicioViewModel = new ViewModelProvider(
                this,
                new ViewModelFactory(getApplication())
        ).get(ServicioViewModel.class);

        // ── Toolbar ───────────────────────────────────────────────────────────
        MaterialToolbar toolbar = findViewById(R.id.toolbarProveedor);
        toolbar.setNavigationOnClickListener(v -> cerrarSesion());

        // ── Contador pendientes ───────────────────────────────────────────────
        tvCantidadServicios = findViewById(R.id.tvCantidadServicios);

        // ── RecyclerView ──────────────────────────────────────────────────────
        RecyclerView rvAgenda = findViewById(R.id.rvAgendaProveedor);
        rvAgenda.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProveedorServicioAdapter(listaServicios,
                new ProveedorServicioAdapter.OnServicioClickListener() {
                    @Override
                    public void onConfirmar(Servicio servicio) {
                        servicioViewModel.confirmarServicio(servicio.getId());
                        Toast.makeText(DetalleProveedor.this,
                                "Servicio confirmado", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinalizar(Servicio servicio) {
                        abrirFragmentConfirmado(servicio);
                    }
                });

        rvAgenda.setAdapter(adapter);

        // ── Observar datos ────────────────────────────────────────────────────
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (!uid.isEmpty()) {
            // Lista completa de servicios
            servicioViewModel.getServiciosProveedor(uid).observe(this, servicios -> {
                listaServicios.clear();
                if (servicios != null) listaServicios.addAll(servicios);
                adapter.notifyDataSetChanged();
            });

            // Contador pendientes de hoy
            servicioViewModel.getPendientesHoy(uid).observe(this, pendientes -> {
                int count = pendientes != null ? pendientes.size() : 0;
                tvCantidadServicios.setText(count + " Pendientes");
            });
        }

        // Resultado de operaciones
        servicioViewModel.getErrorMensaje().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                servicioViewModel.limpiarError();
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(DetalleProveedor.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void abrirFragmentConfirmado(Servicio servicio) {
        FragmentServicioConfirmado fragment = new FragmentServicioConfirmado();
        Bundle args = new Bundle();
        args.putString("idCita",    servicio.getId());
        args.putString("categoria", servicio.getCategoria());
        args.putString("fecha",     servicio.getFecha());
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_proveedor, fragment)
                .addToBackStack(null)
                .commit();
    }
}