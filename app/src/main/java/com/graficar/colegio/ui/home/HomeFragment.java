package com.graficar.colegio.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.graficar.colegio.R;
import com.graficar.colegio.databinding.FragmentHomeBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;

    // ============================================================
    // VISTAS
    // ============================================================
    private LinearLayout contenedorHijos;
    private TextView textViewStatus;
    private TextView textViewStudentName;
    private TextView textViewDate;

    // ============================================================
    // FIREBASE
    // ============================================================
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;

    // ============================================================
    // DATOS DEL PADRE
    // ============================================================
    private String authUid;
    private String idLocalPadre;
    private String nombrePadre;
    private List<String> hijosUids = new ArrayList<>();

    // ============================================================
    // MAPAS
    // ============================================================
    private final Map<String, String> mapaUidANombre = new HashMap<>();

    // ============================================================
    // FECHA ACTUAL (formato YYYY-MM-DD)
    // ============================================================
    private String fechaActual;

    // ============================================================
    // GRADO POR DEFECTO (si lo necesitas)
    // ============================================================
    private final String gradoActual = "primeroC";

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
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ========================================================
        // INICIALIZAR VISTAS
        // ========================================================
        contenedorHijos = root.findViewById(R.id.contenedorHijos);
        textViewStatus = root.findViewById(R.id.textViewStatus);
        textViewStudentName = root.findViewById(R.id.textViewStudentName);
        textViewDate = root.findViewById(R.id.textViewDate);

        // ========================================================
        // FECHA ACTUAL
        // ========================================================
        fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());
        textViewDate.setText("📅 " + fechaActual);

        // ========================================================
        // FIREBASE
        // ========================================================
        dbRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // ========================================================
        // CARGAR DATOS DEL PADRE LOGUEADO
        // ========================================================
        cargarPadreYHijos();

        return root;
    }

    // ============================================================
    // CARGAR PADRE Y SUS HIJOS
    // ============================================================
    private void cargarPadreYHijos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            textViewStatus.setText("❌ No hay sesión activa");
            return;
        }

        authUid = user.getUid();

        // 1. Buscar en authIndex
        dbRef.child("authIndex").child(authUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            textViewStatus.setText("❌ Usuario no registrado");
                            return;
                        }

                        String tipo = snapshot.child("tipo").getValue(String.class);
                        idLocalPadre = snapshot.child("idLocal").getValue(String.class);

                        // Verificar que sea apoderado
                        if (!"apoderado".equals(tipo)) {
                            textViewStatus.setText("⚠️ Esta pantalla es para apoderados");
                            return;
                        }

                        // 2. Cargar datos del padre
                        cargarDatosPadre();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        textViewStatus.setText("Error: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // CARGAR DATOS DEL PADRE
    // ============================================================
    private void cargarDatosPadre() {
        dbRef.child("usuarios").child(idLocalPadre)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            textViewStatus.setText("❌ Datos del apoderado no encontrados");
                            return;
                        }

                        nombrePadre = snapshot.child("nombre").getValue(String.class);

                        // Mostrar nombre del padre como encabezado
                        textViewStudentName.setText("👨‍👩‍👧 Apoderado: " + nombrePadre);
                        textViewStudentName.setVisibility(View.VISIBLE);

                        // 3. Obtener lista de hijos
                        hijosUids.clear();
                        for (DataSnapshot hijo : snapshot.child("estudiantes").getChildren()) {
                            String uid = hijo.getValue(String.class);
                            if (uid != null) {
                                hijosUids.add(uid);
                            }
                        }

                        if (hijosUids.isEmpty()) {
                            textViewStatus.setText("No tienes hijos registrados");
                            return;
                        }

                        // 4. Cargar nombres e info de cada hijo
                        cargarNombresHijos();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        textViewStatus.setText("Error: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // CARGAR NOMBRES DE LOS HIJOS
    // ============================================================
    private void cargarNombresHijos() {
        mapaUidANombre.clear();
        final int total = hijosUids.size();
        final int[] cargados = {0};

        for (String uid : hijosUids) {
            dbRef.child("estudiantes").child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String nombre = snapshot.child("nombre").getValue(String.class);
                            if (nombre != null) {
                                mapaUidANombre.put(uid, nombre);
                            }
                            cargados[0]++;
                            if (cargados[0] == total) {
                                // Ya tenemos todos los nombres
                                mostrarHijosConAsistencia();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            cargados[0]++;
                            if (cargados[0] == total) {
                                mostrarHijosConAsistencia();
                            }
                        }
                    });
        }
    }

    // ============================================================
    // MOSTRAR HIJOS CON SU ASISTENCIA
    // ============================================================
    private void mostrarHijosConAsistencia() {
        if (!isAdded()) return;

        // Limpiar contenedor
        contenedorHijos.removeAllViews();

        final int total = hijosUids.size();
        final int[] cargados = {0};

        for (String uidHijo : hijosUids) {
            // Crear card para cada hijo
            CardView card = crearCardHijo(uidHijo);
            contenedorHijos.addView(card);

            // Consultar asistencia de ese hijo
            dbRef.child("asistencias")
                    .child(fechaActual)
                    .child(gradoActual)
                    .child(uidHijo)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            actualizarCardAsistencia(card, snapshot);
                            cargados[0]++;
                            if (cargados[0] == total) {
                                textViewStatus.setText("✅ Datos cargados");
                                textViewStatus.setTextColor(
                                        ContextCompat.getColor(requireContext(),
                                                android.R.color.holo_green_dark));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            cargados[0]++;
                            if (cargados[0] == total) {
                                textViewStatus.setText("Error al cargar asistencias");
                            }
                        }
                    });
        }
    }

    // ============================================================
    // CREAR CARD PARA UN HIJO
    // ============================================================
    private CardView crearCardHijo(String uidHijo) {
        CardView card = new CardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setRadius(16);
        card.setCardElevation(4);
        card.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), android.R.color.white));

        LinearLayout contenido = new LinearLayout(requireContext());
        contenido.setOrientation(LinearLayout.VERTICAL);
        contenido.setPadding(24, 24, 24, 24);
        contenido.setTag("contenido");

        // Nombre del hijo
        TextView tvNombre = new TextView(requireContext());
        String nombre = mapaUidANombre.getOrDefault(uidHijo, "Estudiante");
        tvNombre.setText("👤 " + nombre);
        tvNombre.setTextSize(18);
        tvNombre.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.black));
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNombre.setTag("nombre");
        contenido.addView(tvNombre);

        // Estado
        TextView tvEstado = new TextView(requireContext());
        tvEstado.setText("🔄 Consultando...");
        tvEstado.setTextSize(16);
        tvEstado.setPadding(0, 16, 0, 0);
        tvEstado.setTag("estado");
        contenido.addView(tvEstado);

        // Entrada
        TextView tvEntrada = new TextView(requireContext());
        tvEntrada.setText("⏰ Entrada: --:--");
        tvEntrada.setTextSize(14);
        tvEntrada.setPadding(0, 12, 0, 0);
        tvEntrada.setTag("entrada");
        contenido.addView(tvEntrada);

        // Salida
        TextView tvSalida = new TextView(requireContext());
        tvSalida.setText("⏰ Salida: --:--");
        tvSalida.setTextSize(14);
        tvSalida.setPadding(0, 8, 0, 0);
        tvSalida.setTag("salida");
        contenido.addView(tvSalida);

        card.addView(contenido);
        return card;
    }

    // ============================================================
    // ACTUALIZAR CARD CON ASISTENCIA
    // ============================================================
    private void actualizarCardAsistencia(CardView card, DataSnapshot snapshot) {
        LinearLayout contenido = card.findViewWithTag("contenido");
        if (contenido == null) return;

        TextView tvEstado = contenido.findViewWithTag("estado");
        TextView tvEntrada = contenido.findViewWithTag("entrada");
        TextView tvSalida = contenido.findViewWithTag("salida");

        if (!snapshot.exists()) {
            tvEstado.setText("⚠️ Sin registro de asistencia");
            tvEstado.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
            return;
        }

        Boolean asistio = snapshot.child("asistio").getValue(Boolean.class);
        String entrada = snapshot.child("entrada").getValue(String.class);
        String salida = snapshot.child("salida").getValue(String.class);

        if (asistio != null && asistio) {
            tvEstado.setText("✅ PRESENTE");
            tvEstado.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
            tvEntrada.setText("⏰ Entrada: " + (entrada != null ? entrada : "--:--"));
            tvSalida.setText("⏰ Salida: " + (salida != null ? salida : "--:--"));
        } else {
            tvEstado.setText("❌ AUSENTE");
            tvEstado.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            tvEntrada.setText("⏰ Entrada: --:--");
            tvSalida.setText("⏰ Salida: --:--");
        }
    }

    // ============================================================
    // ON DESTROY VIEW
    // ============================================================
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}