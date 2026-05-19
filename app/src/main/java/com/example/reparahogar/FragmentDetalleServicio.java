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

public class FragmentDetalleServicio extends Fragment {

    private String servicioId;
    private String clienteUid;
    private String proveedorUid;
    private String categoria;
    private String detalle;
    private String estado;
    private String fecha;
    private String hora;
    private String direccion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalle_servicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            servicioId   = getArguments().getString("servicioId", "");
            proveedorUid = getArguments().getString("proveedorUid", "");
            categoria    = getArguments().getString("categoria", "—");
            detalle      = getArguments().getString("detalle", "—");
            estado       = getArguments().getString("estado", "—");
            fecha        = getArguments().getString("fecha", "—");
            hora         = getArguments().getString("hora", "—");
            direccion    = getArguments().getString("direccion", "—");
        }

        ImageButton btnBack = view.findViewById(R.id.btnBackDetalle);
        if (btnBack != null) btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Llenar campos del servicio
        setText(view, R.id.txtDetalleCategoria, categoria);
        setText(view, R.id.txtDetalleDetalle, detalle);
        setText(view, R.id.txtDetalleEstado, estado);
        setText(view, R.id.txtDetalleFecha, fecha);
        setText(view, R.id.txtDetalleHora, hora);
        setText(view, R.id.txtDetalleDireccion, direccion);

        // Color del estado
        TextView txtEstado = view.findViewById(R.id.txtDetalleEstado);
        if (txtEstado != null) {
            switch (estado) {
                case "PENDIENTE":
                    txtEstado.setTextColor(android.graphics.Color.parseColor("#F59E0B")); break;
                case "CONFIRMADO":
                    txtEstado.setTextColor(android.graphics.Color.parseColor("#3B82F6")); break;
                case "TERMINADO":
                    txtEstado.setTextColor(android.graphics.Color.parseColor("#10B981")); break;
            }
        }

        // Cargar nombre del técnico desde Firestore
        TextView txtTecnico = view.findViewById(R.id.txtDetalleTecnico);
        View cardTecnico = view.findViewById(R.id.cardTecnico);

        if (!proveedorUid.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("proveedores")
                    .document(proveedorUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && txtTecnico != null) {
                            String nombre = doc.getString("nombre");
                            float calif = doc.contains("calificacionPromedio")
                                    ? ((Number) doc.get("calificacionPromedio")).floatValue() : 0f;
                            String tipo = doc.getString("tipoServicio");

                            txtTecnico.setText(nombre != null ? nombre : "—");

                            TextView txtCalif = view.findViewById(R.id.txtDetalleTecnicoCalif);
                            TextView txtTipo  = view.findViewById(R.id.txtDetalleTecnicoTipo);

                            if (txtCalif != null)
                                txtCalif.setText(String.format(java.util.Locale.getDefault(), "★ %.1f", calif));
                            if (txtTipo != null)
                                txtTipo.setText(tipo != null ? tipo : "—");
                        }
                    });

            // Click en card del técnico → abrir perfil público
            if (cardTecnico != null) {
                cardTecnico.setOnClickListener(v -> {
                    FragmentPerfilTecnico frag = new FragmentPerfilTecnico();
                    Bundle args = new Bundle();
                    args.putString("proveedorUid", proveedorUid);
                    frag.setArguments(args);
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.mi_hogar, frag)
                            .addToBackStack(null)
                            .commit();
                });
            }
        }
    }

    private void setText(View root, int id, String value) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(value != null ? value : "—");
    }
}