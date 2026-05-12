package com.example.reparahogar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.reparahogar.viewmodel.AuthViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Pantalla de inicio de sesión.
 * Layout: fragment_login.xml ✅
 *
 * Obtiene el AuthViewModel desde la Activity para compartirlo —
 * así MainActivity recibe los LiveData de navegación correctamente.
 */
public class LoginFragment extends Fragment {

    private AuthViewModel authViewModel;

    private TextInputLayout   layoutEmail;
    private TextInputLayout   layoutPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton    btnIniciarSesion;
    private android.widget.TextView txtRegistrate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ⚠️ requireActivity() como owner → mismo ViewModel que MainActivity
        authViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(AuthViewModel.class);

        enlazarVistas(view);
        configurarListeners();
        observarViewModel();
    }

    // ── Vistas ────────────────────────────────────────────────────────────────

    private void enlazarVistas(View view) {
        layoutEmail      = view.findViewById(R.id.layoutEmail);
        layoutPassword   = view.findViewById(R.id.layoutPassword);
        etEmail          = view.findViewById(R.id.etEmail);
        etPassword       = view.findViewById(R.id.etPassword);
        btnIniciarSesion = view.findViewById(R.id.btnIniciarSesion);
        txtRegistrate    = view.findViewById(R.id.txtRegistrate);
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void configurarListeners() {

        btnIniciarSesion.setOnClickListener(v -> intentarLogin());

        txtRegistrate.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_container, new RegistroFragment())
                        .addToBackStack(null)
                        .commit()
        );

        // Limpiar error al empezar a escribir
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) layoutEmail.setError(null);
        });
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) layoutPassword.setError(null);
        });
    }

    // ── Observadores ─────────────────────────────────────────────────────────

    private void observarViewModel() {

        // Botón deshabilitado mientras carga
        authViewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            btnIniciarSesion.setEnabled(!cargando);
            btnIniciarSesion.setText(cargando ? "Verificando..." : "Iniciar sesión");
        });

        // Error en Snackbar — se limpia del ViewModel después de mostrarlo
        authViewModel.getErrorMensaje().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show();
                authViewModel.limpiarError();
            }
        });

        // La navegación la maneja MainActivity — no duplicamos lógica aquí.
        // MainActivity observa getNavegarCliente / getNavegarProveedor /
        // getNavegarVerificacion y actúa en consecuencia.
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    private void intentarLogin() {
        layoutEmail.setError(null);
        layoutPassword.setError(null);

        String correo   = texto(etEmail);
        String password = texto(etPassword);

        // Validación visual inmediata antes de llamar al ViewModel
        if (correo.isEmpty()) {
            layoutEmail.setError("Ingresa tu correo");
            return;
        }
        if (password.isEmpty()) {
            layoutPassword.setError("Ingresa tu contraseña");
            return;
        }

        authViewModel.login(correo, password);
    }

    private String texto(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}