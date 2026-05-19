package com.example.reparahogar.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.R;
import com.example.reparahogar.model.Servicio;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter para mostrar servicios del cliente.
 *
 * CORRECCIONES aplicadas:
 *  1. El tap en un ítem SIEMPRE llama al listener (para abrir detalle).
 *     ES RESPONSABILIDAD DEL LISTENER decidir qué hacer según el estado.
 *     - En DetalleHogar → abre FragmentDetalleServicio (siempre)
 *     - En FragmentNotificaciones → solo abre calificación si es TERMINADO
 *
 *  2. El indicador lateral de color (View#indicadorEstado) refleja el estado:
 *     naranja = PENDIENTE, azul = CONFIRMADO, verde = TERMINADO.
 *
 *  3. actualizarLista() ordena: primero TERMINADOS sin calificar,
 *     luego el resto — para que los que necesitan acción aparezcan arriba.
 */
public class MantenimientoAdapter
        extends RecyclerView.Adapter<MantenimientoAdapter.ViewHolder> {

    public interface OnCalificarListener {
        void onCalificar(Servicio servicio);
    }

    private final List<Servicio>      lista;
    private final OnCalificarListener onCalificarListener;

    public MantenimientoAdapter(List<Servicio> lista,
                                @Nullable OnCalificarListener onCalificarListener) {
        this.lista               = lista;
        this.onCalificarListener = onCalificarListener;
    }

    /**
     * Actualiza la lista ordenando los TERMINADOS sin calificar primero.
     */
    public void actualizarLista(List<Servicio> nuevaLista, List<String> idsCalificados) {
        lista.clear();

        List<Servicio> sinCalificar = new ArrayList<>();
        List<Servicio> resto        = new ArrayList<>();

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

        String estado = s.getEstado() != null ? s.getEstado() : "—";
        String fecha  = s.getFecha()  != null ? s.getFecha()  : "";
        String hora   = s.getHora()   != null ? " · " + s.getHora() : "";

        holder.lblStatusDate.setText(
                estado + (fecha.isEmpty() ? "" : " · " + fecha + hora));

        // Color del texto de estado
        int colorEstado;
        if ("PENDIENTE".equals(estado)) {
            colorEstado = Color.parseColor("#F59E0B");
        } else if ("CONFIRMADO".equals(estado)) {
            colorEstado = Color.parseColor("#3B82F6");
        } else if ("TERMINADO".equals(estado)) {
            colorEstado = Color.parseColor("#10B981");
        } else {
            colorEstado = Color.GRAY;
        }
        holder.lblStatusDate.setTextColor(colorEstado);

        // Color del indicador lateral
        if (holder.indicadorEstado != null) {
            holder.indicadorEstado.setBackgroundColor(colorEstado);
        }

        holder.txtCategoria.setText(s.getCategoria()  != null ? s.getCategoria()  : "—");
        holder.txtDescripcion.setText(s.getDetalle()  != null ? s.getDetalle()    : "—");

        // Tap → delegar al listener; el listener decide qué hacer según el estado
        holder.itemView.setOnClickListener(v -> {
            if (onCalificarListener != null) {
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
        View     indicadorEstado;   // barra lateral de color (puede ser null si no existe en el layout)

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            lblStatusDate  = itemView.findViewById(R.id.lblStatusDate);
            txtCategoria   = itemView.findViewById(R.id.txtCategoria);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcion);
            indicadorEstado = itemView.findViewById(R.id.indicadorEstado); // null-safe
        }
    }
}