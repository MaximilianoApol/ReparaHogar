package com.example.reparahogar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class fragment_servicio_confirmado extends Fragment {

    private String idCita;
    private FirebaseFirestore db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance(); // Inicializar Firestore
        if (getArguments() != null) {
            idCita = getArguments().getString("idCita");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflamos tu diseño de éxito
        View view = inflater.inflate(R.layout.fragment_servicio_confirmado2, container, false);

        // Referencias de los componentes del diseño que elegiste
        ImageButton btnClose = view.findViewById(R.id.btnClose);
        MaterialButton btnGuardar = view.findViewById(R.id.btnGuardarEstado);
        TextView txtResumenNombre = view.findViewById(R.id.txtSuccess); // Del include
        TextView txtResumenFecha = view.findViewById(R.id.txtNombreServicioResumen); // Del include

        // Lógica para ACTUALIZAR UI
        // Seteamos datos del bundle si es necesario
        if (getArguments() != null) {
            txtResumenNombre.setText(getArguments().getString("categoria"));
        }

        // Lógica del botón de cierre
        btnClose.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Lógica para ACTUALIZAR FIRESTORE
        btnGuardar.setOnClickListener(v -> {
            if (idCita != null) {
                db.collection("citas").document(idCita)
                        .update("estado", "completado")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Servicio Finalizado", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack(); // Regresa a la agenda
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        return view;
    }
}