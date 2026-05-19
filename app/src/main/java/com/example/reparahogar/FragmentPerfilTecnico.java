package com.example.reparahogar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

public class FragmentPerfilTecnico extends Fragment {

    private String proveedorUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_tecnico, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            proveedorUid = getArguments().getString("proveedorUid", "");
        }

        ImageButton btnBack = view.findViewById(R.id.btnBackTecnico);
        if (btnBack != null) btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (!proveedorUid.isEmpty()) {
            cargarDatosTecnico(view);
        }
    }

    private void cargarDatosTecnico(View view) {
        FirebaseFirestore.getInstance()
                .collection("proveedores")
                .document(proveedorUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || !isAdded()) return;

                    String nombre      = doc.getString("nombre");
                    String telefono    = doc.getString("telefono");
                    String descripcion = doc.getString("descripcion");
                    String tipo        = doc.getString("tipoServicio");
                    String horaInicio  = doc.getString("horaInicio");
                    String horaFin     = doc.getString("horaFin");
                    float  calif       = doc.contains("calificacionPromedio")
                            ? ((Number) doc.get("calificacionPromedio")).floatValue() : 0f;
                    long   totalServ   = doc.contains("totalServicios")
                            ? ((Number) doc.get("totalServicios")).longValue() : 0L;

                    setText(view, R.id.txtTecnicoNombre, nombre);
                    setText(view, R.id.txtTecnicoTelefono, telefono);
                    setText(view, R.id.txtTecnicoDescripcion,
                            (descripcion != null && !descripcion.isEmpty()) ? descripcion : "Sin descripción");
                    setText(view, R.id.txtTecnicoTipo, tipo);
                    setText(view, R.id.txtTecnicoCalificacion,
                            String.format(java.util.Locale.getDefault(), "★ %.1f  (%d servicios)", calif, totalServ));

                    // Horario estático
                    String horario = "—";
                    if (horaInicio != null && horaFin != null
                            && !horaInicio.isEmpty() && !horaFin.isEmpty()) {
                        horario = "De " + horaInicio + " a " + horaFin;
                    }
                    setText(view, R.id.txtTecnicoHorario, horario);
                });
    }

    private void setText(View root, int id, String value) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(value != null ? value : "—");
    }
}