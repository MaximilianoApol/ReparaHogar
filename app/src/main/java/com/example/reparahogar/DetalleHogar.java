package com.example.reparahogar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.adapter.MantenimientoAdapter;
import com.example.reparahogar.model.Servicio;
import com.example.reparahogar.viewmodel.ServicioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetalleHogar extends AppCompatActivity {

    private ServicioViewModel    servicioViewModel;
    private MantenimientoAdapter adapter;
    private BottomNavigationView bottomNav;
    private View                 fragmentContainer;
    private TextView             btnVerTodo;

    private final List<Servicio> listaServiciosVisibles = new ArrayList<>();
    private List<Servicio> listaCompletaFirebase = new ArrayList<>();

    // Estado para el toggle de visualización
    private boolean mostrandoTodo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_hogar);

        // 1. Status Bar
        getWindow().setStatusBarColor(Color.parseColor("#00468B"));
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(false);
        }

        // 2. Header
        ShapeableImageView btnPerfil = findViewById(R.id.btnPerfil);
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        if (btnPerfil != null) btnPerfil.setOnClickListener(v -> abrirFragment(new FragmentPerfil()));
        if (btnLogout != null) btnLogout.setOnClickListener(v -> cerrarSesion());

        // 3. ViewModel
        fragmentContainer = findViewById(R.id.mi_hogar);
        servicioViewModel = new ViewModelProvider(this, new ViewModelFactory(getApplication())).get(ServicioViewModel.class);

        // 4. RecyclerView
        RecyclerView rv = findViewById(R.id.rvMantenimientos);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new MantenimientoAdapter(listaServiciosVisibles, this::abrirCalificacion);
            rv.setAdapter(adapter);
        }

        // 5. Lógica del botón Toggle "Ver todo / Hoy"
        btnVerTodo = findViewById(R.id.btnVerTodo);
        if (btnVerTodo != null) {
            btnVerTodo.setOnClickListener(v -> {
                if (mostrandoTodo) {
                    filtrarServiciosHoy(listaCompletaFirebase);
                    mostrandoTodo = false;
                    btnVerTodo.setText("Ver todos");
                } else {
                    mostrarTodosLosServicios();
                    mostrandoTodo = true;
                    btnVerTodo.setText("Hoy");
                }
            });
        }

        // 6. Configurar Categorías y Navegación
        configurarCategorias(); // <--- ESTO ES LO QUE FALTABA
        configurarNavegacion();

        cargarServicios();
    }

    private void configurarCategorias() {
        View plomeria = findViewById(R.id.chipPlomeria);
        View electricidad = findViewById(R.id.chipElectricidad);
        View gas = findViewById(R.id.chipGas);

        if (plomeria != null) plomeria.setOnClickListener(v -> irASeleccion("Agua / Plomería"));
        if (electricidad != null) electricidad.setOnClickListener(v -> irASeleccion("Electricidad"));
        if (gas != null) gas.setOnClickListener(v -> irASeleccion("Gas"));
    }

    private void cargarServicios() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        servicioViewModel.getServiciosCliente(uid).observe(this, servicios -> {
            if (servicios != null) {
                this.listaCompletaFirebase = servicios;
                if (mostrandoTodo) {
                    mostrarTodosLosServicios();
                } else {
                    filtrarServiciosHoy(servicios);
                }
                actualizarBadge(servicios);
            }
        });
    }

    private void filtrarServiciosHoy(List<Servicio> servicios) {
        String hoy = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        listaServiciosVisibles.clear();

        for (Servicio s : servicios) {
            if (hoy.equals(s.getFecha())) {
                listaServiciosVisibles.add(s);
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();

        if (btnVerTodo != null) {
            btnVerTodo.setVisibility(listaServiciosVisibles.size() < listaCompletaFirebase.size()
                    ? View.VISIBLE : View.GONE);
        }
    }

    private void mostrarTodosLosServicios() {
        listaServiciosVisibles.clear();
        listaServiciosVisibles.addAll(listaCompletaFirebase);
        if (adapter != null) adapter.notifyDataSetChanged();

        if (btnVerTodo != null) btnVerTodo.setVisibility(View.VISIBLE);
    }

    private void actualizarBadge(List<Servicio> servicios) {
        if (bottomNav == null) return;
        int terminados = 0;
        for (Servicio s : servicios) {
            if (Servicio.ESTADO_TERMINADO.equals(s.getEstado())) terminados++;
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
        startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
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
        args.putString("servicioId", servicio.getId());
        args.putString("proveedorUid", servicio.getProveedorUid());
        frag.setArguments(args);
        abrirFragment(frag);
    }

    private void abrirFragment(androidx.fragment.app.Fragment fragment) {
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction().replace(R.id.mi_hogar, fragment).addToBackStack(null).commit();
    }

    private void configurarNavegacion() {
        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.page_1) {
                    getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.page_1);
            }
        });
    }
}