package com.graficar.colegio.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.graficar.colegio.R;
import com.graficar.colegio.databinding.FragmentHomeBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private EditText editTextNameSearch;
    private TextView textViewStatus;
    private View statusIndicator;
    private TextView textViewEntryTime;
    private TextView textViewExitTime;

    // Patrón para verificar
    private static final String TARGET_NAME = "miguelito";

    // Formato para mostrar la hora
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // El HomeViewModel se mantiene para la estructura, pero lo ignoraremos por ahora
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 1. Inicializar las vistas del nuevo layout
        editTextNameSearch = root.findViewById(R.id.editTextNameSearch);
        textViewStatus = root.findViewById(R.id.textViewStatus);
        statusIndicator = root.findViewById(R.id.statusIndicator);
        textViewEntryTime = root.findViewById(R.id.textViewEntryTime);
        textViewExitTime = root.findViewById(R.id.textViewExitTime);

        // 2. Listener para detectar cambios en el campo de texto
        editTextNameSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No se necesita
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No se necesita
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkStudentStatus(s.toString().trim());
            }
        });

        // 3. Establecer estado inicial
        resetStatus();

        return root;
    }

    /**
     * Verifica el nombre del estudiante y actualiza la UI.
     */
    private void checkStudentStatus(String name) {
        if (name.toLowerCase(Locale.getDefault()).equals(TARGET_NAME)) {
            // Caso de éxito: "miguelito" está en el colegio
            String currentTime = timeFormat.format(new Date());

            // 1. Actualizar indicador y texto
            statusIndicator.setBackgroundResource(R.drawable.status_indicator_green);
            textViewStatus.setText("Está en el colegio");

            // 2. Actualizar horas de entrada (Salida queda pendiente para lógica avanzada)
            textViewEntryTime.setText(currentTime);
            textViewExitTime.setText("Pendiente");

        } else if (name.isEmpty()) {
            // Campo vacío
            resetStatus();
        } else {
            // Nombre diferente
            statusIndicator.setBackgroundResource(R.drawable.status_indicator_gray);
            textViewStatus.setText("Estudiante NO registrado hoy");
            textViewEntryTime.setText("--");
            textViewExitTime.setText("--");
        }
    }

    /**
     * Restaura el estado a la configuración inicial.
     */
    private void resetStatus() {
        statusIndicator.setBackgroundResource(R.drawable.status_indicator_gray);
        textViewStatus.setText("Ingrese un nombre para verificar estado");
        textViewEntryTime.setText("--");
        textViewExitTime.setText("--");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}