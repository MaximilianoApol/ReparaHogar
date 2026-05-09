package com.example.reparahogar;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseHelper {
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // Sincronizar Usuario (Login/Perfil)
    public void guardarUsuarioNube(Usuario usuario) {
        db.collection("usuarios").document(usuario.getId())
                .set(usuario)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Usuario sincronizado"))
                .addOnFailureListener(e -> Log.w("Firebase", "Error al sincronizar", e));
    }

    // Sincronizar una Cita (Calendario/Servicios)
    public void guardarCitaNube(Cita cita) {
        db.collection("citas").document(cita.getIdCita())
                .set(cita);
    }

    // Método para obtener el ID del usuario actual
    public String getUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}
