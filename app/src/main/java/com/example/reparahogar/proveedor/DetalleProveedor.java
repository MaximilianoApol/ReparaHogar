package com.example.reparahogar.proveedor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.MainActivity;
import com.example.reparahogar.R;
import com.example.reparahogar.fragment_servicio_confirmado; // Asegura que este nombre sea exacto
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DetalleProveedor extends AppCompatActivity {

    private RecyclerView rvAgendaProveedor;
    private ProveedorAdapter adapter;
    private List<Cita> listaCitas;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_proveedor);

        db = FirebaseFirestore.getInstance();
        listaCitas = new ArrayList<>();

        // 1. Configurar UI básica
        ImageButton btnPerfil = findViewById(R.id.btnPerfil);
        MaterialToolbar toolbar = findViewById(R.id.toolbarProveedor);

        toolbar.setNavigationOnClickListener(v -> cerrarSesion());

        // 2. Configurar RecyclerView
        rvAgendaProveedor = findViewById(R.id.rvAgendaProveedor);
        rvAgendaProveedor.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar el adapter con el Listener
        adapter = new ProveedorAdapter(listaCitas, new ProveedorAdapter.OnCitaClickListener() {
            @Override
            public void onConfirmarClick(Cita cita) {
                actualizarEstadoCita(cita.getIdCita(), "confirmado");
            }

            @Override
            public void onFinalizarClick(Cita cita) {
                abrirFragmentConfirmado(cita);
            }
        });

        rvAgendaProveedor.setAdapter(adapter);

        // 3. Cargar datos desde Firebase
        cargarCitasDesdeFirestore();
    }

    private void cargarCitasDesdeFirestore() {
        String uidProveedor = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Buscamos solo las citas asignadas a este proveedor
        db.collection("citas")
                .whereEqualTo("idProveedor", uidProveedor)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    listaCitas.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Cita cita = doc.toObject(Cita.class);
                        cita.setIdCita(doc.getId()); // Aseguramos tener el ID del documento
                        listaCitas.add(cita);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void actualizarEstadoCita(String idCita, String nuevoEstado) {
        db.collection("citas").document(idCita)
                .update("estado", nuevoEstado)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(DetalleProveedor.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void abrirFragmentConfirmado(Cita cita) {
        // Instancia del fragmento con el diseño verde de éxito
        fragment_servicio_confirmado fragment = new fragment_servicio_confirmado();

        Bundle args = new Bundle();
        args.putString("idCita", cita.getIdCita());
        args.putString("categoria", cita.getCategoria());
        args.putString("fecha", cita.getFecha());
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_proveedor, fragment) // 'main' debe ser el id del contenedor en activity_detalle_proveedor.xml
                .addToBackStack(null)
                .commit();
    }
}