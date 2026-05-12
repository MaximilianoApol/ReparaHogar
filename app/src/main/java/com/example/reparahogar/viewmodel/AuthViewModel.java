package com.example.reparahogar.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.reparahogar.model.Proveedor;
import com.example.reparahogar.model.Usuario;
import com.example.reparahogar.repository.ProveedorRepository;
import com.example.reparahogar.repository.UsuarioRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Maneja registro, login, sesión activa y cierre de sesión.
 *
 * Navegación según tipo de usuario:
 *   CLIENTE   → DetalleHogar
 *   PROVEEDOR verificado   → DetalleProveedor
 *   PROVEEDOR no verificado → VerificacionFragment
 */
public class AuthViewModel extends AndroidViewModel {

    private final FirebaseAuth        auth;
    private final UsuarioRepository   usuarioRepository;
    private final ProveedorRepository proveedorRepository;

    // ── Estados que observa la UI ─────────────────────────────────────────────
    private final MutableLiveData<Boolean> cargando             = new MutableLiveData<>(false);
    private final MutableLiveData<String>  errorMensaje         = new MutableLiveData<>();
    private final MutableLiveData<Usuario> usuarioActual        = new MutableLiveData<>();
    private final MutableLiveData<Boolean> navegarCliente       = new MutableLiveData<>();
    private final MutableLiveData<Boolean> navegarProveedor     = new MutableLiveData<>();
    private final MutableLiveData<Boolean> navegarVerificacion  = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        auth                = FirebaseAuth.getInstance();
        usuarioRepository   = new UsuarioRepository(application);
        proveedorRepository = new ProveedorRepository(application);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public void login(String correo, String password) {
        if (!validarLogin(correo, password)) return;

        cargando.setValue(true);

        auth.signInWithEmailAndPassword(correo.trim(), password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        cargando.setValue(false);
                        errorMensaje.setValue("Error al iniciar sesión");
                        return;
                    }
                    consultarTipoYNavegar(firebaseUser.getUid());
                })
                .addOnFailureListener(e -> {
                    cargando.setValue(false);
                    errorMensaje.setValue(traducirError(e.getMessage()));
                });
    }

    /**
     * Consulta Room (que se alimenta de Firestore) para obtener el tipo
     * de usuario y emite el LiveData de navegación correcto.
     */
    private void consultarTipoYNavegar(String uid) {
        usuarioRepository.obtenerUsuario(uid).observeForever(usuario -> {
            cargando.setValue(false);
            if (usuario == null) {
                navegarCliente.setValue(true);
                return;
            }
            usuarioActual.setValue(usuario);

            if ("PROVEEDOR".equals(usuario.getTipoUsuario())) {
                if (usuario.isVerificado()) {
                    navegarProveedor.setValue(true);
                } else {
                    navegarVerificacion.setValue(true);
                }
            } else {
                navegarCliente.setValue(true);
            }
        });
    }

    // ── Registro ──────────────────────────────────────────────────────────────

    /**
     * Crea cuenta en Firebase Auth, guarda Usuario en Firestore + Room.
     * Si es PROVEEDOR, crea también el documento base en "proveedores".
     *
     * @param esProveedor true = marcó "Soy proveedor de servicio" (chkProvider)
     *                    false = marcó "Soy dueño de casa" (chkOwner)
     */
    public void registrar(String nombre, String correo, String telefono,
                          String password, boolean esProveedor) {

        if (!validarRegistro(nombre, correo, telefono, password)) return;

        cargando.setValue(true);

        String tipoUsuario = esProveedor ? "PROVEEDOR" : "CLIENTE";

        auth.createUserWithEmailAndPassword(correo.trim(), password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        cargando.setValue(false);
                        errorMensaje.setValue("Error al crear la cuenta");
                        return;
                    }

                    Usuario nuevoUsuario = new Usuario(
                            firebaseUser.getUid(),
                            nombre.trim(),
                            correo.trim(),
                            telefono.trim(),
                            tipoUsuario
                    );

                    usuarioRepository.guardar(nuevoUsuario,
                            new UsuarioRepository.OnResultListener() {
                                @Override
                                public void onSuccess() {
                                    usuarioActual.setValue(nuevoUsuario);
                                    if (esProveedor) {
                                        crearPerfilProveedorBase(nuevoUsuario);
                                    } else {
                                        cargando.setValue(false);
                                        navegarCliente.setValue(true);
                                    }
                                }

                                @Override
                                public void onError(String mensaje) {
                                    cargando.setValue(false);
                                    errorMensaje.setValue(mensaje);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    cargando.setValue(false);
                    errorMensaje.setValue(traducirError(e.getMessage()));
                });
    }

    /**
     * Crea el documento base del proveedor en Firestore.
     * tipoServicio y descripcion los completa en VerificacionFragment.
     */
    private void crearPerfilProveedorBase(Usuario usuario) {
        Proveedor perfilBase = new Proveedor(
                usuario.getUid(),
                usuario.getNombre(),
                "",
                usuario.getTelefono(),
                usuario.getCorreo(),
                ""
        );
        perfilBase.setVerificado(false);
        perfilBase.setCalificacionPromedio(0f);

        proveedorRepository.guardar(perfilBase, new ProveedorRepository.OnResultListener() {
            @Override
            public void onSuccess() {
                cargando.setValue(false);
                navegarVerificacion.setValue(true);
            }

            @Override
            public void onError(String mensaje) {
                // Usuario guardado aunque falle el perfil — no bloqueamos
                cargando.setValue(false);
                navegarVerificacion.setValue(true);
            }
        });
    }

    // ── Sesión activa ─────────────────────────────────────────────────────────

    /**
     * Llamar desde MainActivity.onCreate().
     * Si hay sesión activa navega directo, si no, la UI carga LoginFragment.
     */
    public void verificarSesionActiva() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;
        consultarTipoYNavegar(firebaseUser.getUid());
    }

    public boolean haySesionActiva() {
        return auth.getCurrentUser() != null;
    }

    public FirebaseUser getFirebaseUserActual() {
        return auth.getCurrentUser();
    }

    public void cerrarSesion() {
        auth.signOut();
        usuarioActual.setValue(null);
    }

    // ── Validaciones ──────────────────────────────────────────────────────────

    private boolean validarLogin(String correo, String password) {
        if (correo == null || correo.trim().isEmpty()) {
            errorMensaje.setValue("Ingresa tu correo electrónico");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()) {
            errorMensaje.setValue("Correo electrónico no válido");
            return false;
        }
        if (password == null || password.isEmpty()) {
            errorMensaje.setValue("Ingresa tu contraseña");
            return false;
        }
        return true;
    }

    private boolean validarRegistro(String nombre, String correo,
                                    String telefono, String password) {
        if (nombre == null || nombre.trim().isEmpty()) {
            errorMensaje.setValue("Ingresa tu nombre");
            return false;
        }
        if (correo == null || !android.util.Patterns.EMAIL_ADDRESS
                .matcher(correo.trim()).matches()) {
            errorMensaje.setValue("Correo electrónico no válido");
            return false;
        }
        if (telefono == null || telefono.trim().length() < 10) {
            errorMensaje.setValue("Ingresa un teléfono válido (10 dígitos)");
            return false;
        }
        if (password == null || password.length() < 6) {
            errorMensaje.setValue("La contraseña debe tener al menos 6 caracteres");
            return false;
        }
        return true;
    }

    private String traducirError(String msg) {
        if (msg == null) return "Error desconocido";
        if (msg.contains("email address is already in use"))
            return "Este correo ya está registrado";
        if (msg.contains("password is invalid") || msg.contains("no user record"))
            return "Correo o contraseña incorrectos";
        if (msg.contains("network error"))
            return "Sin conexión a internet";
        if (msg.contains("badly formatted"))
            return "Formato de correo inválido";
        return "Error: " + msg;
    }

    // ── Getters LiveData ──────────────────────────────────────────────────────

    public LiveData<Boolean> getCargando()            { return cargando; }
    public LiveData<String>  getErrorMensaje()        { return errorMensaje; }
    public LiveData<Usuario> getUsuarioActual()       { return usuarioActual; }
    public LiveData<Boolean> getNavegarCliente()      { return navegarCliente; }
    public LiveData<Boolean> getNavegarProveedor()    { return navegarProveedor; }
    public LiveData<Boolean> getNavegarVerificacion() { return navegarVerificacion; }

    public void limpiarError() { errorMensaje.setValue(null); }
}