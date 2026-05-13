package com.example.reparahogar;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.reparahogar.model.Proveedor;
import com.example.reparahogar.model.Usuario;
import com.example.reparahogar.repository.ProveedorRepository;
import com.example.reparahogar.repository.UsuarioRepository;
import com.example.reparahogar.viewmodel.UsuarioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Perfil del PROVEEDOR.
 * Muestra nombre, teléfono y correo desde Firestore/Room.
 * Los campos con ic_edit son editables al tocarlos.
 */
public class FragmentPerfilProveedor extends Fragment {

    private UsuarioViewModel usuarioViewModel;
    private Usuario usuarioCacheado;
    private Proveedor proveedorCacheado;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_proveedor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        usuarioViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication()))
                .get(UsuarioViewModel.class);

        // Toolbar back
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarProveedor);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        // Referencias a campos de texto en la card "Contacto"
        TextView txtNombre  = view.findViewById(R.id.txtNombre);
        TextView txtTel     = view.findViewById(R.id.txtTelefono);
        TextView txtCorreo  = view.findViewById(R.id.txtCorreo);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (!uid.isEmpty()) {
            usuarioViewModel.getUsuario(uid).observe(getViewLifecycleOwner(), usuario -> {
                if (usuario == null) return;
                usuarioCacheado = usuario;
                if (txtNombre != null) txtNombre.setText(usuario.getNombre() != null ? usuario.getNombre() : "—");
                if (txtTel    != null) txtTel.setText(usuario.getTelefono() != null ? usuario.getTelefono() : "—");
                if (txtCorreo != null) txtCorreo.setText(usuario.getCorreo() != null ? usuario.getCorreo() : "—");
            });
        }

        // ── Edición: al tocar el ícono de lápiz junto a Nombre ───────────────
        View icEditNombre = view.findViewById(R.id.icEdit);
        if (icEditNombre != null && txtNombre != null) {
            icEditNombre.setOnClickListener(v -> activarEdicion(txtNombre, "nombre"));
        }

        // ── Edición: al tocar el ícono de lápiz junto a Teléfono ─────────────
        // El layout del proveedor tiene icPhone en el bloque de contacto
        View icEditTel = view.findViewById(R.id.icPhone);
        if (icEditTel != null && txtTel != null) {
            icEditTel.setOnClickListener(v -> activarEdicion(txtTel, "telefono"));
        }
    }

    /** Pone el campo en modo editable y guarda al perder foco */
    private void activarEdicion(TextView campo, String tipoCampo) {
        campo.setFocusable(true);
        campo.setFocusableInTouchMode(true);
        campo.requestFocus();

        campo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                guardarCambio(tipoCampo, campo.getText().toString().trim());
                campo.setFocusable(false);
                campo.setFocusableInTouchMode(false);
            }
        });
    }

    private void guardarCambio(String tipoCampo, String nuevoValor) {
        if (usuarioCacheado == null || nuevoValor.isEmpty()) return;

        switch (tipoCampo) {
            case "nombre":   usuarioCacheado.setNombre(nuevoValor);   break;
            case "telefono": usuarioCacheado.setTelefono(nuevoValor); break;
        }

        new UsuarioRepository(requireContext()).actualizar(usuarioCacheado,
                new UsuarioRepository.OnResultListener() {
                    @Override public void onSuccess() {
                        if (getContext() != null)
                            Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onError(String mensaje) {
                        if (getContext() != null)
                            Toast.makeText(getContext(), "Error: " + mensaje, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}