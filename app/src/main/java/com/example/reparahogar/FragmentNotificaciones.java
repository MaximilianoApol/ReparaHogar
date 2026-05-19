package com.example.reparahogar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reparahogar.adapter.MantenimientoAdapter;
import com.example.reparahogar.model.Servicio;
import com.example.reparahogar.viewmodel.ServicioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * Muestra todos los servicios del cliente con su estado.
 *
 * CORRECCIÓN:
 *  El listener solo abre la calificación cuando el estado es TERMINADO.
 *  Para cualquier otro estado (PENDIENTE, CONFIRMADO) no hace nada,
 *  ya que el servicio aún no ha sido finalizado por el proveedor.
 */
public class FragmentNotificaciones extends Fragment {

    private ServicioViewModel    servicioViewModel;
    private MantenimientoAdapter adapter;
    private final List<Servicio> listaServicios = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notificaciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        servicioViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(ServicioViewModel.class);

        RecyclerView rvNotificaciones = view.findViewById(R.id.rvNotificaciones);
        rvNotificaciones.setLayoutManager(new LinearLayoutManager(getContext()));

        // CORRECCIÓN: el listener verifica el estado antes de abrir calificación
        adapter = new MantenimientoAdapter(listaServicios, this::manejarTapServicio);
        rvNotificaciones.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (!uid.isEmpty()) {
            servicioViewModel.getServiciosCliente(uid).observe(getViewLifecycleOwner(), servicios -> {
                listaServicios.clear();
                if (servicios != null) listaServicios.addAll(servicios);
                adapter.notifyDataSetChanged();
            });
        }
    }

    /**
     * Solo abre la pantalla de calificación si el servicio está TERMINADO.
     * Si está PENDIENTE o CONFIRMADO, no hace nada — el proveedor aún
     * no ha finalizado el trabajo.
     */
    private void manejarTapServicio(Servicio servicio) {
        if (!Servicio.ESTADO_TERMINADO.equals(servicio.getEstado())) {
            // Servicio aún no finalizado → ignorar tap
            return;
        }
        abrirCalificacion(servicio);
    }

    private void abrirCalificacion(Servicio servicio) {
        FragmentCalificacion fragment = new FragmentCalificacion();
        Bundle args = new Bundle();
        args.putString("servicioId",   servicio.getId());
        args.putString("proveedorUid", servicio.getProveedorUid());
        fragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.mi_hogar, fragment)
                .addToBackStack(null)
                .commit();
    }
}