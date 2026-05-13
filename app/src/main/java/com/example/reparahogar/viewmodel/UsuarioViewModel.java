package com.example.reparahogar.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.reparahogar.model.Usuario;
import com.example.reparahogar.repository.UsuarioRepository;


public class UsuarioViewModel extends AndroidViewModel {

    private final UsuarioRepository usuarioRepository;

    public UsuarioViewModel(@NonNull Application application) {
        super(application);
        usuarioRepository = new UsuarioRepository(application);
    }

    public LiveData<Usuario> getUsuario(String uid) {
        return usuarioRepository.obtenerUsuario(uid);
    }
}