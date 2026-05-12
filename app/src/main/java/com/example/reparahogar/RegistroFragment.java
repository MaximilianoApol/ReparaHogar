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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Pantalla de registro de usuario nuevo.
 * Layout: activity_crear_cuenta.xml ✅ (tu layout existente)
 *
 * IDs usados:
 *   tilNombre, tilCorreo, tilTelefono → TextInputLayout
 *   tilPassword, tilConfirmPassword   → TextInputLayout
 *   chkOwner    → "Soy dueño de casa"      → CLIENTE
 *   chkProvider → "Soy proveedor de servicio" → PROVEEDOR
 *   btnLogin    → botón de acción (cambiamos texto a "Crear cuenta")
 *
 * Flujo:
 *   CLIENTE   → AuthViewModel.registrar() → navegarCliente → DetalleHogar
 *   PROVEEDOR → AuthViewModel.registrar() → crea perfil base → navegarVerificacion
 */
public class RegistroFragment extends Fragment {

    private AuthViewModel authViewModel;

    // Vistas — coinciden con los IDs de activity_crear_cuenta.xml
    private TextInputLayout   tilNombre;
    private TextInputLayout   tilCorreo;
    private TextInputLayout   tilTelefono;
    private TextInputLayout   tilPassword;
    private TextInputLayout   tilConfirmPassword;
    private TextInputEditText etNombre;
    private TextInputEditText etCorreo;
    private TextInputEditText etTelefono;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialCheckBox  chkOwner;
    private MaterialCheckBox  chkProvider;
    private MaterialButton    btnCrearCuenta;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Reutilizamos activity_crear_cuenta.xml que ya tienes
        return inflater.inflate(R.layout.fragment_registro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Mismo ViewModel que MainActivity y LoginFragment
        authViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(AuthViewModel.class);

        enlazarVistas(view);
        configurarCheckBoxes();
        configurarListeners();
        observarViewModel();
    }

    // ── Vistas ────────────────────────────────────────────────────────────────

    private void enlazarVistas(View view) {
        tilNombre          = view.findViewById(R.id.tilNombre);
        tilCorreo          = view.findViewById(R.id.tilCorreo);
        tilTelefono        = view.findViewById(R.id.tilTelefono);
        tilPassword        = view.findViewById(R.id.tilPassword);
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);
        chkOwner           = view.findViewById(R.id.chkOwner);
        chkProvider        = view.findViewById(R.id.chkProvider);
        btnCrearCuenta     = view.findViewById(R.id.btnLogin);

        // Los EditText viven dentro de los TextInputLayout
        etNombre          = (TextInputEditText) tilNombre.getEditText();
        etCorreo          = (TextInputEditText) tilCorreo.getEditText();
        etTelefono        = (TextInputEditText) tilTelefono.getEditText();
        etPassword        = (TextInputEditText) tilPassword.getEditText();
        etConfirmPassword = (TextInputEditText) tilConfirmPassword.getEditText();

        // El layout tiene texto "Iniciar sesión" por defecto — lo corregimos
        btnCrearCuenta.setText("Crear cuenta");
    }

    // ── CheckBoxes excluyentes ────────────────────────────────────────────────

    private void configurarCheckBoxes() {
        // Solo uno puede estar marcado a la vez
        chkOwner.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) chkProvider.setChecked(false);
        });
        chkProvider.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) chkOwner.setChecked(false);
        });
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void configurarListeners() {
        btnCrearCuenta.setOnClickListener(v -> intentarRegistro());

        // Limpiar errores al escribir
        etNombre.setOnFocusChangeListener((v, f) -> { if (f) tilNombre.setError(null); });
        etCorreo.setOnFocusChangeListener((v, f) -> { if (f) tilCorreo.setError(null); });
        etTelefono.setOnFocusChangeListener((v, f) -> { if (f) tilTelefono.setError(null); });
        etPassword.setOnFocusChangeListener((v, f) -> { if (f) tilPassword.setError(null); });
        etConfirmPassword.setOnFocusChangeListener((v, f) -> {
            if (f) tilConfirmPassword.setError(null);
        });
    }

    // ── Observadores ─────────────────────────────────────────────────────────

    private void observarViewModel() {

        authViewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            btnCrearCuenta.setEnabled(!cargando);
            btnCrearCuenta.setText(cargando ? "Creando cuenta..." : "Crear cuenta");
        });

        authViewModel.getErrorMensaje().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show();
                authViewModel.limpiarError();
            }
        });

        // La navegación (Cliente → DetalleHogar, Proveedor → VerificacionFragment)
        // la maneja MainActivity — no duplicamos aquí.
    }

    // ── Registro ──────────────────────────────────────────────────────────────

    private void intentarRegistro() {
        // Limpiar errores visuales
        limpiarErrores();

        String nombre   = texto(etNombre);
        String correo   = texto(etCorreo);
        String telefono = texto(etTelefono);
        String password = texto(etPassword);
        String confirm  = texto(etConfirmPassword);

        // Validaciones visuales inmediatas
        if (nombre.isEmpty())   { tilNombre.setError("Campo requerido");   return; }
        if (correo.isEmpty())   { tilCorreo.setError("Campo requerido");   return; }
        if (telefono.isEmpty()) { tilTelefono.setError("Campo requerido"); return; }
        if (password.isEmpty()) { tilPassword.setError("Campo requerido"); return; }

        if (!password.equals(confirm)) {
            tilConfirmPassword.setError("Las contraseñas no coinciden");
            return;
        }

        if (!chkOwner.isChecked() && !chkProvider.isChecked()) {
            Snackbar.make(requireView(),
                    "Selecciona si eres dueño de casa o proveedor",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        // Delegamos toda la lógica real al ViewModel
        boolean esProveedor = chkProvider.isChecked();
        authViewModel.registrar(nombre, correo, telefono, password, esProveedor);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void limpiarErrores() {
        tilNombre.setError(null);
        tilCorreo.setError(null);
        tilTelefono.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private String texto(TextInputEditText et) {
        if (et == null) return "";
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}