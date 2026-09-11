package com.graficar.colegio.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    // ============================================================
    // VISTAS
    // ============================================================

    private EditText editTextNameSearch;
    private TextView textViewStatus;
    private View statusIndicator;
    private TextView textViewEntryTime;
    private TextView textViewExitTime;
    private TextView textViewStudentName;
    private TextView textViewDate;
    private Button btnBuscar;

    // ============================================================
    // FIREBASE
    // ============================================================

    private DatabaseReference databaseReference;

    // ============================================================
    // FECHA DE PRUEBA
    // ============================================================

    private String currentDate = "2026-08-20";

    // ============================================================
    // MAPAS
    // ============================================================

    // UID -> Nombre
    private final Map<String, String> mapaUidANombre = new HashMap<>();

    // Nombre en MAYÚSCULAS -> UID
    private final Map<String, String> mapaNombreAUid = new HashMap<>();

    private boolean nombresCargados = false;

    // ============================================================
    // GRADO
    // ============================================================

    private final String gradoActual = "primeroC";

    // ============================================================
    // LOG
    // ============================================================

    private static final String TAG = "ASISTENCIA";


    // ============================================================
    // ON CREATE VIEW
    // ============================================================

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentHomeBinding.inflate(
                inflater,
                container,
                false
        );

        View root = binding.getRoot();

        // ========================================================
        // INICIALIZAR VISTAS
        // ========================================================

        editTextNameSearch =
                root.findViewById(R.id.editTextNameSearch);

        textViewStatus =
                root.findViewById(R.id.textViewStatus);

        statusIndicator =
                root.findViewById(R.id.statusIndicator);

        textViewEntryTime =
                root.findViewById(R.id.textViewEntryTime);

        textViewExitTime =
                root.findViewById(R.id.textViewExitTime);

        textViewStudentName =
                root.findViewById(R.id.textViewStudentName);

        textViewDate =
                root.findViewById(R.id.textViewDate);

        btnBuscar =
                root.findViewById(R.id.btnBuscar);


        // ========================================================
        // FECHA
        // ========================================================

        textViewDate.setText("📅 " + currentDate);


        // ========================================================
        // FIREBASE
        // ========================================================

        databaseReference =
                FirebaseDatabase
                        .getInstance()
                        .getReference();


        // ========================================================
        // ESTADO INICIAL
        // ========================================================

        resetStatus();


        // ========================================================
        // CARGAR ESTUDIANTES
        // ========================================================

        cargarNombresEstudiantes();


        // ========================================================
        // BOTÓN BUSCAR
        // ========================================================

        btnBuscar.setOnClickListener(v -> {

            String query =
                    editTextNameSearch
                            .getText()
                            .toString()
                            .trim();

            if (!query.isEmpty()) {

                buscarEstudiante(query);

            } else {

                Toast.makeText(
                        getContext(),
                        "Ingresa el nombre de tu hijo",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });


        // ========================================================
        // TECLADO - ENTER / BUSCAR
        // ========================================================

        editTextNameSearch.setOnEditorActionListener(
                (v, actionId, event) -> {

                    if (actionId == EditorInfo.IME_ACTION_SEARCH
                            || actionId == EditorInfo.IME_ACTION_DONE) {

                        String query =
                                editTextNameSearch
                                        .getText()
                                        .toString()
                                        .trim();

                        if (!query.isEmpty()) {

                            buscarEstudiante(query);

                            return true;
                        }
                    }

                    return false;
                }
        );


        return root;
    }


    // ============================================================
    // CARGAR NOMBRES DE ESTUDIANTES
    // ============================================================

    private void cargarNombresEstudiantes() {

        Log.d(
                TAG,
                "📥 Cargando estudiantes..."
        );

        textViewStatus.setText(
                "🔄 Cargando..."
        );

        textViewStatus.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_blue_dark
                )
        );


        databaseReference
                .child("estudiantes")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                mapaUidANombre.clear();
                                mapaNombreAUid.clear();

                                nombresCargados = true;


                                // =================================================
                                // RECORRER ESTUDIANTES
                                // =================================================

                                for (DataSnapshot child :
                                        snapshot.getChildren()) {

                                    String uid =
                                            child.getKey();

                                    String nombre =
                                            child
                                                    .child("nombre")
                                                    .getValue(String.class);

                                    String grado =
                                            child
                                                    .child("grado")
                                                    .getValue(String.class);


                                    if (uid != null
                                            && nombre != null
                                            && !nombre.trim().isEmpty()
                                            && grado != null
                                            && grado.equalsIgnoreCase(
                                            gradoActual
                                    )) {

                                        String nombreMayusculas =
                                                nombre
                                                        .trim()
                                                        .toUpperCase(
                                                                Locale.getDefault()
                                                        );


                                        // UID -> NOMBRE
                                        mapaUidANombre.put(
                                                uid,
                                                nombreMayusculas
                                        );


                                        // NOMBRE -> UID
                                        mapaNombreAUid.put(
                                                nombreMayusculas,
                                                uid
                                        );


                                        Log.d(
                                                TAG,
                                                "✅ Estudiante cargado: "
                                                        + uid
                                                        + " -> "
                                                        + nombreMayusculas
                                        );
                                    }
                                }


                                Log.d(
                                        TAG,
                                        "✅ Lista de estudiantes lista"
                                );


                                // =================================================
                                // NO MOSTRAR CANTIDAD DE ESTUDIANTES
                                // =================================================

                                if (isAdded()) {

                                    resetStatus();
                                }
                            }


                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                Log.e(
                                        TAG,
                                        "❌ Error al cargar estudiantes: "
                                                + error.getMessage()
                                );

                                nombresCargados = false;


                                if (isAdded()) {

                                    textViewStatus.setText(
                                            "❌ Error al cargar datos"
                                    );

                                    textViewStatus.setTextColor(
                                            ContextCompat.getColor(
                                                    requireContext(),
                                                    android.R.color
                                                            .holo_red_dark
                                            )
                                    );
                                }
                            }
                        }
                );
    }


    // ============================================================
    // CONVERTIR NOMBRE A UID
    // ============================================================

    private String convertirNombreAUid(
            String nombre
    ) {

        if (!nombresCargados
                || mapaNombreAUid.isEmpty()) {

            return null;
        }


        String nombreUpper =
                nombre
                        .trim()
                        .toUpperCase(
                                Locale.getDefault()
                        );


        // ========================================================
        // COINCIDENCIA EXACTA
        // ========================================================

        if (mapaNombreAUid.containsKey(
                nombreUpper
        )) {

            return mapaNombreAUid.get(
                    nombreUpper
            );
        }


        // ========================================================
        // COINCIDENCIA APROXIMADA
        // ========================================================

        String mejorCoincidencia = null;

        int minDiferencia =
                Integer.MAX_VALUE;


        for (String key :
                mapaNombreAUid.keySet()) {

            int distancia =
                    calcularDistancia(
                            key,
                            nombreUpper
                    );


            if (distancia < minDiferencia
                    && distancia < 5) {

                minDiferencia = distancia;

                mejorCoincidencia = key;
            }
        }


        if (mejorCoincidencia != null) {

            return mapaNombreAUid.get(
                    mejorCoincidencia
            );
        }


        return null;
    }


    // ============================================================
    // BUSCAR ESTUDIANTE
    // ============================================================

    private void buscarEstudiante(
            String nombreCompleto
    ) {

        Log.d(
                TAG,
                "🔍 Buscando: "
                        + nombreCompleto
        );


        // ========================================================
        // SI TODAVÍA NO CARGÓ LOS ESTUDIANTES
        // ========================================================

        if (!nombresCargados
                || mapaUidANombre.isEmpty()) {

            textViewStatus.setText(
                    "⏳ Cargando lista..."
            );

            textViewStatus.setTextColor(
                    ContextCompat.getColor(
                            requireContext(),
                            android.R.color.holo_blue_dark
                    )
            );


            cargarNombresEstudiantes();


            new android.os.Handler()
                    .postDelayed(() -> {

                        if (isAdded()) {

                            buscarEstudiante(
                                    nombreCompleto
                            );
                        }

                    }, 1000);


            return;
        }


        // ========================================================
        // MOSTRAR BUSCANDO
        // ========================================================

        textViewStatus.setText(
                "🔍 Buscando..."
        );

        textViewStatus.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_blue_dark
                )
        );


        // ========================================================
        // OBTENER UID
        // ========================================================

        String uid =
                convertirNombreAUid(
                        nombreCompleto
                );


        if (uid == null) {

            Log.d(
                    TAG,
                    "❌ Estudiante no encontrado"
            );


            actualizarUI(
                    false,
                    null,
                    null,
                    null,
                    null
            );

            return;
        }


        Log.d(
                TAG,
                "✅ UID encontrado: " + uid
        );


        // ========================================================
        // RUTA FIREBASE
        // ========================================================

        DatabaseReference studentRef =
                databaseReference
                        .child("asistencias")
                        .child(currentDate)
                        .child(gradoActual)
                        .child(uid);


        Log.d(
                TAG,
                "📂 Consultando: asistencias/"
                        + currentDate
                        + "/"
                        + gradoActual
                        + "/"
                        + uid
        );


        // ========================================================
        // CONSULTAR ASISTENCIA
        // ========================================================

        studentRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (!snapshot.exists()) {

                            Log.d(
                                    TAG,
                                    "❌ No existe asistencia"
                            );


                            actualizarUI(
                                    false,
                                    null,
                                    null,
                                    null,
                                    null
                            );

                            return;
                        }


                        // =================================================
                        // LEER NUEVA ESTRUCTURA
                        // =================================================

                        Boolean asistio =
                                snapshot
                                        .child("asistio")
                                        .getValue(Boolean.class);


                        String entrada =
                                snapshot
                                        .child("entrada")
                                        .getValue(String.class);


                        String salida =
                                snapshot
                                        .child("salida")
                                        .getValue(String.class);


                        Log.d(
                                TAG,
                                "📊 Asistió: " + asistio
                        );

                        Log.d(
                                TAG,
                                "⏰ Entrada: " + entrada
                        );

                        Log.d(
                                TAG,
                                "⏰ Salida: " + salida
                        );


                        // =================================================
                        // OBTENER NOMBRE
                        // =================================================

                        String nombreEstudiante =
                                mapaUidANombre.get(uid);


                        if (nombreEstudiante == null) {

                            nombreEstudiante =
                                    nombreCompleto;
                        }


                        // =================================================
                        // ACTUALIZAR PANTALLA
                        // =================================================

                        actualizarUI(
                                true,
                                asistio,
                                nombreEstudiante,
                                entrada,
                                salida
                        );
                    }


                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        Log.e(
                                TAG,
                                "🔥 Error Firebase: "
                                        + error.getMessage()
                        );


                        Toast.makeText(
                                getContext(),
                                "Error al consultar asistencia",
                                Toast.LENGTH_SHORT
                        ).show();


                        resetStatus();
                    }
                }
        );
    }


    // ============================================================
    // ACTUALIZAR UI
    // ============================================================

    private void actualizarUI(
            boolean encontrado,
            Boolean presente,
            String nombre,
            String entrada,
            String salida
    ) {

        // ========================================================
        // ESTUDIANTE ENCONTRADO
        // ========================================================

        if (encontrado) {

            String nombreMostrar =
                    nombre != null
                            ? nombre.replace("_", " ")
                            : "Estudiante";


            textViewStudentName.setText(
                    "👤 " + nombreMostrar
            );


            textViewStudentName.setVisibility(
                    View.VISIBLE
            );


            // ====================================================
            // PRESENTE
            // ====================================================

            if (presente != null
                    && presente) {

                statusIndicator.setBackgroundColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_green_dark
                        )
                );


                textViewStatus.setText(
                        "✅ PRESENTE"
                );


                textViewStatus.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_green_dark
                        )
                );


                // =================================================
                // ENTRADA
                // =================================================

                if (entrada != null
                        && !entrada.trim().isEmpty()) {

                    textViewEntryTime.setText(
                            entrada
                    );

                } else {

                    textViewEntryTime.setText(
                            "--:--"
                    );
                }


                // =================================================
                // SALIDA
                // =================================================

                if (salida != null
                        && !salida.trim().isEmpty()) {

                    textViewExitTime.setText(
                            salida
                    );

                } else {

                    textViewExitTime.setText(
                            "--:--"
                    );
                }
            }


            // ====================================================
            // AUSENTE
            // ====================================================

            else if (presente != null
                    && !presente) {

                statusIndicator.setBackgroundColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_red_dark
                        )
                );


                textViewStatus.setText(
                        "❌ AUSENTE"
                );


                textViewStatus.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_red_dark
                        )
                );


                textViewEntryTime.setText(
                        "--:--"
                );


                textViewExitTime.setText(
                        "--:--"
                );
            }


            // ====================================================
            // SIN REGISTRO
            // ====================================================

            else {

                statusIndicator.setBackgroundColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_orange_dark
                        )
                );


                textViewStatus.setText(
                        "⚠️ Sin registro"
                );


                textViewStatus.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_orange_dark
                        )
                );


                textViewEntryTime.setText(
                        "--:--"
                );


                textViewExitTime.setText(
                        "--:--"
                );
            }

        } else {

            // ========================================================
            // NO ENCONTRADO
            // ========================================================

            statusIndicator.setBackgroundColor(
                    ContextCompat.getColor(
                            requireContext(),
                            android.R.color.darker_gray
                    )
            );


            textViewStatus.setText(
                    "❌ Estudiante no encontrado"
            );


            textViewStatus.setTextColor(
                    ContextCompat.getColor(
                            requireContext(),
                            android.R.color.darker_gray
                    )
            );


            textViewStudentName.setVisibility(
                    View.GONE
            );


            textViewEntryTime.setText(
                    "--:--"
            );


            textViewExitTime.setText(
                    "--:--"
            );
        }
    }


    // ============================================================
    // ESTADO INICIAL
    // ============================================================

    private void resetStatus() {

        if (!isAdded()) {
            return;
        }


        statusIndicator.setBackgroundColor(
                ContextCompat.getColor(
                        requireContext(),
                        android.R.color.darker_gray
                )
        );


        textViewStatus.setText(
                "🔍 Busca a tu hijo"
        );


        textViewStatus.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        android.R.color.darker_gray
                )
        );


        textViewStudentName.setVisibility(
                View.GONE
        );


        textViewEntryTime.setText(
                "--:--"
        );


        textViewExitTime.setText(
                "--:--"
        );
    }


    // ============================================================
    // LEVENSHTEIN
    // ============================================================

    private int calcularDistancia(
            String s1,
            String s2
    ) {

        int[][] dp =
                new int[
                        s1.length() + 1
                        ][
                        s2.length() + 1
                        ];


        for (int i = 0;
             i <= s1.length();
             i++) {

            dp[i][0] = i;
        }


        for (int j = 0;
             j <= s2.length();
             j++) {

            dp[0][j] = j;
        }


        for (int i = 1;
             i <= s1.length();
             i++) {

            for (int j = 1;
                 j <= s2.length();
                 j++) {

                int cost =
                        s1.charAt(i - 1)
                                == s2.charAt(j - 1)
                                ? 0
                                : 1;


                dp[i][j] =
                        Math.min(
                                Math.min(
                                        dp[i - 1][j] + 1,
                                        dp[i][j - 1] + 1
                                ),
                                dp[i - 1][j - 1] + cost
                        );
            }
        }


        return dp[
                s1.length()
                ][
                s2.length()
                ];
    }


    // ============================================================
    // DESTROY VIEW
    // ============================================================

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        binding = null;
    }
}
