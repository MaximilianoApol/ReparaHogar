package com.example.reparahogar;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Establecemos el layout del contenedor
        setContentView(R.layout.activity_main);

        // 2. Cargamos el Fragment de Verificación inmediatamente
        // Asegúrate de que en activity_main.xml el ID sea 'main_container'
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new VerificacionFragment())
                    .commit();
        }
    }
}