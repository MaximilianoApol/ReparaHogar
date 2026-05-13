package com.example.reparahogar.proveedor;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.R;
import com.example.reparahogar.model.Servicio;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

/**
 * Adapter para la agenda del proveedor.
 * Usa el modelo Servicio (colección "servicios" en Firestore).
 * Al tocar un ítem vigente muestra un BottomSheet con opciones:
 *  - Confirmar llegada → CONFIRMADO
 *  - Finalizar servicio → abre fragment_servicio_confirmado2
 */
public class ProveedorAdapter extends RecyclerView.Adapter<ProveedorAdapter.ViewHolder> {

    public interface OnServicioClickListener {
        void onConfirmarClick(Servicio servicio);
        void onFinalizarClick(Servicio servicio);
    }

    // Mantenemos también la interfaz antigua para compatibilidad con DetalleProveedor
    public interface OnCitaClickListener {
        void onConfirmarClick(Cita cita);
        void onFinalizarClick(Cita cita);
    }

    private final List<Servicio> lista;
    private final OnServicioClickListener listener;

    public ProveedorAdapter(List<Servicio> lista, OnServicioClickListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    /** Constructor de compatibilidad para DetalleProveedor que usa Cita */
    public ProveedorAdapter(List<Cita> listaCitas, OnCitaClickListener citaListener) {
        // Convertimos a lista vacía de Servicio — DetalleProveedor usará su propio adapter
        this.lista    = new java.util.ArrayList<>();
        this.listener = null;
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

        String estado = s.getEstado() != null ? s.getEstado() : "—";
        String fecha  = s.getFecha()  != null ? s.getFecha()  : "";
        holder.lblStatusDate.setText(estado + (fecha.isEmpty() ? "" : " · " + fecha));

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

        holder.txtCategoria.setText(s.getCategoria() != null ? s.getCategoria() : "—");
        holder.txtDescripcion.setText(s.getDetalle() != null ? s.getDetalle() : "—");

        // Solo servicios no terminados son accionables
        boolean vigente = !Servicio.ESTADO_TERMINADO.equals(s.getEstado());
        holder.itemView.setOnClickListener(v -> {
            if (!vigente || listener == null) return;
            mostrarBottomSheet(v, s);
        });
    }

    private void mostrarBottomSheet(View anchor, Servicio servicio) {
        BottomSheetDialog dialog = new BottomSheetDialog(anchor.getContext());
        View sheet = LayoutInflater.from(anchor.getContext())
                .inflate(R.layout.layout_cambio_estado, null);

        sheet.findViewById(R.id.optionConfirmar).setOnClickListener(v -> {
            if (listener != null) listener.onConfirmarClick(servicio);
            dialog.dismiss();
        });
        sheet.findViewById(R.id.optionFinalizar).setOnClickListener(v -> {
            if (listener != null) listener.onFinalizarClick(servicio);
            dialog.dismiss();
        });

        dialog.setContentView(sheet);
        dialog.show();
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