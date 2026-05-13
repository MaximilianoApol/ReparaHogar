package com.example.reparahogar.proveedor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.FragmentPerfil;
import com.example.reparahogar.FragmentPerfilProveedor;
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

        servicioViewModel = new ViewModelProvider(
                this,
                new ViewModelFactory(getApplication())
        ).get(ServicioViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbarProveedor);

        toolbar.setNavigationOnClickListener(v -> cerrarSesion());

        ImageButton btnPerfil = toolbar.findViewById(R.id.btnPerfil);

        if (btnPerfil != null) {
            btnPerfil.setOnClickListener(v -> {
                findViewById(R.id.contenedorFragments)
                        .setVisibility(View.VISIBLE);

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.contenedorFragments,
                                new FragmentPerfilProveedor())
                        .addToBackStack(null)
                        .commit();
            });
        }

        tvCantidadServicios = findViewById(R.id.tvCantidadServicios);

        RecyclerView rvAgenda = findViewById(R.id.rvAgendaProveedor);
        rvAgenda.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProveedorServicioAdapter(
                listaServicios,
                new ProveedorServicioAdapter.OnServicioClickListener() {

                    @Override
                    public void onConfirmar(Servicio servicio) {
                        servicioViewModel.confirmarServicio(servicio.getId());

                        Toast.makeText(
                                DetalleProveedor.this,
                                "Servicio confirmado",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onFinalizar(Servicio servicio) {
                        abrirFragmentConfirmado(servicio);
                    }
                });

        rvAgenda.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (!uid.isEmpty()) {

            servicioViewModel.getServiciosProveedor(uid)
                    .observe(this, servicios -> {

                        listaServicios.clear();

                        if (servicios != null) {
                            listaServicios.addAll(servicios);
                        }

                        adapter.notifyDataSetChanged();
                    });

            servicioViewModel.getPendientesHoy(uid)
                    .observe(this, pendientes -> {

                        int count = pendientes != null
                                ? pendientes.size()
                                : 0;

                        tvCantidadServicios.setText(count + " Pendientes");
                    });
        }

        servicioViewModel.getErrorMensaje()
                .observe(this, error -> {

                    if (error != null && !error.isEmpty()) {

                        Toast.makeText(
                                this,
                                error,
                                Toast.LENGTH_LONG
                        ).show();

                        servicioViewModel.limpiarError();
                    }
                });

        getSupportFragmentManager()
                .addOnBackStackChangedListener(() -> {

                    if (getSupportFragmentManager()
                            .getBackStackEntryCount() == 0) {

                        findViewById(R.id.contenedorFragments)
                                .setVisibility(View.GONE);
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
        args.putString("idCita",    servicio.getId());
        args.putString("categoria", servicio.getCategoria());
        args.putString("fecha",     servicio.getFecha());
        fragment.setArguments(args);

        // Mostrar el contenedor antes de cargar el fragment
        findViewById(R.id.contenedorFragments).setVisibility(View.VISIBLE);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedorFragments, fragment)
                .addToBackStack(null)
                .commit();
    }

}