package com.example.reparahogar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar; // Importante
import com.google.firebase.auth.FirebaseAuth;

public class DetalleHogar extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_hogar);
        ImageButton btnPerfil = findViewById(R.id.btnPerfil);

        // 1. Referenciar el Toolbar por su ID
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        // 2. Configurar el click en el icono de navegación (ic_exit)
        toolbar.setNavigationOnClickListener(v -> {
            cerrarSesion();
        });

        findViewById(R.id.chipPlomeria).setOnClickListener(v -> irASeleccion("Agua / Plomería"));
        findViewById(R.id.chipElectricidad).setOnClickListener(v -> irASeleccion("Electricidad"));
        findViewById(R.id.chipGas).setOnClickListener(v -> irASeleccion("Gas"));


    }

    private void cerrarSesion() {
        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut();

        // Redirigir al MainActivity (Login) y limpiar la pila de actividades
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

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mi_hogar, fragment) // Asegúrate de que el id sea el layout raíz de DetalleHogar
                .addToBackStack(null)
                .commit();
    }

}