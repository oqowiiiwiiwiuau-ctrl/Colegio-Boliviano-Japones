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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    // Mapa para guardar UID -> Nombre (TODO EN MAYÚSCULAS)
    private Map<String, String> mapaUidANombre = new HashMap<>();          // uid -> nombre (mayúsculas)
    private Map<String, String> mapaNombreAUid = new HashMap<>();          // nombre (mayúsculas) -> uid
    private boolean nombresCargados = false;

    // Grado actual
    private String gradoActual = "primeroC";

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

        // Actualizar fecha en UI
        TextView tvDate = root.findViewById(R.id.textViewDate);
        if (tvDate != null) {
            tvDate.setText("📅 " + currentDate);
        }

        // Inicializar Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Cargar los nombres de los estudiantes
        cargarNombresEstudiantes();

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

    // ============================================================
    // CARGAR NOMBRES DE ESTUDIANTES (TODO EN MAYÚSCULAS)
    // ============================================================
    private void cargarNombresEstudiantes() {
        Log.d("ASISTENCIA", "📥 CARGANDO NOMBRES DE ESTUDIANTES...");

        textViewStatus.setText("🔄 Cargando lista de estudiantes...");
        textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));

        databaseReference.child("estudiantes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mapaUidANombre.clear();
                mapaNombreAUid.clear();
                nombresCargados = true;

                Log.d("ASISTENCIA", "📊 Total estudiantes en Firebase: " + snapshot.getChildrenCount());

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String uid = child.getKey();
                        String nombre = child.child("nombre").getValue(String.class);
                        String grado = child.child("grado").getValue(String.class);

                        Log.d("ASISTENCIA", "  🔍 UID: " + uid + ", Nombre: " + nombre + ", Grado: " + grado);

                        if (nombre != null && !nombre.isEmpty() && grado != null && grado.equals(gradoActual)) {
                            // ✅ Guardar TODO en MAYÚSCULAS para comparación insensible
                            String nombreMayusculas = nombre.toUpperCase();
                            mapaUidANombre.put(uid, nombreMayusculas);
                            mapaNombreAUid.put(nombreMayusculas, uid);
                            Log.d("ASISTENCIA", "  ✅ Cargado: " + uid + " -> " + nombreMayusculas);
                        }
                    }
                }

                Log.d("ASISTENCIA", "📊 Total estudiantes cargados para " + gradoActual + ": " + mapaUidANombre.size());

                // Mostrar todos los nombres cargados
                Log.d("ASISTENCIA", "📋 Nombres cargados (MAYÚSCULAS):");
                for (String key : mapaNombreAUid.keySet()) {
                    Log.d("ASISTENCIA", "   📌 " + key + " -> " + mapaNombreAUid.get(key));
                }

                requireActivity().runOnUiThread(() -> {
                    if (mapaUidANombre.isEmpty()) {
                        textViewStatus.setText("⚠️ No hay estudiantes en " + gradoActual);
                        textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
                    } else {
                        textViewStatus.setText("✅ " + mapaUidANombre.size() + " estudiantes cargados");
                        textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                    }
                });

                // Restaurar estado inicial después de 2 segundos
                new android.os.Handler().postDelayed(() -> {
                    if (editTextNameSearch.getText().toString().trim().isEmpty()) {
                        resetStatus();
                    }
                }, 2000);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ASISTENCIA", "❌ Error al cargar nombres: " + error.getMessage());
                nombresCargados = false;

                requireActivity().runOnUiThread(() -> {
                    textViewStatus.setText("❌ Error al cargar datos");
                    textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                });
            }
        });
    }

    // ============================================================
    // CONVERTIR NOMBRE A UID (INSENSIBLE A MAYÚSCULAS/MINÚSCULAS)
    // ============================================================
    private String convertirNombreAUid(String nombre) {
        Log.d("ASISTENCIA", "🔄 CONVIRTIENDO: " + nombre + " a UID");
        Log.d("ASISTENCIA", "   📊 nombresCargados: " + nombresCargados);
        Log.d("ASISTENCIA", "   📊 mapaNombreAUid.size(): " + mapaNombreAUid.size());

        // Si los nombres no están cargados, intentar cargarlos primero
        if (!nombresCargados || mapaNombreAUid.isEmpty()) {
            Log.d("ASISTENCIA", "⏳ Nombres no cargados aún, intentando cargar...");
            cargarNombresEstudiantes();
            return null;
        }

        // ✅ Convertir el nombre ingresado a MAYÚSCULAS para comparar
        String nombreUpper = nombre.trim().toUpperCase();
        Log.d("ASISTENCIA", "   🔑 Buscando (MAYÚSCULAS): " + nombreUpper);

        // Buscar en el mapa (todas las claves están en mayúsculas)
        if (mapaNombreAUid.containsKey(nombreUpper)) {
            String uid = mapaNombreAUid.get(nombreUpper);
            Log.d("ASISTENCIA", "   ✅ ENCONTRADO EXACTO: " + nombreUpper + " -> " + uid);
            return uid;
        }

        // Si no encuentra exacto, buscar por coincidencia parcial
        Log.d("ASISTENCIA", "   ❌ No encontrado exacto, buscando sugerencias...");
        String mejorCoincidencia = null;
        int minDiferencia = Integer.MAX_VALUE;

        for (String key : mapaNombreAUid.keySet()) {
            int distancia = calcularDistancia(key, nombreUpper);
            Log.d("ASISTENCIA", "      " + key + " vs " + nombreUpper + " = " + distancia);
            if (distancia < minDiferencia && distancia < 5) {
                minDiferencia = distancia;
                mejorCoincidencia = key;
            }
        }

        if (mejorCoincidencia != null) {
            Log.d("ASISTENCIA", "   💡 SUGERENCIA: " + mejorCoincidencia + " -> " + mapaNombreAUid.get(mejorCoincidencia));
            return mapaNombreAUid.get(mejorCoincidencia);
        }

        Log.d("ASISTENCIA", "   ❌ No se encontró coincidencia para: " + nombreUpper);
        return null;
    }

    // ============================================================
    // BUSCAR ESTUDIANTE
    // ============================================================
    private void buscarEstudiante(String nombreCompleto) {
        Log.d("ASISTENCIA", "========================================");
        Log.d("ASISTENCIA", "🔍 1. Nombre ingresado: " + nombreCompleto);
        Log.d("ASISTENCIA", "📊 mapaNombreAUid tiene: " + mapaNombreAUid.size() + " elementos");
        Log.d("ASISTENCIA", "📊 mapaUidANombre tiene: " + mapaUidANombre.size() + " elementos");
        Log.d("ASISTENCIA", "========================================");

        // Verificar si los nombres están cargados
        if (!nombresCargados || mapaUidANombre.isEmpty()) {
            Log.d("ASISTENCIA", "⏳ Nombres no cargados, cargando...");
            textViewStatus.setText("⏳ Cargando lista de estudiantes...");
            textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));
            cargarNombresEstudiantes();
            // Esperar un poco y reintentar
            new android.os.Handler().postDelayed(() -> {
                buscarEstudiante(nombreCompleto);
            }, 1000);
            return;
        }

        // Mostrar mensaje de búsqueda
        textViewStatus.setText("🔍 Buscando...");
        textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));

        // Convertir nombre a UID (insensible a mayúsculas/minúsculas)
        String uid = convertirNombreAUid(nombreCompleto);
        Log.d("ASISTENCIA", "🔑 2. UID encontrado: " + uid);

        if (uid == null) {
            Log.d("ASISTENCIA", "❌ 3. No se encontró UID para: " + nombreCompleto);
            actualizarUI(false, null, null);
            return;
        }

        // Buscar en asistencias/[fecha]/[grado]/[uid]
        DatabaseReference dayRef = databaseReference
                .child("asistencias")
                .child(currentDate)
                .child(gradoActual)
                .child(uid);

        Log.d("ASISTENCIA", "📂 3. Ruta: asistencias/" + currentDate + "/" + gradoActual + "/" + uid);

        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("ASISTENCIA", "📊 4. Snapshot existe: " + snapshot.exists());

                if (snapshot.exists()) {
                    Boolean presente = snapshot.getValue(Boolean.class);
                    Log.d("ASISTENCIA", "✅ 5. Resultado: " + presente);
                    String nombreEstudiante = mapaUidANombre.getOrDefault(uid, nombreCompleto);
                    actualizarUI(true, presente, nombreEstudiante);
                } else {
                    Log.d("ASISTENCIA", "❌ 5. No hay datos para: " + uid + " en " + currentDate);
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

    // ============================================================
    // ACTUALIZAR UI
    // ============================================================
    private void actualizarUI(boolean encontrado, Boolean presente, String nombre) {
        if (encontrado) {
            // Mostrar el nombre con formato original (con espacios)
            String nombreMostrar = nombre.replace("_", " ");
            textViewStudentName.setText("👤 " + nombreMostrar);
            textViewStudentName.setVisibility(View.VISIBLE);

            if (presente != null && presente) {
                // ✅ PRESENTE - Verde
                statusIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                textViewStatus.setText("✅ PRESENTE");
                textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                textViewEntryTime.setText("Hora de ingreso no encontrada");
                textViewExitTime.setText("Hora de salida no encontrada");

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

    // ============================================================
    // ESTADO INICIAL
    // ============================================================
    private void resetStatus() {
        statusIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        textViewStatus.setText("🔍 Busca a tu hijo");
        textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        textViewStudentName.setVisibility(View.GONE);
        textViewEntryTime.setText("--:--:--");
        textViewExitTime.setText("--:--:--");
    }

    // ============================================================
    // CALCULAR DISTANCIA DE LEVENSHTEIN
    // ============================================================
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}