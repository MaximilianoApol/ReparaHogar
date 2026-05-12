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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegistroFragment extends Fragment {

    private TextInputEditText etNombre, etCorreo, etTelefono, etPassword, etConfirmPassword;
    private TextInputLayout tilNombre, tilCorreo, tilTelefono, tilPassword, tilConfirmPassword;
    private MaterialButton btnLogin;
    private MaterialCheckBox chkOwner, chkProvider;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DataRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_crear_cuenta, container, false);

        mAuth      = FirebaseAuth.getInstance();
        db         = FirebaseFirestore.getInstance();
        repository = new DataRepository(requireContext());

        // Referencias de UI
        tilNombre          = view.findViewById(R.id.tilNombre);
        tilCorreo          = view.findViewById(R.id.tilCorreo);
        tilTelefono        = view.findViewById(R.id.tilTelefono);
        tilPassword        = view.findViewById(R.id.tilPassword);
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);
        chkOwner           = view.findViewById(R.id.chkOwner);
        chkProvider        = view.findViewById(R.id.chkProvider);
        btnLogin           = view.findViewById(R.id.btnLogin);

        etNombre          = (TextInputEditText) tilNombre.getEditText();
        etCorreo          = (TextInputEditText) tilCorreo.getEditText();
        etTelefono        = (TextInputEditText) tilTelefono.getEditText();
        etPassword        = (TextInputEditText) tilPassword.getEditText();
        etConfirmPassword = (TextInputEditText) tilConfirmPassword.getEditText();

        // Lógica de CheckBoxes excluyentes (opcional pero recomendado)
        chkOwner.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) chkProvider.setChecked(false);
        });
        chkProvider.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) chkOwner.setChecked(false);
        });

        btnLogin.setText("Crear cuenta");
        btnLogin.setOnClickListener(v -> registrarUsuario());

        return view;
    }

    private void registrarUsuario() {
        String nombre    = texto(etNombre);
        String correo    = texto(etCorreo);
        String telefono  = texto(etTelefono);
        String password  = texto(etPassword);
        String confirm   = texto(etConfirmPassword);

        // Validaciones básicas
        if (TextUtils.isEmpty(nombre))   { tilNombre.setError("Campo requerido"); return; }
        if (TextUtils.isEmpty(correo))   { tilCorreo.setError("Campo requerido"); return; }
        if (TextUtils.isEmpty(telefono)) { tilTelefono.setError("Campo requerido"); return; }
        if (TextUtils.isEmpty(password)) { tilPassword.setError("Campo requerido"); return; }
        if (!password.equals(confirm))   { tilConfirmPassword.setError("Las contraseñas no coinciden"); return; }
        if (!chkOwner.isChecked() && !chkProvider.isChecked()) {
            Toast.makeText(getContext(), "Selecciona si eres Usuario o Proveedor", Toast.LENGTH_SHORT).show();
            return;
        }

        tilNombre.setError(null);
        tilCorreo.setError(null);
        tilTelefono.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        btnLogin.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(correo, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        String rol = chkProvider.isChecked() ? "proveedor" : "hogar";

                        // 1. Crear el objeto Usuario basado en el nuevo modelo
                        Usuario nuevoUsuario = new Usuario(uid, nombre, correo, rol);
                        nuevoUsuario.setTelefono(telefono);

                        // 2. Guardar en Firestore (Set en lugar de Update para crear el documento)
                        db.collection("usuarios").document(uid).set(nuevoUsuario)
                                .addOnSuccessListener(aVoid -> {

                                    // 3. Si es proveedor, crear también su perfil de proveedor inicial
                                    if ("proveedor".equals(rol)) {
                                        crearPerfilProveedorBase(uid, nombre);
                                    }

                                    // 4. Guardar en Room localmente
                                    repository.registrarUsuarioCompleto(nuevoUsuario);

                                    Toast.makeText(getContext(), "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                                    navegarSegunRol(rol);
                                })
                                .addOnFailureListener(e -> {
                                    btnLogin.setEnabled(true);
                                    Toast.makeText(getContext(), "Error al guardar perfil", Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        btnLogin.setEnabled(true);
                        Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void crearPerfilProveedorBase(String uid, String nombre) {
        Proveedor p = new Proveedor();
        p.setIdProveedor(uid);
        p.setNombre(nombre);
        p.setCalificacionPromedio(5.0f); // Empiezan con buena nota
        p.setVerificado(false);

        db.collection("proveedores").document(uid).set(p);
    }

    private void navegarSegunRol(String rol) {
        android.content.Intent intent;
        if ("proveedor".equals(rol)) {
            // Ajustado al nombre de la actividad que mencionaste anteriormente
            intent = new android.content.Intent(getContext(), Detalle_Servicio.class);
        } else {
            intent = new android.content.Intent(getContext(), DetalleHogar.class);
        }
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }

    private String texto(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
}