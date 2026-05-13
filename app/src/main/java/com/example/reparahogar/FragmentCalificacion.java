package com.example.reparahogar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.reparahogar.viewmodel.CalificacionViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;


public class FragmentCalificacion extends Fragment {

    private CalificacionViewModel calificacionViewModel;
    private String servicioId;
    private String proveedorUid;
    private int puntuacionActual = 5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calificacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            servicioId   = getArguments().getString("servicioId", "");
            proveedorUid = getArguments().getString("proveedorUid", "");
        }

        calificacionViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(CalificacionViewModel.class);

        // Vistas
        ImageButton   btnBack          = view.findViewById(R.id.btnBackCalifi);
        RatingBar     ratingBar        = view.findViewById(R.id.ratingBar);
        TextView      txtRatingLabel   = view.findViewById(R.id.txtRatingLabel);
        MaterialButton btnGuardar      = view.findViewById(R.id.btnGuardarCalif);

        // RatingBar — usamos numStars=5 para calificación completa
        ratingBar.setNumStars(5);
        ratingBar.setRating(5f);
        ratingBar.setStepSize(1f);

        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            puntuacionActual = (int) Math.max(1, rating);
            txtRatingLabel.setText(puntuacionActual + " - " + etiqueta(puntuacionActual));
        });

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnGuardar.setOnClickListener(v -> {
            String clienteUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
            calificacionViewModel.guardarCalificacion(
                    servicioId, proveedorUid, clienteUid, puntuacionActual);
        });

        // Observadores
        calificacionViewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            btnGuardar.setEnabled(!cargando);
            btnGuardar.setText(cargando ? "Guardando..." : "Guardar calificación");
        });

        calificacionViewModel.getCalificacionGuardada().observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Toast.makeText(getContext(), "¡Calificación guardada!", Toast.LENGTH_SHORT).show();
                calificacionViewModel.limpiarCalificacion();
                getParentFragmentManager().popBackStack();
            }
        });

        calificacionViewModel.getErrorMensaje().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                calificacionViewModel.limpiarError();
            }
        });
    }

    private String etiqueta(int puntos) {
        switch (puntos) {
            case 1: return "Muy malo";
            case 2: return "Malo";
            case 3: return "Regular";
            case 4: return "Bueno";
            case 5: return "Excelente";
            default: return "";
        }
    }
}