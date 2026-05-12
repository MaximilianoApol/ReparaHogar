package com.example.reparahogar;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.reparahogar.proveedor.DetalleProveedor;
import com.example.reparahogar.viewmodel.AuthViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;

/**
 * Punto de entrada de la app.
 *
 * Responsabilidades:
 *  1. Crea el AuthViewModel a nivel de Activity para que LoginFragment
 *     y RegistroFragment puedan compartirlo.
 *  2. Si hay sesión activa → navega directo.
 *  3. Si no hay sesión     → carga LoginFragment.
 *
 * CLAVE: los Fragments obtienen el mismo AuthViewModel usando
 *   ViewModelFactory.obtener(requireActivity(), AuthViewModel.class)
 * Eso garantiza que observan los mismos LiveData que emite esta Activity.
 *
 * activity_main.xml ya tiene FrameLayout id="main_container" ✅
 */
public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ViewModel a nivel de Activity — lo comparten todos los Fragments
        authViewModel = new ViewModelProvider(
                this,
                new ViewModelFactory(getApplication())
        ).get(AuthViewModel.class);

        observarNavegacion();

        authViewModel.verificarSesionActiva();

        // Carga LoginFragment solo si no hay sesión y es la primera creación
        if (!authViewModel.haySesionActiva() && savedInstanceState == null) {
            cargarFragment(new LoginFragment(), false);
        }
    }

    // ── Observadores ─────────────────────────────────────────────────────────

    private void observarNavegacion() {

        authViewModel.getNavegarCliente().observe(this, navegar -> {
            if (Boolean.TRUE.equals(navegar)) irA(DetalleHogar.class);
        });

        authViewModel.getNavegarProveedor().observe(this, navegar -> {
            if (Boolean.TRUE.equals(navegar)) irA(DetalleProveedor.class);
        });

        authViewModel.getNavegarVerificacion().observe(this, navegar -> {
            if (Boolean.TRUE.equals(navegar)) {
                cargarFragment(new VerificacionFragment(), true);
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void cargarFragment(androidx.fragment.app.Fragment fragment,
                                boolean agregarAlBackStack) {
        androidx.fragment.app.FragmentTransaction tx =
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_container, fragment);
        if (agregarAlBackStack) tx.addToBackStack(null);
        tx.commit();
    }

    private void irA(Class<?> destino) {
        Intent intent = new Intent(this, destino);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}