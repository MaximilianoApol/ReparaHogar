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
import androidx.lifecycle.ViewModelProvider;

import com.example.reparahogar.model.Usuario;
import com.example.reparahogar.viewmodel.UsuarioViewModel;
import com.example.reparahogar.viewmodel.ViewModelFactory;
import com.google.firebase.auth.FirebaseAuth;


public class FragmentPerfil extends Fragment {

    private UsuarioViewModel usuarioViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        usuarioViewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication())
        ).get(UsuarioViewModel.class);

        ImageButton btnBack    = view.findViewById(R.id.btnBack);
        TextView    txtNombre  = view.findViewById(R.id.txtNombre);
        TextView    txtTel     = view.findViewById(R.id.txtTelefono);
        TextView    txtCorreo  = view.findViewById(R.id.txtCorreo);
        TextView    txtLabel   = view.findViewById(R.id.txtUserLabel);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (!uid.isEmpty()) {
            usuarioViewModel.getUsuario(uid).observe(getViewLifecycleOwner(), usuario -> {
                if (usuario == null) return;
                if (txtNombre != null)  txtNombre.setText(usuario.getNombre()   != null ? usuario.getNombre()   : "—");
                if (txtTel    != null)  txtTel.setText   (usuario.getTelefono() != null ? usuario.getTelefono() : "—");
                if (txtCorreo != null)  txtCorreo.setText(usuario.getCorreo()   != null ? usuario.getCorreo()   : "—");
                if (txtLabel  != null)  txtLabel.setText (usuario.getNombre()   != null ? usuario.getNombre()   : "Perfil");
            });
        }
    }
}