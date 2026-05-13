package com.example.reparahogar.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.R;
import com.example.reparahogar.model.Proveedor;

import java.util.List;

/**
 * Adapter para mostrar la lista de técnicos disponibles en FragmentTecnicosDisponibles.
 * Al tocar un ítem se notifica mediante OnTecnicoClickListener.
 */
public class TecnicoAdapter extends RecyclerView.Adapter<TecnicoAdapter.ViewHolder> {

    public interface OnTecnicoClickListener {
        void onTecnicoSeleccionado(Proveedor proveedor);
    }

    private final List<Proveedor> lista;
    private final OnTecnicoClickListener listener;
    private long posicionSeleccionada = RecyclerView.NO_ID;

    public TecnicoAdapter(List<Proveedor> lista, OnTecnicoClickListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tecnico, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Proveedor p = lista.get(position);

        holder.txtNombre.setText(p.getNombre() != null ? p.getNombre() : "—");
        holder.txtServicios.setText("Servicios realizados: " + p.getTotalServicios());

        // Calificación
        String calif = String.format(java.util.Locale.getDefault(),
                "Calificación: %.1f", p.getCalificacionPromedio());
        holder.txtCalificacion.setText(calif);

        // Resaltar seleccionado
        boolean seleccionado = (holder.getAdapterPosition() == posicionSeleccionada);
        holder.itemView.setBackgroundColor(seleccionado
                ? 0xFFE3EBF8   // azul claro
                : 0xFFFFFFFF);

        holder.itemView.setOnClickListener(v -> {
            int anterior = (int) posicionSeleccionada;
            posicionSeleccionada = holder.getAdapterPosition();
            notifyItemChanged(anterior);
            notifyItemChanged((int) posicionSeleccionada);
            if (listener != null) listener.onTecnicoSeleccionado(p);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre;
        TextView txtServicios;
        TextView txtCalificacion;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre       = itemView.findViewById(R.id.txtNombreTecnico);
            txtServicios    = itemView.findViewById(R.id.txtServicios);
            // ratingBar en el layout es un LinearLayout que contiene un TextView
            ViewGroup ratingBar = itemView.findViewById(R.id.ratingBar);
            txtCalificacion = (TextView) ratingBar.getChildAt(0); // primer TextView
        }
    }
}