package com.graficar.colegio.ui.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.graficar.colegio.databinding.FragmentNotificationsBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NOTIFICACIONES";

    private FragmentNotificationsBinding binding;

    // ============================================================
    // UI
    // ============================================================
    private Spinner spinnerHijos;
    private TextView textViewStudentName;
    private TextView textViewTotalObs;
    private LinearLayout layoutObservaciones;
    private View scrollViewResultados;
    private TextView textViewEstado;

    // ============================================================
    // FIREBASE
    // ============================================================
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;

    // ============================================================
    // PADRE Y HIJOS
    // ============================================================
    private String authUid;
    private String idLocalPadre;
    private String nombrePadre;

    private final List<String> hijosUids = new ArrayList<>();
    private final List<String> hijosNombres = new ArrayList<>();

    private String currentStudentUid = "";
    private String currentStudentName = "";

    // ============================================================
    // CACHÉ DE PROFESORES (para mostrar nombre en lugar del ID)
    // ============================================================
    private final Map<String, String> mapaProfesorIdANombre = new HashMap<>();
    private boolean profesoresCargados = false;

    // ============================================================
    // PREFERENCIAS (para marcar observaciones como leídas)
    // ============================================================
    private SharedPreferences prefs;

    // ============================================================
    // ON CREATE VIEW
    // ============================================================
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Inicializar vistas
        spinnerHijos = root.findViewById(R.id.spinnerHijos);
        textViewStudentName = root.findViewById(R.id.textViewStudentName);
        textViewTotalObs = root.findViewById(R.id.textViewTotalObs);
        layoutObservaciones = root.findViewById(R.id.layoutObservaciones);
        scrollViewResultados = root.findViewById(R.id.scrollViewResultados);
        textViewEstado = root.findViewById(R.id.textViewEstado);

        // SharedPreferences
        prefs = requireContext().getSharedPreferences("NOTIFICACIONES", Context.MODE_PRIVATE);

        // Firebase
        dbRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Ocultar resultados inicialmente
        limpiarResultados();
        if (textViewEstado != null) {
            textViewEstado.setText("🔄 Cargando...");
            textViewEstado.setVisibility(View.VISIBLE);
        }

        // 1. Cargar nombres de profesores primero (para mostrarlos)
        cargarNombresProfesores();

        return root;
    }

    // ============================================================
    // CARGAR NOMBRES DE PROFESORES
    // ============================================================
    private void cargarNombresProfesores() {
        dbRef.child("profesores")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        mapaProfesorIdANombre.clear();

                        for (DataSnapshot prof : snapshot.getChildren()) {
                            String id = prof.getKey();
                            String nombre = prof.child("nombre").getValue(String.class);
                            if (id != null && nombre != null) {
                                mapaProfesorIdANombre.put(id, nombre);
                            }
                        }

                        profesoresCargados = true;
                        Log.d(TAG, "Profesores cargados: " + mapaProfesorIdANombre.size());

                        // Después de cargar profesores, cargar el padre
                        cargarPadreYHijos();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al cargar profesores: " + error.getMessage());
                        // Aunque falle, seguimos con los IDs
                        cargarPadreYHijos();
                    }
                });
    }

    // ============================================================
    // CARGAR PADRE Y SUS HIJOS
    // ============================================================
    private void cargarPadreYHijos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            mostrarErrorEstado("❌ No hay sesión activa");
            return;
        }

        authUid = user.getUid();

        dbRef.child("authIndex").child(authUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            mostrarErrorEstado("❌ Usuario no registrado");
                            return;
                        }

                        String tipo = snapshot.child("tipo").getValue(String.class);
                        idLocalPadre = snapshot.child("idLocal").getValue(String.class);

                        if (!"apoderado".equals(tipo)) {
                            mostrarErrorEstado("⚠️ Esta pantalla es para apoderados");
                            return;
                        }

                        cargarDatosPadre();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        mostrarErrorEstado("Error: " + error.getMessage());
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
                            mostrarErrorEstado("❌ Datos del apoderado no encontrados");
                            return;
                        }

                        nombrePadre = snapshot.child("nombre").getValue(String.class);

                        hijosUids.clear();
                        for (DataSnapshot hijo : snapshot.child("estudiantes").getChildren()) {
                            String uid = hijo.getValue(String.class);
                            if (uid != null) {
                                hijosUids.add(uid);
                            }
                        }

                        if (hijosUids.isEmpty()) {
                            mostrarErrorEstado("No tienes hijos registrados");
                            return;
                        }

                        cargarNombresHijos();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        mostrarErrorEstado("Error: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // CARGAR NOMBRES DE LOS HIJOS
    // ============================================================
    private void cargarNombresHijos() {
        hijosNombres.clear();
        final int total = hijosUids.size();
        final int[] cargados = {0};

        for (String uid : hijosUids) {
            dbRef.child("estudiantes").child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String nombre = snapshot.child("nombre").getValue(String.class);
                            hijosNombres.add(nombre != null ? nombre : "Estudiante");

                            cargados[0]++;
                            if (cargados[0] == total) {
                                if (isAdded() && binding != null) {
                                    setupSpinnerHijos();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            hijosNombres.add("Estudiante");
                            cargados[0]++;
                            if (cargados[0] == total && isAdded() && binding != null) {
                                setupSpinnerHijos();
                            }
                        }
                    });
        }
    }

    // ============================================================
    // CONFIGURAR SPINNER DE HIJOS
    // ============================================================
    private void setupSpinnerHijos() {
        if (!isAdded() || binding == null) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                hijosNombres
        );

        spinnerHijos.setAdapter(adapter);
        spinnerHijos.setVisibility(View.VISIBLE);

        spinnerHijos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < hijosUids.size()) {
                    currentStudentUid = hijosUids.get(position);
                    currentStudentName = hijosNombres.get(position);

                    textViewStudentName.setText("👤 " + currentStudentName);
                    textViewStudentName.setVisibility(View.VISIBLE);
                    scrollViewResultados.setVisibility(View.VISIBLE);

                    // Cargar TODAS las observaciones del hijo
                    cargarTodasLasObservaciones(currentStudentUid);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (textViewEstado != null) {
            textViewEstado.setVisibility(View.GONE);
        }
    }

    // ============================================================
    // CARGAR TODAS LAS OBSERVACIONES DEL HIJO
    // Recorre: observaciones/{uid}/{fecha}/{obsId}
    // ============================================================
    private void cargarTodasLasObservaciones(String uid) {
        mostrarMensajeCarga();

        dbRef.child("observaciones").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        layoutObservaciones.removeAllViews();

                        if (!snapshot.exists()) {
                            mostrarMensajeSinObservaciones();
                            actualizarContador(0);
                            return;
                        }

                        // Lista para ordenar por fecha (más recientes primero)
                        List<ObservacionItem> listaObs = new ArrayList<>();

                        // Recorrer fechas
                        for (DataSnapshot fechaSnap : snapshot.getChildren()) {
                            String fecha = fechaSnap.getKey(); // "2026-09-15"

                            // Recorrer cada observación del día
                            for (DataSnapshot obsSnap : fechaSnap.getChildren()) {
                                String obsId = obsSnap.getKey();

                                String materia = obsSnap.child("materia").getValue(String.class);
                                String profesorId = obsSnap.child("profesorId").getValue(String.class);
                                String texto = obsSnap.child("texto").getValue(String.class);

                                if (texto != null && !texto.isEmpty()) {
                                    ObservacionItem item = new ObservacionItem(
                                            obsId, fecha, materia, profesorId, texto
                                    );
                                    listaObs.add(item);
                                }
                            }
                        }

                        if (listaObs.isEmpty()) {
                            mostrarMensajeSinObservaciones();
                            actualizarContador(0);
                            return;
                        }

                        // Ordenar por fecha descendente (más recientes primero)
                        Collections.sort(listaObs, new Comparator<ObservacionItem>() {
                            @Override
                            public int compare(ObservacionItem a, ObservacionItem b) {
                                return b.fecha.compareTo(a.fecha); // descendente
                            }
                        });

                        // Mostrar cada observación
                        for (ObservacionItem item : listaObs) {
                            agregarTarjetaObservacion(item);
                        }

                        actualizarContador(listaObs.size());
                        marcarComoLeidas(uid, listaObs);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        mostrarMensajeError();
                    }
                });
    }

    // ============================================================
    // TARJETA DE OBSERVACIÓN
    // ============================================================
    private void agregarTarjetaObservacion(ObservacionItem item) {
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

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);

        // ----------------------------------------------------------
        // Fila superior: Fecha + Materia
        // ----------------------------------------------------------
        LinearLayout filaSuperior = new LinearLayout(requireContext());
        filaSuperior.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvFecha = new TextView(requireContext());
        tvFecha.setText("📅 " + formatearFecha(item.fecha));
        tvFecha.setTextSize(13);
        tvFecha.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        tvFecha.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        filaSuperior.addView(tvFecha);

        TextView tvMateria = new TextView(requireContext());
        tvMateria.setText("📚 " + (item.materia != null ? item.materia : "General"));
        tvMateria.setTextSize(13);
        tvMateria.setTypeface(null, android.graphics.Typeface.BOLD);
        tvMateria.setTextColor(ContextCompat.getColor(requireContext(), R.color.curso_primero));
        filaSuperior.addView(tvMateria);

        content.addView(filaSuperior);

        // ----------------------------------------------------------
        // Nombre del profesor
        // ----------------------------------------------------------
        String nombreProfesor = mapaProfesorIdANombre.getOrDefault(
                item.profesorId,
                item.profesorId != null ? item.profesorId : "Profesor"
        );

        TextView tvProfesor = new TextView(requireContext());
        tvProfesor.setText("👨‍🏫 " + nombreProfesor);
        tvProfesor.setTextSize(13);
        tvProfesor.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        tvProfesor.setPadding(0, dpToPx(6), 0, 0);
        content.addView(tvProfesor);

        // ----------------------------------------------------------
        // Texto de la observación
        // ----------------------------------------------------------
        TextView tvTexto = new TextView(requireContext());
        tvTexto.setText(item.texto);
        tvTexto.setTextSize(15);
        tvTexto.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        tvTexto.setPadding(0, dpToPx(12), 0, 0);
        content.addView(tvTexto);

        card.addView(content);
        layoutObservaciones.addView(card);
    }

    // ============================================================
    // ACTUALIZAR CONTADOR
    // ============================================================
    private void actualizarContador(int total) {
        if (textViewTotalObs != null) {
            if (total > 0) {
                textViewTotalObs.setText("📋 " + total + " observación" +
                        (total == 1 ? "" : "es"));
                textViewTotalObs.setVisibility(View.VISIBLE);
            } else {
                textViewTotalObs.setVisibility(View.GONE);
            }
        }
    }

    // ============================================================
    // MARCAR COMO LEÍDAS (guarda última fecha vista)
    // ============================================================
    private void marcarComoLeidas(String uid, List<ObservacionItem> items) {
        if (items.isEmpty()) return;

        // La lista está ordenada de más reciente a más antigua
        // Guardamos la fecha de la más reciente
        String ultimaFecha = items.get(0).fecha;
        prefs.edit()
                .putString("ultima_obs_" + uid, ultimaFecha)
                .putInt("total_obs_" + uid, items.size())
                .apply();
    }

    // ============================================================
    // FORMATEAR FECHA (2026-09-15 → 15 sep 2026)
    // ============================================================
    private String formatearFecha(String fechaISO) {
        try {
            SimpleDateFormat entrada = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat salida = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
            Date date = entrada.parse(fechaISO);
            if (date != null) {
                return salida.format(date);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear fecha: " + fechaISO);
        }
        return fechaISO; // fallback
    }

    // ============================================================
    // CLASE INTERNA PARA ORDENAR
    // ============================================================
    private static class ObservacionItem {
        String obsId;
        String fecha;
        String materia;
        String profesorId;
        String texto;

        ObservacionItem(String obsId, String fecha, String materia,
                        String profesorId, String texto) {
            this.obsId = obsId;
            this.fecha = fecha;
            this.materia = materia;
            this.profesorId = profesorId;
            this.texto = texto;
        }
    }

    // ============================================================
    // MENSAJES
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
        layoutObservaciones.removeAllViews();
        TextView tvMensaje = new TextView(requireContext());
        tvMensaje.setText("ℹ️ No hay observaciones registradas para este estudiante");
        tvMensaje.setTextSize(16);
        tvMensaje.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        tvMensaje.setPadding(0, dpToPx(20), 0, 0);
        tvMensaje.setGravity(android.view.Gravity.CENTER);
        layoutObservaciones.addView(tvMensaje);
    }

    private void mostrarMensajeError() {
        layoutObservaciones.removeAllViews();
        TextView tvMensaje = new TextView(requireContext());
        tvMensaje.setText("❌ Error al cargar las observaciones");
        tvMensaje.setTextSize(16);
        tvMensaje.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        tvMensaje.setPadding(0, dpToPx(20), 0, 0);
        tvMensaje.setGravity(android.view.Gravity.CENTER);
        layoutObservaciones.addView(tvMensaje);
    }

    private void mostrarErrorEstado(String mensaje) {
        if (textViewEstado != null) {
            textViewEstado.setText(mensaje);
            textViewEstado.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            textViewEstado.setVisibility(View.VISIBLE);
        }
        spinnerHijos.setVisibility(View.GONE);
        textViewStudentName.setVisibility(View.GONE);
        scrollViewResultados.setVisibility(View.GONE);
    }

    // ============================================================
    // LIMPIAR
    // ============================================================
    private void limpiarResultados() {
        if (layoutObservaciones != null) layoutObservaciones.removeAllViews();
        if (textViewStudentName != null) textViewStudentName.setVisibility(View.GONE);
        if (scrollViewResultados != null) scrollViewResultados.setVisibility(View.GONE);
        if (textViewTotalObs != null) textViewTotalObs.setVisibility(View.GONE);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}