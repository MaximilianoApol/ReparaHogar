package com.example.reparahogar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.reparahogar.model.Servicio;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class FragmentServicioConfirmado extends Fragment {

    private String idCita;
    private String clienteUid;
    private FirebaseFirestore db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            idCita     = getArguments().getString("idCita");
            clienteUid = getArguments().getString("clienteUid");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_servicio_confirmado, container, false);

        ImageButton    btnClose   = view.findViewById(R.id.btnClose);
        MaterialButton btnGuardar = view.findViewById(R.id.btnGuardarEstado);
        TextView txtCategoria     = view.findViewById(R.id.txtSuccess);
        TextView txtDireccion     = view.findViewById(R.id.txtResumenDireccion);
        TextView txtHora          = view.findViewById(R.id.txtResumenHora);
        TextView txtCliente       = view.findViewById(R.id.txtResumenCliente);

        // Bloquear clicks
        view.setClickable(true);
        view.setOnClickListener(v -> {});
        View cardInfo = view.findViewById(R.id.cardServiceInfo);
        if (cardInfo != null) {
            cardInfo.setClickable(true);
            cardInfo.setOnClickListener(v -> {});
        }

        // Llenar datos básicos desde el bundle
        if (getArguments() != null) {
            txtCategoria.setText(getArguments().getString("categoria", "—"));
            txtDireccion.setText(getArguments().getString("direccion", "—"));
            txtHora.setText(getArguments().getString("hora", "—"));
        }

        // Buscar nombre del cliente en Firestore
        if (clienteUid != null && !clienteUid.isEmpty()) {
            db.collection("users").document(clienteUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && txtCliente != null) {
                            String nombre = doc.getString("nombre");
                            txtCliente.setText(nombre != null ? nombre : "—");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (txtCliente != null) txtCliente.setText("—");
                    });
        }

        btnClose.setOnClickListener(v ->
                getParentFragmentManager().popBackStack(
                        null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE));

        btnGuardar.setOnClickListener(v -> {
            if (idCita != null) {
                db.collection("servicios").document(idCita)
                        .update("estado", Servicio.ESTADO_TERMINADO)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Servicio Finalizado",
                                    Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack(
                                    null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Error al actualizar",
                                        Toast.LENGTH_SHORT).show());
            }
        });

        return view;
    }
}