package com.example.reparahogar.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * Factory para crear cualquier ViewModel de la app que extienda AndroidViewModel.
 *
 * Uso en un Fragment:
 *
 *   AuthViewModel vm = new ViewModelProvider(this,
 *       new ViewModelFactory(requireActivity().getApplication()))
 *       .get(AuthViewModel.class);
 *
 * O más corto con el helper de abajo:
 *
 *   AuthViewModel vm = ViewModelFactory.of(this, AuthViewModel.class);
 */
public class ViewModelFactory extends ViewModelProvider.AndroidViewModelFactory {

    private final Application application;

    public ViewModelFactory(@NonNull Application application) {
        super(application);
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        // Delega a AndroidViewModelFactory que sabe construir AndroidViewModels
        return super.create(modelClass);
    }

    // ── Helper estático ───────────────────────────────────────────────────────

    /**
     * Crea o recupera un ViewModel ya existente para el Fragment o Activity dado.
     *
     * Ejemplo de uso en Fragment:
     *   AuthViewModel vm = ViewModelFactory.obtener(this, AuthViewModel.class);
     */
    public static <T extends ViewModel> T obtener(
            @NonNull androidx.fragment.app.Fragment fragment,
            @NonNull Class<T> claseViewModel) {

        Application app = fragment.requireActivity().getApplication();
        return new ViewModelProvider(fragment, new ViewModelFactory(app))
                .get(claseViewModel);
    }

    /**
     * Versión para Activity.
     */
    public static <T extends ViewModel> T obtener(
            @NonNull androidx.appcompat.app.AppCompatActivity activity,
            @NonNull Class<T> claseViewModel) {

        return new ViewModelProvider(activity, new ViewModelFactory(activity.getApplication()))
                .get(claseViewModel);
    }
}