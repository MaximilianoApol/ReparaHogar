package com.example.reparahogar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * LoginFragment — pantalla de inicio de sesión.
 * Se muestra como primer Fragment dentro de MainActivity.
 * Al autenticarse con Firebase Auth, navega a:
 *   - DetalleHogar (si el usuario es de tipo "hogar")
 *   - Detalle_Servicio (si el usuario es de tipo "proveedor")
 */
public class LoginFragment extends Fragment {

    private TextInputLayout layoutEmail, layoutPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnIniciarSesion;
    private android.widget.TextView txtRegistrate;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Cambia R.layout.activity_main por R.layout.fragment_login
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        layoutEmail    = view.findViewById(R.id.layoutEmail);
        layoutPassword = view.findViewById(R.id.layoutPassword);
        etEmail        = view.findViewById(R.id.etEmail);
        etPassword     = view.findViewById(R.id.etPassword);
        btnIniciarSesion = view.findViewById(R.id.btnIniciarSesion);
        txtRegistrate    = view.findViewById(R.id.txtRegistrate);

        btnIniciarSesion.setOnClickListener(v -> intentarLogin());

        txtRegistrate.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_container, new RegistroFragment())
                        .addToBackStack(null)
                        .commit()
        );

        return view;
    }

    // ─── Lógica de login ───────────────────────────────────────────────────────

    private void intentarLogin() {
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError("Ingresa tu correo");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            layoutPassword.setError("Ingresa tu contraseña");
            return;
        }

        layoutEmail.setError(null);
        layoutPassword.setError(null);
        btnIniciarSesion.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnIniciarSesion.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) consultarTipoUsuario(user.getUid());
                    } else {
                        Toast.makeText(getContext(),
                                "Error: " + (task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Credenciales incorrectas"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Consulta Firestore para saber si el usuario es dueño de hogar o proveedor
     * y redirige a la pantalla correspondiente.
     */
    private void consultarTipoUsuario(String uid) {
        db.collection("usuarios").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String tipo = doc.getString("tipo"); // "hogar" o "proveedor"
                    if ("proveedor".equals(tipo)) {
                        abrirActividad(Detalle_Servicio.class);
                    } else {
                        abrirActividad(DetalleHogar.class);
                    }
                })
                .addOnFailureListener(e -> {
                    // Por defecto enviamos a DetalleHogar
                    abrirActividad(DetalleHogar.class);
                });
    }

    private void abrirActividad(Class<?> cls) {
        android.content.Intent intent = new android.content.Intent(getContext(), cls);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
}