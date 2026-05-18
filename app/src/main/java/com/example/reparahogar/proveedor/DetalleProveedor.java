package com.example.reparahogar.proveedor;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.FragmentPerfilProveedor;
import com.example.reparahogar.MainActivity;
import com.example.reparahogar.R;
import com.example.reparahogar.FragmentServicioConfirmado;
import com.example.reparahogar.model.Servicio;
import com.example.reparahogar.viewmodel.ServicioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

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
// 1. Cambiar el color de fondo a azul
        getWindow().setStatusBarColor(Color.parseColor("#00468B"));

// 2. Forzar iconos blancos (Modo Oscuro de barra)
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        windowInsetsController.setAppearanceLightStatusBars(false);
        servicioViewModel = new ViewModelProvider(
                this,
                new ViewModelFactory(getApplication())
        ).get(ServicioViewModel.class);

        // ── Nuevo header (LinearLayout, ya no MaterialToolbar) ────────────────
        // El botón de perfil y el de salir ahora son hijos directos del header
        com.google.android.material.imageview.ShapeableImageView btnPerfil =
                findViewById(R.id.btnPerfil);
        ImageButton btnSalirProveedor = findViewById(R.id.btnSalirProveedor);

        if (btnPerfil != null) {
            btnPerfil.setOnClickListener(v -> {
                findViewById(R.id.contenedorFragments).setVisibility(View.VISIBLE);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.contenedorFragments, new FragmentPerfilProveedor())
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (btnSalirProveedor != null) {
            btnSalirProveedor.setOnClickListener(v -> cerrarSesion());
        }

        // ── RecyclerView ──────────────────────────────────────────────────────
        tvCantidadServicios = findViewById(R.id.tvCantidadServicios);

        RecyclerView rvAgenda = findViewById(R.id.rvAgendaProveedor);
        rvAgenda.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProveedorServicioAdapter(
                listaServicios,
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

        // ── Datos ─────────────────────────────────────────────────────────────
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (!uid.isEmpty()) {
            servicioViewModel.getServiciosProveedor(uid).observe(this, servicios -> {
                listaServicios.clear();
                if (servicios != null) listaServicios.addAll(servicios);
                adapter.notifyDataSetChanged();
            });

            servicioViewModel.getPendientesHoy(uid).observe(this, pendientes -> {
                int count = pendientes != null ? pendientes.size() : 0;
                // Actualizar solo el número, el texto "Pendientes hoy" ya está en el layout
                tvCantidadServicios.setText(String.valueOf(count));
            });
        }

        servicioViewModel.getErrorMensaje().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                servicioViewModel.limpiarError();
            }
        });

        // ── Back stack → ocultar contenedor ──────────────────────────────────
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                findViewById(R.id.contenedorFragments).setVisibility(View.GONE);
            }
        });
    }

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
        args.putString("idCita",     servicio.getId());
        args.putString("categoria",  servicio.getCategoria());
        args.putString("fecha",      servicio.getFecha());
        args.putString("direccion",  servicio.getDireccion());
        args.putString("hora",       servicio.getHora());
        args.putString("clienteUid", servicio.getClienteUid());
        fragment.setArguments(args);

        findViewById(R.id.contenedorFragments).setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedorFragments, fragment)
                .addToBackStack(null)
                .commit();
    }
}