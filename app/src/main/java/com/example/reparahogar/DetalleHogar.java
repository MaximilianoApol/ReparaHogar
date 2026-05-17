package com.example.reparahogar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

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
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
public class DetalleHogar extends AppCompatActivity {

    private ServicioViewModel    servicioViewModel;
    private MantenimientoAdapter adapter;
    private BottomNavigationView bottomNav;
    private View                 fragmentContainer;
    private final List<Servicio> listaServicios = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_hogar);

        fragmentContainer = findViewById(R.id.mi_hogar);

        servicioViewModel = new ViewModelProvider(
                this, new ViewModelFactory(getApplication()))
                .get(ServicioViewModel.class);


// Reemplaza en DetalleHogar.java el bloque del toolbar por esto:
        LinearLayout header = findViewById(R.id.headerLayout);
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> cerrarSesion());
        }

// El toolbar sigue existente (hidden) para no romper el código,
// pero ya no tiene navigationIcon funcional — lo maneja btnLogout


        RecyclerView rv = findViewById(R.id.rvMantenimientos);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new MantenimientoAdapter(listaServicios, this::abrirCalificacion);
            rv.setAdapter(adapter);
        }

        findViewById(R.id.chipPlomeria)
                .setOnClickListener(v -> irASeleccion("Agua / Plomería"));
        findViewById(R.id.chipElectricidad)
                .setOnClickListener(v -> irASeleccion("Electricidad"));
        findViewById(R.id.chipGas)
                .setOnClickListener(v -> irASeleccion("Gas"));


        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.page_1) {
                    // Limpiar todo el back stack y ocultar el contenedor
                    getSupportFragmentManager().popBackStack(
                            null,
                            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    return true;
                } else if (id == R.id.page_2) {
                    abrirFragment(new FragmentCalendario());
                    return true;
                } else if (id == R.id.page_3) {
                    BadgeDrawable badge = bottomNav.getBadge(R.id.page_3);
                    if (badge != null) badge.setVisible(false);
                    abrirFragment(new FragmentNotificaciones());
                    return true;
                }
                return false;
            });
        }


        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                fragmentContainer.setVisibility(View.GONE);
                // Restaurar selección del tab Inicio
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.page_1);
                }
            }
        });

        cargarServicios();
    }

    private void cargarServicios() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (uid.isEmpty()) return;

        servicioViewModel.getServiciosCliente(uid).observe(this, servicios -> {
            listaServicios.clear();
            if (servicios != null) listaServicios.addAll(servicios);
            if (adapter != null) adapter.notifyDataSetChanged();
            actualizarBadge(servicios);
        });
    }

    private void actualizarBadge(List<Servicio> servicios) {
        if (bottomNav == null) return;
        int terminados = 0;
        if (servicios != null) {
            for (Servicio s : servicios) {
                if (Servicio.ESTADO_TERMINADO.equals(s.getEstado())) terminados++;
            }
        }
        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.page_3);
        if (terminados > 0) {
            badge.setNumber(terminados);
            badge.setVisible(true);
        } else {
            badge.setVisible(false);
        }
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void irASeleccion(String categoria) {
        FragmentSeleccionEspecificacion frag = new FragmentSeleccionEspecificacion();
        Bundle args = new Bundle();
        args.putString("categoria", categoria);
        frag.setArguments(args);
        abrirFragment(frag);
    }

    private void abrirCalificacion(Servicio servicio) {
        FragmentCalificacion frag = new FragmentCalificacion();
        Bundle args = new Bundle();
        args.putString("servicioId",   servicio.getId());
        args.putString("proveedorUid", servicio.getProveedorUid());
        frag.setArguments(args);
        abrirFragment(frag);
    }

    private void abrirFragment(androidx.fragment.app.Fragment fragment) {
        fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mi_hogar, fragment)
                .addToBackStack(null)
                .commit();
    }
}