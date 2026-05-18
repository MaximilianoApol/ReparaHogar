package com.example.reparahogar;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.reparahogar.model.Usuario;
import com.example.reparahogar.repository.UsuarioRepository;
import com.example.reparahogar.viewmodel.UsuarioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.firebase.auth.FirebaseAuth;

public class FragmentPerfil extends Fragment {

    private UsuarioViewModel usuarioViewModel;
    private Usuario usuarioActual; // Para comparar cambios
    private Button btnGuardar;
    private TextView txtNombre, txtTel, txtCorreo, txtLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        usuarioViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(UsuarioViewModel.class);

        // Referencias UI
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        txtNombre = view.findViewById(R.id.txtNombre);
        txtTel = view.findViewById(R.id.txtTelefono);
        txtCorreo = view.findViewById(R.id.txtCorreo);
        txtLabel = view.findViewById(R.id.txtUserLabel);
        btnGuardar = view.findViewById(R.id.btnGuardar); // Asegúrate de tener este ID en tu XML

        // Íconos de edición (Asumiendo IDs basados en tu descripción)
        View editNombre = view.findViewById(R.id.btnEditNombre);
        View editTelefono = view.findViewById(R.id.btnEditTelefono);

        if (btnBack != null) btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Cargar Datos
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            usuarioViewModel.getUsuario(uid).observe(getViewLifecycleOwner(), usuario -> {
                if (usuario == null) return;
                this.usuarioActual = usuario; // Guardamos el estado original
                actualizarInterfaz(usuario);
            });
        }

        // Configurar clics de edición
        if (editNombre != null) editNombre.setOnClickListener(v -> mostrarDialogoEdicion("Nombre", txtNombre.getText().toString()));
        if (editTelefono != null) editTelefono.setOnClickListener(v -> mostrarDialogoEdicion("Teléfono", txtTel.getText().toString()));

        // Acción Guardar
        if (btnGuardar != null) {
            btnGuardar.setOnClickListener(v -> guardarCambios());
        }
    }

    private void actualizarInterfaz(Usuario u) {
        txtNombre.setText(u.getNombre() != null ? u.getNombre() : "—");
        txtTel.setText(u.getTelefono() != null ? u.getTelefono() : "—");
        txtCorreo.setText(u.getCorreo() != null ? u.getCorreo() : "—");
        txtLabel.setText(u.getNombre());
        btnGuardar.setVisibility(View.GONE); // Ocultar al cargar/refrescar
    }

    private void mostrarDialogoEdicion(String campo, String valorActual) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Editar " + campo);

        final EditText input = new EditText(requireContext());
        input.setText(valorActual);

        // Aplicar filtros según el campo
        if (campo.equals("Nombre")) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        } else {
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        }

        builder.setView(input);
        builder.setPositiveButton("Listo", (dialog, which) -> {
            String nuevoValor = input.getText().toString().trim();
            if (validarYAsignar(campo, nuevoValor)) {
                detectarCambios();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private boolean validarYAsignar(String campo, String valor) {
        if (valor.isEmpty()) {
            Toast.makeText(getContext(), "El campo no puede estar vacío", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (campo.equals("Teléfono") && valor.length() != 10) {
            Toast.makeText(getContext(), "El teléfono debe tener 10 dígitos", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (campo.equals("Nombre")) {
            txtNombre.setText(valor);
        } else {
            txtTel.setText(valor);
        }
        return true;
    }

    private void detectarCambios() {
        if (usuarioActual == null) return;

        String nombreUI = txtNombre.getText().toString();
        String telUI = txtTel.getText().toString();

        // Si el texto actual en pantalla es diferente al de la base de datos
        boolean huboCambio = !nombreUI.equals(usuarioActual.getNombre()) ||
                !telUI.equals(usuarioActual.getTelefono());

        btnGuardar.setVisibility(huboCambio ? View.VISIBLE : View.GONE);
    }

    // En FragmentPerfil.java
    private void guardarCambios() {
        if (usuarioActual == null) return;

        // Actualizamos el objeto con lo que hay en los TextViews
        usuarioActual.setNombre(txtNombre.getText().toString());
        usuarioActual.setTelefono(txtTel.getText().toString());

        // Llamamos al ViewModel pasando un listener
        usuarioViewModel.updateUsuario(usuarioActual, new UsuarioRepository.OnResultListener() {
            @Override
            public void onSuccess() {
                // Esto corre en el hilo principal gracias a los listeners de Firebase
                btnGuardar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String mensaje) {
                Toast.makeText(getContext(), "Error: " + mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }
}