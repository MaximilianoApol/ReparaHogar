package com.example.reparahogar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;

public class FragmentSeleccionEspecificacion extends Fragment {

    private String categoriaPadre;
    private String especificacionSeleccionada = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_seleccion_especificacion, container, false);

        // Referencias
        TextView txtTitulo = view.findViewById(R.id.txtTituloToolbar);
        TextView txtSeccion = view.findViewById(R.id.txtSeccionTitulo);
        ChipGroup group = view.findViewById(R.id.chipGroupDinamico);
        MaterialButton btnSiguiente = view.findViewById(R.id.btnSiguiente);
        ImageButton btnBack = view.findViewById(R.id.btnBack);

        // Recuperar la categoría enviada
        if (getArguments() != null) {
            categoriaPadre = getArguments().getString("categoria");
            txtTitulo.setText(categoriaPadre);
            txtSeccion.setText("¿Qué tipo de " + categoriaPadre + " necesitas?");
            cargarChips(categoriaPadre, group);
        }

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnSiguiente.setOnClickListener(v -> {
            if (!especificacionSeleccionada.isEmpty()) {
                // AQUÍ PASAREMOS A LA PANTALLA DE FECHA/HORA
                Toast.makeText(getContext(), "Seleccionaste: " + especificacionSeleccionada, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Por favor selecciona una opción", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void cargarChips(String categoria, ChipGroup group) {
        String[] opciones;

        // Lógica para decidir qué mostrar
        switch (categoria) {
            case "Agua / Plomería":
                opciones = new String[]{"Fuga de agua", "Drenaje tapado", "Instalación de grifo", "Tinacos", "Calentadores"};
                break;
            case "Electricidad":
                opciones = new String[]{"Cortocircuito", "Instalación de sockets", "Tablero eléctrico", "Cableado", "Interfón"};
                break;
            case "Gas":
                opciones = new String[]{"Fuga de gas", "Instalación de estufa", "Línea de llenado", "Reguladores"};
                break;
            default:
                opciones = new String[]{"Mantenimiento general", "Revisión técnica"};
                break;
        }

        for (String opcion : opciones) {
            Chip chip = new Chip(getContext());
            chip.setText(opcion);
            chip.setCheckable(true);
            chip.setClickable(true);


            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) especificacionSeleccionada = opcion;
            });

            group.addView(chip);
        }
    }
}