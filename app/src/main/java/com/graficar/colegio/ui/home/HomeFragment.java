package com.graficar.colegio.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
    private TextView textViewStudentName;
    private Button btnBuscar;

    // Firebase
    private DatabaseReference databaseReference;

    // Fecha actual
    private String currentDate;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Inicializar vistas
        editTextNameSearch = root.findViewById(R.id.editTextNameSearch);
        textViewStatus = root.findViewById(R.id.textViewStatus);
        statusIndicator = root.findViewById(R.id.statusIndicator);
        textViewEntryTime = root.findViewById(R.id.textViewEntryTime);
        textViewExitTime = root.findViewById(R.id.textViewExitTime);
        textViewStudentName = root.findViewById(R.id.textViewStudentName);
        btnBuscar = root.findViewById(R.id.btnBuscar);

        // ============================================================
        // 🔥 IMPORTANTE: Usar la fecha que tienes en Firebase
        // ============================================================
        // currentDate = dateFormat.format(new Date()); // ← COMENTADO
        currentDate = "2026-08-20"; // ← FECHA DE TU FIREBASE

        // Si quieres usar la fecha actual, descomenta la línea de arriba
        // y comenta la de abajo

        // Actualizar fecha en UI
        TextView tvDate = root.findViewById(R.id.textViewDate);
        if (tvDate != null) {
            tvDate.setText("📅 " + currentDate);
        }

        // Inicializar Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("asistencias");

        // Botón de búsqueda
        btnBuscar.setOnClickListener(v -> {
            String query = editTextNameSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                buscarEstudiante(query);
            } else {
                Toast.makeText(getContext(), "Ingresa el nombre de tu hijo", Toast.LENGTH_SHORT).show();
            }
        });

        // Buscar al presionar Enter
        editTextNameSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = editTextNameSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                buscarEstudiante(query);
                return true;
            }
            return false;
        });

        resetStatus();
        return root;
    }

    /**
     * Convierte un nombre ingresado por el usuario al formato de Firebase.
     */
    private String convertirClaveFirebase(String nombre) {
        String limpio = nombre.trim();
        limpio = limpio.replace("_", " ");
        limpio = limpio.replaceAll("\\s+", " ");
        limpio = limpio.toUpperCase();
        limpio = limpio.replace(" ", "_");
        return limpio;
    }

    /**
     * Busca un estudiante por nombre completo.
     */
    private void buscarEstudiante(String nombreCompleto) {
        String claveBusqueda = convertirClaveFirebase(nombreCompleto);

        // 🔍 LOGS PARA DEPURAR
        Log.d("ASISTENCIA", "========================================");
        Log.d("ASISTENCIA", "📝 Nombre ingresado: " + nombreCompleto);
        Log.d("ASISTENCIA", "🔑 Clave convertida: " + claveBusqueda);
        Log.d("ASISTENCIA", "📅 Fecha actual: " + currentDate);
        Log.d("ASISTENCIA", "📂 Ruta Firebase: asistencias/" + currentDate);
        Log.d("ASISTENCIA", "========================================");

        DatabaseReference dayRef = databaseReference.child(currentDate);

        // Mostrar mensaje de búsqueda
        textViewStatus.setText("🔍 Buscando...");
        textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));

        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("ASISTENCIA", "📊 Snapshot existe: " + snapshot.exists());
                Log.d("ASISTENCIA", "📊 Total estudiantes: " + snapshot.getChildrenCount());

                if (snapshot.exists()) {
                    // Mostrar todos los nombres disponibles
                    Log.d("ASISTENCIA", "📋 Nombres en Firebase:");
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Log.d("ASISTENCIA", "   - " + child.getKey() + " = " + child.getValue());
                    }

                    boolean encontrado = false;
                    String nombreEncontrado = null;
                    Boolean presente = null;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        String key = child.getKey();
                        if (key != null) {
                            Log.d("ASISTENCIA", "🔍 Comparando: '" + key + "' vs '" + claveBusqueda + "'");
                            if (key.equalsIgnoreCase(claveBusqueda)) {
                                encontrado = true;
                                nombreEncontrado = key;
                                if (child.getValue() instanceof Boolean) {
                                    presente = child.getValue(Boolean.class);
                                }
                                Log.d("ASISTENCIA", "✅ ¡ENCONTRADO! " + key + " = " + presente);
                                break;
                            }
                        }
                    }

                    if (encontrado) {
                        String nombreFormateado = nombreEncontrado.replace("_", " ");
                        actualizarUI(true, presente, nombreFormateado);
                    } else {
                        Log.d("ASISTENCIA", "❌ No encontrado exacto, buscando sugerencias...");
                        buscarSugerencia(claveBusqueda, snapshot);
                    }
                } else {
                    Log.d("ASISTENCIA", "❌ No hay datos para la fecha: " + currentDate);
                    actualizarUI(false, null, null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ASISTENCIA", "🔥 Error: " + error.getMessage());
                Toast.makeText(getContext(), "Error al consultar asistencia", Toast.LENGTH_SHORT).show();
                resetStatus();
            }
        });
    }

    /**
     * Busca sugerencias similares cuando no encuentra exacto.
     */
    private void buscarSugerencia(String claveBusqueda, DataSnapshot snapshot) {
        String nombreEncontrado = null;
        Boolean presente = null;
        int minDiferencia = Integer.MAX_VALUE;

        for (DataSnapshot child : snapshot.getChildren()) {
            String key = child.getKey();
            if (key != null) {
                String keyUpper = key.toUpperCase();
                int distancia = calcularDistancia(keyUpper, claveBusqueda);
                Log.d("ASISTENCIA", "   Distancia '" + key + "' = " + distancia);
                if (distancia < minDiferencia && distancia < 5) {
                    minDiferencia = distancia;
                    nombreEncontrado = key;
                    if (child.getValue() instanceof Boolean) {
                        presente = child.getValue(Boolean.class);
                    }
                }
            }
        }

        if (nombreEncontrado != null) {
            Log.d("ASISTENCIA", "💡 Sugerencia: " + nombreEncontrado);
            String nombreFormateado = nombreEncontrado.replace("_", " ");
            actualizarUI(true, presente, nombreFormateado + " (Presente)");
        } else {
            Log.d("ASISTENCIA", "❌ No hay sugerencias");
            actualizarUI(false, null, null);
        }
    }

    /**
     * Calcula distancia de Levenshtein.
     */
    private int calcularDistancia(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }

    /**
     * Actualiza la UI con el resultado.
     */
    private void actualizarUI(boolean encontrado, Boolean presente, String nombre) {
        if (encontrado) {
            textViewStudentName.setText("👤 " + nombre);
            textViewStudentName.setVisibility(View.VISIBLE);

            if (presente != null && presente) {
                // ✅ PRESENTE - Verde
                statusIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                textViewStatus.setText("✅ PRESENTE");
                textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                textViewEntryTime.setText("Hora de ingreso no encontrada en la base de datos.");
                textViewExitTime.setText("Hola de salida no encontrada en la base de datos.");

            } else if (presente != null && !presente) {
                // ❌ AUSENTE - Rojo
                statusIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                textViewStatus.setText("❌ AUSENTE");
                textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                textViewEntryTime.setText("--:--:--");
                textViewExitTime.setText("--:--:--");
            } else {
                // ⚠️ Sin registro
                statusIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
                textViewStatus.setText("⚠️ Sin registro");
                textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
                textViewEntryTime.setText("--:--:--");
                textViewExitTime.setText("--:--:--");
            }
        } else {
            // ❌ No encontrado
            statusIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            textViewStatus.setText("❌ Estudiante no encontrado");
            textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            textViewStudentName.setVisibility(View.GONE);
            textViewEntryTime.setText("--:--:--");
            textViewExitTime.setText("--:--:--");
        }
    }

    /**
     * Estado inicial.
     */
    private void resetStatus() {
        statusIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        textViewStatus.setText("🔍 Busca a tu hijo");
        textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        textViewStudentName.setVisibility(View.GONE);
        textViewEntryTime.setText("--:--:--");
        textViewExitTime.setText("--:--:--");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}