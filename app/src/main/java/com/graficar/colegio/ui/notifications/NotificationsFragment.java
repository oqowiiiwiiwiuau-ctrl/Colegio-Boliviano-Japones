package com.graficar.colegio.ui.notifications;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.graficar.colegio.R;
import com.graficar.colegio.databinding.FragmentNotificationsBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    // ============================================================
    // UI COMPONENTS
    // ============================================================
    private EditText editTextStudentName;
    private Button btnBuscar;
    private TextView textViewStudentName;
    private LinearLayout layoutObservaciones;
    private ScrollView scrollViewResultados;

    // ============================================================
    // FIREBASE
    // ============================================================
    private DatabaseReference databaseReference;

    // ============================================================
    // MAPAS PARA NOMBRES Y UIDS
    // ============================================================
    private Map<String, String> mapaUidANombre = new HashMap<>();
    private Map<String, String> mapaNombreAUid = new HashMap<>();
    private boolean nombresCargados = false;

    // ============================================================
    // CONFIGURACIÓN
    // ============================================================
    private String gradoActual = "primeroC";
    private String currentDate;

    private final String[] trimestres = {"trimestre1", "trimestre2", "trimestre3"};
    private final String[] trimestresNombres = {"Trimestre 1", "Trimestre 2", "Trimestre 3"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ============================================================
        // INICIALIZAR VISTAS
        // ============================================================
        editTextStudentName = root.findViewById(R.id.editTextStudentName);
        btnBuscar = root.findViewById(R.id.btnBuscarObservaciones);
        textViewStudentName = root.findViewById(R.id.textViewStudentName);
        layoutObservaciones = root.findViewById(R.id.layoutObservaciones);
        scrollViewResultados = root.findViewById(R.id.scrollViewResultados);

        // Obtener fecha actual
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Inicializar Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Cargar nombres de estudiantes
        cargarNombresEstudiantes();

        // ============================================================
        // CONFIGURAR BOTÓN DE BÚSQUEDA
        // ============================================================
        btnBuscar.setOnClickListener(v -> {
            String query = editTextStudentName.getText().toString().trim();
            if (!query.isEmpty()) {
                buscarObservaciones(query);
            } else {
                Toast.makeText(getContext(), "Ingresa el nombre del estudiante", Toast.LENGTH_SHORT).show();
            }
        });

        // Buscar al presionar Enter
        editTextStudentName.setOnEditorActionListener((v, actionId, event) -> {
            String query = editTextStudentName.getText().toString().trim();
            if (!query.isEmpty()) {
                buscarObservaciones(query);
                return true;
            }
            return false;
        });

        // Limpiar resultados al borrar el texto
        editTextStudentName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.toString().trim().isEmpty()) {
                    limpiarResultados();
                }
            }
        });

        // ============================================================
        // ESTADO INICIAL
        // ============================================================
        limpiarResultados();

        return root;
    }

    // ============================================================
    // CARGAR NOMBRES DE ESTUDIANTES
    // ============================================================
    private void cargarNombresEstudiantes() {
        Log.d("NOTIFICACIONES", "📥 Cargando nombres de estudiantes...");

        databaseReference.child("estudiantes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mapaUidANombre.clear();
                mapaNombreAUid.clear();
                nombresCargados = true;

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String uid = child.getKey();
                        String nombre = child.child("nombre").getValue(String.class);
                        String grado = child.child("grado").getValue(String.class);

                        if (nombre != null && !nombre.isEmpty() && grado != null && grado.equals(gradoActual)) {
                            String nombreMayusculas = nombre.toUpperCase();
                            mapaUidANombre.put(uid, nombreMayusculas);
                            mapaNombreAUid.put(nombreMayusculas, uid);
                            Log.d("NOTIFICACIONES", "📌 Cargado: " + uid + " -> " + nombreMayusculas);
                        }
                    }
                }

                Log.d("NOTIFICACIONES", "📊 Total estudiantes cargados: " + mapaUidANombre.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NOTIFICACIONES", "❌ Error al cargar nombres: " + error.getMessage());
                nombresCargados = false;
            }
        });
    }

    // ============================================================
    // CONVERTIR NOMBRE A UID
    // ============================================================
    private String convertirNombreAUid(String nombre) {
        if (!nombresCargados || mapaNombreAUid.isEmpty()) {
            cargarNombresEstudiantes();
            return null;
        }

        String nombreUpper = nombre.trim().toUpperCase();

        if (mapaNombreAUid.containsKey(nombreUpper)) {
            return mapaNombreAUid.get(nombreUpper);
        }

        // Buscar coincidencia parcial
        String mejorCoincidencia = null;
        int minDiferencia = Integer.MAX_VALUE;

        for (String key : mapaNombreAUid.keySet()) {
            int distancia = calcularDistancia(key, nombreUpper);
            if (distancia < minDiferencia && distancia < 5) {
                minDiferencia = distancia;
                mejorCoincidencia = key;
            }
        }

        if (mejorCoincidencia != null) {
            return mapaNombreAUid.get(mejorCoincidencia);
        }

        return null;
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

    // ============================================================
    // BUSCAR OBSERVACIONES
    // ============================================================
    private void buscarObservaciones(String nombreCompleto) {
        // Verificar si los nombres están cargados
        if (!nombresCargados || mapaNombreAUid.isEmpty()) {
            Toast.makeText(getContext(), "Cargando lista de estudiantes...", Toast.LENGTH_SHORT).show();
            cargarNombresEstudiantes();
            new android.os.Handler().postDelayed(() -> buscarObservaciones(nombreCompleto), 1000);
            return;
        }

        // Convertir nombre a UID
        String uid = convertirNombreAUid(nombreCompleto);

        if (uid == null) {
            Toast.makeText(getContext(), "❌ Estudiante no encontrado", Toast.LENGTH_SHORT).show();
            limpiarResultados();
            return;
        }

        String nombreEstudiante = mapaUidANombre.getOrDefault(uid, nombreCompleto);
        textViewStudentName.setText("👤 " + nombreEstudiante);
        textViewStudentName.setVisibility(View.VISIBLE);
        scrollViewResultados.setVisibility(View.VISIBLE);

        // Mostrar mensaje de carga
        mostrarMensajeCarga();

        // Buscar observaciones
        DatabaseReference obsRef = databaseReference
                .child("observaciones")
                .child(uid);

        obsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                layoutObservaciones.removeAllViews();

                if (!snapshot.exists()) {
                    mostrarMensajeSinObservaciones();
                    return;
                }

                boolean tieneObservaciones = false;

                // Recorrer los trimestres
                for (int i = 0; i < trimestres.length; i++) {
                    String trimestreKey = trimestres[i];
                    String trimestreNombre = trimestresNombres[i];

                    DataSnapshot trimestreSnapshot = snapshot.child(trimestreKey);

                    if (trimestreSnapshot.exists()) {
                        String texto = trimestreSnapshot.child("texto").getValue(String.class);

                        if (texto != null && !texto.isEmpty()) {
                            tieneObservaciones = true;
                            agregarTarjetaObservacion(trimestreNombre, texto);
                        }
                    }
                }

                if (!tieneObservaciones) {
                    mostrarMensajeSinObservaciones();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al cargar observaciones", Toast.LENGTH_SHORT).show();
                mostrarMensajeError();
            }
        });
    }

    // ============================================================
    // AGREGAR TARJETA DE OBSERVACIÓN
    // ============================================================
    private void agregarTarjetaObservacion(String trimestre, String texto) {
        // CardView para cada observación
        CardView card = new CardView(requireContext());
        CardView.LayoutParams cardParams = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(4));
        card.setContentPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        // Contenido de la tarjeta
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);

        // Título del trimestre
        TextView tvTrimestre = new TextView(requireContext());
        tvTrimestre.setText("📌 " + trimestre);
        tvTrimestre.setTextSize(16);
        tvTrimestre.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTrimestre.setTextColor(ContextCompat.getColor(requireContext(), R.color.curso_primero));
        tvTrimestre.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        content.addView(tvTrimestre);

        // Texto de la observación
        TextView tvTexto = new TextView(requireContext());
        tvTexto.setText(texto);
        tvTexto.setTextSize(14);
        tvTexto.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        tvTexto.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        tvTexto.setPadding(0, dpToPx(8), 0, 0);
        content.addView(tvTexto);

        // Fecha
        TextView tvFecha = new TextView(requireContext());
        tvFecha.setText("📅 " + currentDate);
        tvFecha.setTextSize(12);
        tvFecha.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        tvFecha.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        tvFecha.setPadding(0, dpToPx(8), 0, 0);
        content.addView(tvFecha);

        card.addView(content);
        layoutObservaciones.addView(card);
    }

    // ============================================================
    // MENSAJES DE ESTADO
    // ============================================================
    private void mostrarMensajeCarga() {
        layoutObservaciones.removeAllViews();
        TextView tvMensaje = new TextView(requireContext());
        tvMensaje.setText("📥 Cargando observaciones...");
        tvMensaje.setTextSize(16);
        tvMensaje.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));
        tvMensaje.setPadding(0, dpToPx(20), 0, 0);
        tvMensaje.setGravity(android.view.Gravity.CENTER);
        layoutObservaciones.addView(tvMensaje);
    }

    private void mostrarMensajeSinObservaciones() {
        TextView tvMensaje = new TextView(requireContext());
        tvMensaje.setText("ℹ️ No hay observaciones registradas para este estudiante");
        tvMensaje.setTextSize(16);
        tvMensaje.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        tvMensaje.setPadding(0, dpToPx(20), 0, 0);
        tvMensaje.setGravity(android.view.Gravity.CENTER);
        layoutObservaciones.addView(tvMensaje);
    }

    private void mostrarMensajeError() {
        TextView tvMensaje = new TextView(requireContext());
        tvMensaje.setText("❌ Error al cargar las observaciones");
        tvMensaje.setTextSize(16);
        tvMensaje.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        tvMensaje.setPadding(0, dpToPx(20), 0, 0);
        tvMensaje.setGravity(android.view.Gravity.CENTER);
        layoutObservaciones.addView(tvMensaje);
    }

    // ============================================================
    // LIMPIAR RESULTADOS
    // ============================================================
    private void limpiarResultados() {
        if (layoutObservaciones != null) {
            layoutObservaciones.removeAllViews();
        }
        if (textViewStudentName != null) {
            textViewStudentName.setVisibility(View.GONE);
        }
        if (scrollViewResultados != null) {
            scrollViewResultados.setVisibility(View.GONE);
        }
    }

    // ============================================================
    // UTILIDADES
    // ============================================================
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}