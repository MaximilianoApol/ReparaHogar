package com.example.reparahogar.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.R;
import com.example.reparahogar.model.Servicio;
import androidx.annotation.Nullable;


import java.util.ArrayList;
import java.util.List;


public class MantenimientoAdapter extends RecyclerView.Adapter<MantenimientoAdapter.ViewHolder> {

    public interface OnCalificarListener {
        void onCalificar(Servicio servicio);
    }

    private final List<Servicio> lista;
    private final OnCalificarListener onCalificarListener;

    public MantenimientoAdapter(List<Servicio> lista,
                            @Nullable OnCalificarListener onCalificarListener) {
        this.lista = lista;
        this.onCalificarListener = onCalificarListener;
    }

    public void actualizarLista(List<Servicio> nuevaLista, List<String> idsCalificados) {
        lista.clear();

        // Separar en dos grupos
        List<Servicio> sinCalificar = new ArrayList<>();
        List<Servicio> resto = new ArrayList<>();

        for (Servicio s : nuevaLista) {
            if (Servicio.ESTADO_TERMINADO.equals(s.getEstado())
                    && !idsCalificados.contains(s.getId())) {
                sinCalificar.add(s);
            } else {
                resto.add(s);
            }
        }

        lista.addAll(sinCalificar);
        lista.addAll(resto);
        notifyDataSetChanged();
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mantenimiento, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Servicio s = lista.get(position);

        // Estado + fecha
        String estado = s.getEstado() != null ? s.getEstado() : "—";
        String fecha  = s.getFecha()  != null ? s.getFecha()  : "";
        holder.lblStatusDate.setText(estado + (fecha.isEmpty() ? "" : " · " + fecha));

        // Color del estado
        switch (estado) {
            case Servicio.ESTADO_PENDIENTE:
                holder.lblStatusDate.setTextColor(Color.parseColor("#FF6D00")); break;
            case Servicio.ESTADO_CONFIRMADO:
                holder.lblStatusDate.setTextColor(Color.parseColor("#1976D2")); break;
            case Servicio.ESTADO_TERMINADO:
                holder.lblStatusDate.setTextColor(Color.parseColor("#2E7D32")); break;
            default:
                holder.lblStatusDate.setTextColor(Color.GRAY);
        }

        // Categoría
        holder.txtCategoria.setText(s.getCategoria() != null ? s.getCategoria() : "—");

        // Detalle
        holder.txtDescripcion.setText(s.getDetalle() != null ? s.getDetalle() : "—");

        // Tap en TERMINADO → calificar
        holder.itemView.setOnClickListener(v -> {
            if (Servicio.ESTADO_TERMINADO.equals(s.getEstado()) && onCalificarListener != null) {
                onCalificarListener.onCalificar(s);
            }
        });
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView lblStatusDate;
        TextView txtCategoria;
        TextView txtDescripcion;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            lblStatusDate  = itemView.findViewById(R.id.lblStatusDate);
            txtCategoria   = itemView.findViewById(R.id.txtCategoria);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcion);
        }
    }
}