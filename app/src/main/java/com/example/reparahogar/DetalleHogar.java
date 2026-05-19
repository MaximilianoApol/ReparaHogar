package com.example.reparahogar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.adapter.MantenimientoAdapter;
import com.example.reparahogar.database.AppDatabase;
import com.example.reparahogar.model.Calificacion;
import com.example.reparahogar.model.Servicio;
import com.example.reparahogar.utils.ExecutorUtils;
import com.example.reparahogar.viewmodel.ServicioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Pantalla principal del cliente.
 *
 * CORRECCIÓN CRÍTICA aplicada aquí:
 *  abrirDetalle() sanitiza TODOS los campos con nn() antes de pasarlos
 *  como Bundle a FragmentDetalleServicio, evitando el NPE en switch(estado).
 */
public class DetalleHogar extends AppCompatActivity {

    private ServicioViewModel    servicioViewModel;
    private MantenimientoAdapter adapter;
    private BottomNavigationView bottomNav;
    private View                 fragmentContainer;
    private TextView             btnVerTodo;

    private final List<Servicio> listaServiciosVisibles = new ArrayList<>();
    private List<Servicio>       listaCompletaFirebase  = new ArrayList<>();
    private boolean              mostrandoTodo          = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_hogar);

        getWindow().setStatusBarColor(Color.parseColor("#00468B"));
        WindowInsetsControllerCompat wic =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (wic != null) wic.setAppearanceLightStatusBars(false);

        fragmentContainer = findViewById(R.id.mi_hogar);
        servicioViewModel = new ViewModelProvider(
                this, new ViewModelFactory(getApplication()))
                .get(ServicioViewModel.class);

        ShapeableImageView btnPerfil = findViewById(R.id.btnPerfil);
        ImageButton        btnLogout = findViewById(R.id.btnLogout);
        if (btnPerfil != null) btnPerfil.setOnClickListener(v -> abrirFragment(new FragmentPerfil()));
        if (btnLogout != null) btnLogout.setOnClickListener(v -> cerrarSesion());

        RecyclerView rv = findViewById(R.id.rvMantenimientos);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new MantenimientoAdapter(listaServiciosVisibles, this::abrirDetalle);
            rv.setAdapter(adapter);
        }

        btnVerTodo = findViewById(R.id.btnVerTodo);
        if (btnVerTodo != null) {
            btnVerTodo.setOnClickListener(v -> {
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid == null) return;
                ExecutorUtils.getExecutor().execute(() -> {
                    List<String> ids = AppDatabase.getInstance(this)
                            .calificacionDao().obtenerIdsCalificadosPorCliente(uid);
                    runOnUiThread(() -> {
                        if (mostrandoTodo) {
                            filtrarServiciosHoy(listaCompletaFirebase, ids);
                            mostrandoTodo = false;
                            btnVerTodo.setText("Ver todos");
                        } else {
                            mostrarTodosLosServicios(ids);
                            mostrandoTodo = true;
                            btnVerTodo.setText("Hoy");
                        }
                    });
                });
            });
        }

        configurarCategorias();
        configurarNavegacion();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) sincronizarCalificacionesDesdeFirestore(uid);
        else             cargarServicios();
    }

    private void configurarCategorias() {
        View plomeria     = findViewById(R.id.chipPlomeria);
        View electricidad = findViewById(R.id.chipElectricidad);
        View gas          = findViewById(R.id.chipGas);
        if (plomeria     != null) plomeria.setOnClickListener(v -> irASeleccion("Agua / Plomería"));
        if (electricidad != null) electricidad.setOnClickListener(v -> irASeleccion("Electricidad"));
        if (gas          != null) gas.setOnClickListener(v -> irASeleccion("Gas"));
    }

    private void cargarServicios() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        servicioViewModel.getServiciosCliente(uid).observe(this, servicios -> {
            if (servicios == null) return;
            listaCompletaFirebase = servicios;

            ExecutorUtils.getExecutor().execute(() -> {
                List<String> ids = AppDatabase.getInstance(this)
                        .calificacionDao().obtenerIdsCalificadosPorCliente(uid);
                runOnUiThread(() -> {
                    if (mostrandoTodo) mostrarTodosLosServicios(ids);
                    else               filtrarServiciosHoy(servicios, ids);
                    actualizarBadge(servicios, ids);
                });
            });
        });
    }

    private void filtrarServiciosHoy(List<Servicio> servicios, List<String> idsCalificados) {
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        List<Servicio> hoyList = new ArrayList<>();
        for (Servicio s : servicios) {
            if (hoy.equals(s.getFecha())) hoyList.add(s);
        }
        if (adapter != null) adapter.actualizarLista(hoyList, idsCalificados);
        if (btnVerTodo != null) {
            btnVerTodo.setVisibility(
                    hoyList.size() < listaCompletaFirebase.size() ? View.VISIBLE : View.GONE);
        }
    }

    private void mostrarTodosLosServicios(List<String> idsCalificados) {
        if (adapter != null) adapter.actualizarLista(listaCompletaFirebase, idsCalificados);
        if (btnVerTodo != null) btnVerTodo.setVisibility(View.VISIBLE);
    }

    private void actualizarBadge(List<Servicio> servicios, List<String> idsCalificados) {
        if (bottomNav == null) return;
        int sinCalificar = 0;
        for (Servicio s : servicios) {
            if (Servicio.ESTADO_TERMINADO.equals(s.getEstado())
                    && !idsCalificados.contains(s.getId())) sinCalificar++;
        }
        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.page_3);
        if (sinCalificar > 0) {
            badge.setNumber(sinCalificar);
            badge.setVisible(true);
        } else {
            badge.setVisible(false);
            badge.clearNumber();
        }
    }

    private void configurarNavegacion() {
        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav == null) return;

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.page_1) {
                getSupportFragmentManager().popBackStack(
                        null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
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

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
                bottomNav.setSelectedItemId(R.id.page_1);
            }
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) sincronizarYRefrescarBadge(uid);
        });
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void irASeleccion(String categoria) {
        FragmentSeleccionEspecificacion frag = new FragmentSeleccionEspecificacion();
        Bundle args = new Bundle();
        args.putString("categoria", categoria);
        frag.setArguments(args);
        abrirFragment(frag);
    }

    /**
     * CORRECCIÓN CRÍTICA del crash:
     * Todos los campos del Servicio se sanitizan con nn() → nunca null en el Bundle.
     * El estado usa PENDIENTE como fallback (nunca llega null al fragment).
     */
    private void abrirDetalle(Servicio servicio) {
        FragmentDetalleServicio frag = new FragmentDetalleServicio();
        Bundle args = new Bundle();
        args.putString("servicioId",   nn(servicio.getId()));
        args.putString("proveedorUid", nn(servicio.getProveedorUid()));
        args.putString("categoria",    nn(servicio.getCategoria()));
        args.putString("detalle",      nn(servicio.getDetalle()));
        args.putString("estado",       servicio.getEstado() != null
                ? servicio.getEstado() : Servicio.ESTADO_PENDIENTE);
        args.putString("fecha",        nn(servicio.getFecha()));
        args.putString("hora",         nn(servicio.getHora()));
        args.putString("direccion",    nn(servicio.getDireccion()));
        frag.setArguments(args);
        abrirFragment(frag);
    }

    private void abrirFragment(androidx.fragment.app.Fragment fragment) {
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mi_hogar, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void sincronizarYRefrescarBadge(String uid) {
        FirebaseFirestore.getInstance()
                .collection("calificaciones")
                .whereEqualTo("clienteUid", uid)
                .get()
                .addOnSuccessListener(query -> {
                    List<String> ids = new ArrayList<>();
                    if (!query.isEmpty()) {
                        List<Calificacion> cals = query.toObjects(Calificacion.class);
                        ExecutorUtils.getExecutor().execute(() -> {
                            AppDatabase db = AppDatabase.getInstance(this);
                            for (Calificacion c : cals) db.calificacionDao().insertar(c);
                        });
                        for (Calificacion c : cals) ids.add(c.getServicioId());
                    }
                    if (!listaCompletaFirebase.isEmpty()) {
                        actualizarBadge(listaCompletaFirebase, ids);
                        if (mostrandoTodo) mostrarTodosLosServicios(ids);
                        else              filtrarServiciosHoy(listaCompletaFirebase, ids);
                    }
                });
    }

    private void sincronizarCalificacionesDesdeFirestore(String uid) {
        FirebaseFirestore.getInstance()
                .collection("calificaciones")
                .whereEqualTo("clienteUid", uid)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        List<Calificacion> cals = query.toObjects(Calificacion.class);
                        ExecutorUtils.getExecutor().execute(() -> {
                            AppDatabase db = AppDatabase.getInstance(this);
                            for (Calificacion c : cals) db.calificacionDao().insertar(c);
                            runOnUiThread(() -> cargarServicios());
                        });
                    } else {
                        cargarServicios();
                    }
                })
                .addOnFailureListener(e -> cargarServicios());
    }

    /** Devuelve el valor o "—" si es null. Nunca retorna null. */
    private static String nn(String s) {
        return s != null ? s : "—";
    }
}