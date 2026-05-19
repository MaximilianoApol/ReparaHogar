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

/**
 * Pantalla de detalle de un servicio del cliente.
 *
 * CORRECCIÓN CRÍTICA:
 *  - Todos los argumentos tienen valores por defecto no-null ("—")
 *  - El switch(estado) nunca recibe null → se usa if/else en su lugar
 *    para evitar el NullPointerException de Java al hacer hashCode()
 */
public class FragmentDetalleServicio extends Fragment {

    private String servicioId   = "";
    private String proveedorUid = "";
    private String categoria    = "—";
    private String detalle      = "—";
    private String estado       = "—";
    private String fecha        = "—";
    private String hora         = "—";
    private String direccion    = "—";

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

        // ── Leer argumentos de forma null-safe ────────────────────────────────
        Bundle args = getArguments();
        if (args != null) {
            servicioId   = nonNull(args.getString("servicioId"),   "");
            proveedorUid = nonNull(args.getString("proveedorUid"), "");
            categoria    = nonNull(args.getString("categoria"),    "—");
            detalle      = nonNull(args.getString("detalle"),      "—");
            estado       = nonNull(args.getString("estado"),       "—");
            fecha        = nonNull(args.getString("fecha"),        "—");
            hora         = nonNull(args.getString("hora"),         "—");
            direccion    = nonNull(args.getString("direccion"),    "—");
        }

        // ── Botón atrás ───────────────────────────────────────────────────────
        ImageButton btnBack = view.findViewById(R.id.btnBackDetalle);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        // ── Rellenar campos ───────────────────────────────────────────────────
        setText(view, R.id.txtDetalleCategoria, categoria);
        setText(view, R.id.txtDetalleDetalle,   detalle);
        setText(view, R.id.txtDetalleEstado,    estado);
        setText(view, R.id.txtDetalleFecha,     fecha);
        setText(view, R.id.txtDetalleHora,      hora);
        setText(view, R.id.txtDetalleDireccion, direccion);

        // ── Color del estado — if/else, nunca switch sobre String ─────────────
        // NOTA: switch(String) en Java llama .hashCode() → NPE si es null.
        //       Usamos if/else para ser explícitamente null-safe.
        TextView txtEstado = view.findViewById(R.id.txtDetalleEstado);
        if (txtEstado != null) {
            if ("PENDIENTE".equals(estado)) {
                txtEstado.setTextColor(android.graphics.Color.parseColor("#F59E0B"));
            } else if ("CONFIRMADO".equals(estado)) {
                txtEstado.setTextColor(android.graphics.Color.parseColor("#3B82F6"));
            } else if ("TERMINADO".equals(estado)) {
                txtEstado.setTextColor(android.graphics.Color.parseColor("#10B981"));
            } else {
                txtEstado.setTextColor(android.graphics.Color.parseColor("#6B7280"));
            }
        }

        // ── Cargar nombre del técnico desde Firestore ─────────────────────────
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
                    if (doc == null || !doc.exists() || !isAdded()) return;

                    String nombre = doc.getString("nombre");
                    String tipo   = doc.getString("tipoServicio");
                    float  calif  = 0f;
                    if (doc.contains("calificacionPromedio") && doc.get("calificacionPromedio") != null) {
                        calif = ((Number) doc.get("calificacionPromedio")).floatValue();
                    }

                    TextView txtTecnico = view.findViewById(R.id.txtDetalleTecnico);
                    TextView txtCalif   = view.findViewById(R.id.txtDetalleTecnicoCalif);
                    TextView txtTipo    = view.findViewById(R.id.txtDetalleTecnicoTipo);

                    if (txtTecnico != null) txtTecnico.setText(nonNull(nombre, "—"));
                    if (txtCalif   != null) txtCalif.setText(
                            String.format(java.util.Locale.getDefault(), "★ %.1f", calif));
                    if (txtTipo    != null) txtTipo.setText(nonNull(tipo, "—"));
                });

        // Al tocar la card del técnico → abrir perfil público
        View cardTecnico = view.findViewById(R.id.cardTecnico);
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setText(View root, int id, String value) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(nonNull(value, "—"));
    }

    /** Devuelve {@code value} si no es null, o {@code fallback} si es null. */
    private static String nonNull(@Nullable String value, String fallback) {
        return value != null ? value : fallback;
    }
}