package com.example.reparahogar;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.reparahogar.model.Usuario;
import com.example.reparahogar.repository.UsuarioRepository;
import com.example.reparahogar.viewmodel.UsuarioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class FragmentPerfilProveedor extends Fragment {

    private UsuarioViewModel usuarioViewModel;
    private Usuario usuarioCacheado;

    private TextInputLayout tilNombre, tilDescripcion, tilTelefono, tilHoraInicio, tilHoraFin;
    private TextInputEditText etNombre, etDescripcion, etTelefono, etCorreo, etHoraInicio, etHoraFin;
    private MaterialButton btnGuardar;

    private static final int MAX_TELEFONO = 10;
    private static final int MAX_PALABRAS = 30;
    private int scrollPosicionOriginal = 0;

    // Esta bandera evita que el botón aparezca mientras se cargan los datos de Firebase
    private boolean estaCargandoDatos = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_proveedor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        usuarioViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(UsuarioViewModel.class);

        // --- Inicialización de Vistas ---
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarProveedor);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        tilNombre = view.findViewById(R.id.tilNombre);
        tilDescripcion = view.findViewById(R.id.tilDescripcion);
        tilTelefono = view.findViewById(R.id.tilTelefono);
        tilHoraInicio = view.findViewById(R.id.tilHoraInicio);
        tilHoraFin = view.findViewById(R.id.tilHoraFin);

        etNombre = view.findViewById(R.id.etNombre);
        etDescripcion = view.findViewById(R.id.etDescripcion);
        etTelefono = view.findViewById(R.id.etTelefono);
        etCorreo = view.findViewById(R.id.etCorreo);
        etHoraInicio = view.findViewById(R.id.etHoraInicio);
        etHoraFin = view.findViewById(R.id.etHoraFin);

        btnGuardar = view.findViewById(R.id.btnGuardarPerfil);
        btnGuardar.setVisibility(View.GONE); // Oculto inicialmente

        // ─────────────────────────────────────────────────────────────
        // LISTENERS DE CAMBIOS (TextWatchers)
        // ─────────────────────────────────────────────────────────────

        etNombre.addTextChangedListener(new SimpleTextWatcher());

        etDescripcion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!estaCargandoDatos) mostrarBotonGuardar();
                int palabras = contarPalabras(s.toString());
                tilDescripcion.setHelperText(palabras + " / " + MAX_PALABRAS + " palabras");
                if (palabras > MAX_PALABRAS) {
                    tilDescripcion.setError("Máximo " + MAX_PALABRAS + " palabras.");
                } else {
                    tilDescripcion.setError(null);
                }
            }
        });

        etTelefono.setFilters(new InputFilter[]{new DigitsOnlyFilter()});
        etTelefono.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!estaCargandoDatos) mostrarBotonGuardar();
                String telefono = s.toString();
                if (telefono.length() > MAX_TELEFONO) {
                    tilTelefono.setError("Máximo " + MAX_TELEFONO + " dígitos");
                    s.delete(MAX_TELEFONO, telefono.length());
                } else {
                    tilTelefono.setError(null);
                    tilTelefono.setHelperText(telefono.length() + " / " + MAX_TELEFONO);
                }
            }
        });

        // ─────────────────────────────────────────────────────────────
        // ICONOS DE EDICIÓN
        // ─────────────────────────────────────────────────────────────

        tilNombre.setEndIconOnClickListener(v -> activarEdicion(etNombre, tilNombre));
        tilDescripcion.setEndIconOnClickListener(v -> activarEdicion(etDescripcion, tilDescripcion));
        tilTelefono.setEndIconOnClickListener(v -> activarEdicion(etTelefono, tilTelefono));
        tilHoraInicio.setEndIconOnClickListener(v -> mostrarTimePicker(etHoraInicio));
        tilHoraFin.setEndIconOnClickListener(v -> mostrarTimePicker(etHoraFin));

        // ─────────────────────────────────────────────────────────────
        // CARGAR DATOS
        // ─────────────────────────────────────────────────────────────

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (!uid.isEmpty()) {
            estaCargandoDatos = true; // Iniciamos carga

            usuarioViewModel.getUsuario(uid).observe(getViewLifecycleOwner(), usuario -> {
                if (usuario == null) return;
                usuarioCacheado = usuario;
                etNombre.setText(usuario.getNombre() != null ? usuario.getNombre() : "");
                etTelefono.setText(usuario.getTelefono() != null ? usuario.getTelefono() : "");
                etCorreo.setText(usuario.getCorreo() != null ? usuario.getCorreo() : "");
            });

            FirebaseFirestore.getInstance().collection("proveedores").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            if (doc.getString("descripcion") != null) etDescripcion.setText(doc.getString("descripcion"));
                            if (doc.getString("horaInicio") != null) etHoraInicio.setText(doc.getString("horaInicio"));
                            if (doc.getString("horaFin") != null) etHoraFin.setText(doc.getString("horaFin"));
                        }
                        // Damos un pequeño respiro para que los TextWatchers no se activen con el setResidue
                        view.postDelayed(() -> estaCargandoDatos = false, 500);
                    });
        }

        btnGuardar.setOnClickListener(v -> guardarCambios(uid));
    }

    private void mostrarBotonGuardar() {
        if (btnGuardar != null && btnGuardar.getVisibility() != View.VISIBLE) {
            btnGuardar.setVisibility(View.VISIBLE);
        }
    }

    private void activarEdicion(TextInputEditText campo, TextInputLayout til) {
        boolean estaEditando = campo.isFocusable();
        View rootView = getView();
        if (rootView == null) return;

        NestedScrollView scrollView = rootView.findViewById(R.id.nestedScrollViewPerfil);
        // Buscamos el contenedor interno del scroll para darle padding
        View contenedorInterno = scrollView.getChildAt(0);

        if (!estaEditando) {
            // --- EMPEZAR EDICIÓN ---
            campo.setFocusable(true);
            campo.setFocusableInTouchMode(true);
            campo.requestFocus();

            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(campo, InputMethodManager.SHOW_IMPLICIT);

            if (campo.getId() == R.id.etTelefono && scrollView != null) {
                // 1. Damos padding solo ahora para que el usuario pueda subir
                contenedorInterno.setPadding(0, 0, 0, 1000); // 1000px de espacio extra temporal

                scrollPosicionOriginal = scrollView.getScrollY();
                til.postDelayed(() -> {
                    int relativeTop = 0;
                    View current = til;
                    while (current != null && current != scrollView) {
                        relativeTop += current.getTop();
                        current = (View) current.getParent();
                    }
                    scrollView.smoothScrollTo(0, relativeTop - 20);
                }, 350);
            }

            campo.setSelection(campo.getText() != null ? campo.getText().length() : 0);
            til.setEndIconDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_check));

        } else {
            // --- TERMINAR EDICIÓN ---
            campo.setFocusable(false);
            campo.setFocusableInTouchMode(false);
            campo.clearFocus();
            til.setEndIconDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit));

            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(campo.getWindowToken(), 0);

            if (campo.getId() == R.id.etTelefono && scrollView != null) {
                // Regresamos a la posición original
                scrollView.smoothScrollTo(0, scrollPosicionOriginal);

                // 2. Quitamos el padding después de que termine la animación de scroll
                scrollView.postDelayed(() -> {
                    contenedorInterno.setPadding(0, 0, 0, 0);
                }, 500);
            }
        }
    }

    private void mostrarTimePicker(TextInputEditText campo) {
        int horaActual = 9, minActual = 0;
        new TimePickerDialog(requireContext(), (tp, hourOfDay, minute) -> {
            String amPm = hourOfDay < 12 ? "AM" : "PM";
            int hora12 = hourOfDay == 0 ? 12 : (hourOfDay > 12 ? hourOfDay - 12 : hourOfDay);
            campo.setText(String.format(Locale.getDefault(), "%02d:%02d %s", hora12, minute, amPm));

            if (!estaCargandoDatos) mostrarBotonGuardar();
        }, horaActual, minActual, false).show();
    }

    private void guardarCambios(String uid) {
        if (uid.isEmpty()) return;
        String nombre = texto(etNombre), descripcion = texto(etDescripcion),
                telefono = texto(etTelefono), hIni = texto(etHoraInicio), hFin = texto(etHoraFin);

        if (!validarCampos(nombre, descripcion, telefono)) return;

        if (usuarioCacheado != null) {
            usuarioCacheado.setNombre(nombre);
            usuarioCacheado.setTelefono(telefono);
            new UsuarioRepository(requireContext()).actualizar(usuarioCacheado, null);
        }

        FirebaseFirestore.getInstance().collection("proveedores").document(uid)
                .update("nombre", nombre, "descripcion", descripcion, "telefono", telefono, "horaInicio", hIni, "horaFin", hFin)
                .addOnSuccessListener(unused -> {
                    // Verificar que el fragmento sigue vivo antes de usar getContext()
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "✓ Perfil actualizado",
                                Toast.LENGTH_SHORT).show();
                    }
                    if (btnGuardar != null) btnGuardar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validarCampos(String n, String d, String t) {
        boolean v = true;
        if (n.isEmpty()) { tilNombre.setError("Campo obligatorio"); v = false; } else tilNombre.setError(null);
        if (contarPalabras(d) > MAX_PALABRAS) { tilDescripcion.setError("Demasiadas palabras"); v = false; } else tilDescripcion.setError(null);
        if (!t.isEmpty() && t.length() != MAX_TELEFONO) { tilTelefono.setError("Deben ser 10 dígitos"); v = false; } else tilTelefono.setError(null);
        return v;
    }

    private int contarPalabras(String t) {
        return (t == null || t.trim().isEmpty()) ? 0 : t.trim().split("\\s+").length;
    }

    private String texto(TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }

    // Clase auxiliar para TextWatcher simple
    private class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { if (!estaCargandoDatos) mostrarBotonGuardar(); }
    }

    private static class DigitsOnlyFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence s, int start, int end, Spanned d, int ds, int de) {
            for (int i = start; i < end; i++) if (!Character.isDigit(s.charAt(i))) return "";
            return null;
        }
    }
}