package com.graficar.colegio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.graficar.colegio.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MateriasActivity extends AppCompatActivity {

    private static final String TAG = "MateriasActivity";

    // ============================================================
    // UI
    // ============================================================
    private TextView tvTitulo;
    private TextView tvSubtitulo;
    private TextView tvEstado;
    private RecyclerView recyclerViewParalelos;

    // ============================================================
    // FIREBASE
    // ============================================================
    private DatabaseReference dbRef;

    // ============================================================
    // DATOS RECIBIDOS
    // ============================================================
    private String gradoBase;
    private String nombreProfesor;
    private String idLocalProfesor;

    // ============================================================
    // LISTA DE PARALELOS
    // ============================================================
    private final List<Paralelo> listaParalelos = new ArrayList<>();
    private ParaleloAdapter paraleloAdapter;

    // ============================================================
    // ON CREATE
    // ============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materias);

        // Vistas
        tvTitulo = findViewById(R.id.tvTituloMaterias);
        tvSubtitulo = findViewById(R.id.tvSubtituloMaterias);
        tvEstado = findViewById(R.id.tvEstadoMaterias);
        recyclerViewParalelos = findViewById(R.id.recyclerViewParalelos);

        // RecyclerView
        recyclerViewParalelos.setLayoutManager(new LinearLayoutManager(this));
        paraleloAdapter = new ParaleloAdapter(listaParalelos);
        recyclerViewParalelos.setAdapter(paraleloAdapter);

        // Firebase
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Leer datos del Intent
        leerIntent();

        // Cargar paralelos y materias del profesor
        cargarAsignaciones();
    }

    // ============================================================
    // LEER DATOS DEL INTENT
    // ============================================================
    private void leerIntent() {
        String cursoNombre = getIntent().getStringExtra("curso_nombre");
        nombreProfesor = getIntent().getStringExtra("nombreProfesor");
        idLocalProfesor = getIntent().getStringExtra("idLocalProfesor");

        // Extraer solo la palabra "Primero" (sin la letra del paralelo)
        if (cursoNombre != null) {
            String[] partes = cursoNombre.trim().split("\\s+");
            gradoBase = partes.length > 0 ? partes[0] : cursoNombre;
        } else {
            gradoBase = "Primero";
        }

        tvTitulo.setText("📚 " + gradoBase);
        if (nombreProfesor != null) {
            tvSubtitulo.setText("Prof. " + nombreProfesor);
        }

        Log.d(TAG, "Grado base: " + gradoBase);
        Log.d(TAG, "Profesor: " + nombreProfesor + " (" + idLocalProfesor + ")");
    }

    // ============================================================
    // CARGAR ASIGNACIONES DEL PROFESOR
    // ============================================================
    private void cargarAsignaciones() {
        if (idLocalProfesor == null) {
            mostrarError("No se recibió el profesor");
            return;
        }

        dbRef.child("profesores").child(idLocalProfesor).child("asignaciones")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaParalelos.clear();

                        if (!snapshot.exists()) {
                            mostrarError("No tienes asignaciones registradas");
                            return;
                        }

                        // Map<paralelo, List<materia>>
                        Map<String, List<String>> paralelosConMaterias = new HashMap<>();

                        for (DataSnapshot asig : snapshot.getChildren()) {
                            String grado = asig.child("grado").getValue(String.class);
                            String materia = asig.child("materia").getValue(String.class);

                            if (grado == null || materia == null) continue;

                            // Filtrar por grado base ("Primero")
                            if (!grado.toLowerCase().startsWith(gradoBase.toLowerCase())) {
                                continue;
                            }

                            paralelosConMaterias
                                    .computeIfAbsent(grado, k -> new ArrayList<>())
                                    .add(materia);
                        }

                        if (paralelosConMaterias.isEmpty()) {
                            mostrarError("No tienes paralelos en " + gradoBase);
                            return;
                        }

                        // Construir lista de paralelos
                        for (Map.Entry<String, List<String>> entry : paralelosConMaterias.entrySet()) {
                            String paralelo = entry.getKey();
                            List<String> materias = entry.getValue();
                            listaParalelos.add(new Paralelo(paralelo, materias));
                        }

                        // Ordenar por nombre
                        listaParalelos.sort((a, b) -> a.getNombre().compareTo(b.getNombre()));

                        paraleloAdapter.notifyDataSetChanged();

                        tvEstado.setVisibility(View.GONE);
                        recyclerViewParalelos.setVisibility(View.VISIBLE);

                        // Cargar cantidad de estudiantes
                        cargarCantidadEstudiantes();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        mostrarError("Error: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // CARGAR CANTIDAD DE ESTUDIANTES POR PARALELO
    // ============================================================
    private void cargarCantidadEstudiantes() {
        dbRef.child("estudiantesPorGrado")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (Paralelo paralelo : listaParalelos) {
                            String key = normalizarGradoKey(paralelo.getNombre());
                            DataSnapshot paraleloSnap = snapshot.child(key);

                            if (paraleloSnap.exists()) {
                                paralelo.setCantidadEstudiantes((int) paraleloSnap.getChildrenCount());
                            }
                        }
                        paraleloAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error estudiantes: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // NORMALIZAR NOMBRE: "Primero A" → "primeroA"
    // ============================================================
    private String normalizarGradoKey(String grado) {
        if (grado == null) return "";
        String[] partes = grado.trim().split("\\s+");
        if (partes.length < 2) return grado.toLowerCase();
        return partes[0].toLowerCase() + partes[1].toUpperCase();
    }

    // ============================================================
    // MOSTRAR ERROR
    // ============================================================
    private void mostrarError(String mensaje) {
        tvEstado.setText(mensaje);
        tvEstado.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        tvEstado.setVisibility(View.VISIBLE);
        recyclerViewParalelos.setVisibility(View.GONE);
    }

    // ============================================================
    // CLASE PARALELO
    // ============================================================
    public static class Paralelo {
        private final String nombre;
        private final List<String> materias;
        private int cantidadEstudiantes;

        public Paralelo(String nombre, List<String> materias) {
            this.nombre = nombre;
            this.materias = materias;
            this.cantidadEstudiantes = 0;
        }

        public String getNombre() { return nombre; }
        public List<String> getMaterias() { return materias; }
        public int getCantidadEstudiantes() { return cantidadEstudiantes; }

        public void setCantidadEstudiantes(int cantidad) {
            this.cantidadEstudiantes = cantidad;
        }
    }

    // ============================================================
    // ADAPTER
    // ============================================================
    private class ParaleloAdapter extends RecyclerView.Adapter<ParaleloAdapter.ParaleloViewHolder> {

        private final List<Paralelo> paralelos;

        public ParaleloAdapter(List<Paralelo> paralelos) {
            this.paralelos = paralelos;
        }

        @NonNull
        @Override
        public ParaleloViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_paralelo, parent, false);
            return new ParaleloViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ParaleloViewHolder holder, int position) {
            holder.bind(paralelos.get(position));
        }

        @Override
        public int getItemCount() {
            return paralelos.size();
        }

        class ParaleloViewHolder extends RecyclerView.ViewHolder {
            private final CardView cardView;
            private final TextView tvNombreParalelo;
            private final TextView tvCantidadEstudiantes;
            private final android.widget.LinearLayout layoutMaterias;

            public ParaleloViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.cardViewParalelo);
                tvNombreParalelo = itemView.findViewById(R.id.tvNombreParalelo);
                tvCantidadEstudiantes = itemView.findViewById(R.id.tvCantidadEstudiantes);
                layoutMaterias = itemView.findViewById(R.id.layoutMaterias);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        abrirParalelo(paralelos.get(position));
                    }
                });
            }

            public void bind(Paralelo paralelo) {
                tvNombreParalelo.setText(paralelo.getNombre());
                tvCantidadEstudiantes.setText(paralelo.getCantidadEstudiantes() + " estudiantes");

                // Mostrar materias como chips
                layoutMaterias.removeAllViews();

                for (String materia : paralelo.getMaterias()) {
                    TextView chip = new TextView(itemView.getContext());
                    chip.setText("📚 " + capitalizar(materia));

                    chip.setTextSize(12);
                    chip.setTextColor(itemView.getContext()
                            .getResources().getColor(android.R.color.white));
                    chip.setBackgroundColor(itemView.getContext()
                            .getResources().getColor(R.color.color_secundario));

                    int padH = dpToPx(10);
                    int padV = dpToPx(4);
                    chip.setPadding(padH, padV, padH, padV);

                    android.widget.LinearLayout.LayoutParams params =
                            new android.widget.LinearLayout.LayoutParams(
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, dpToPx(6), dpToPx(6), 0);
                    chip.setLayoutParams(params);

                    layoutMaterias.addView(chip);
                }
            }
        }
    }

    // ============================================================
    // ABRIR PARALELO
    // ============================================================
    private void abrirParalelo(Paralelo paralelo) {
        if (paralelo.getMaterias().size() == 1) {
            // Una sola materia → ir directo
            abrirVerTrimestres(paralelo.getNombre(), paralelo.getMaterias().get(0));
        } else {
            // Varias materias → mostrar diálogo
            mostrarDialogoMaterias(paralelo);
        }
    }

    private void mostrarDialogoMaterias(Paralelo paralelo) {
        List<String> materias = paralelo.getMaterias();
        String[] opciones = new String[materias.size()];
        for (int i = 0; i < materias.size(); i++) {
            opciones[i] = capitalizar(materias.get(i));
        }

        new AlertDialog.Builder(this)
                .setTitle("Materias en " + paralelo.getNombre())
                .setItems(opciones, (dialog, which) -> {
                    abrirVerTrimestres(paralelo.getNombre(), materias.get(which));
                })
                .show();
    }

    private void abrirVerTrimestres(String grado, String materia) {
        Intent intent = new Intent(MateriasActivity.this, verTrimestres.class);
        intent.putExtra("grado", grado);
        intent.putExtra("materia", materia);
        intent.putExtra("idLocalProfesor", idLocalProfesor);
        intent.putExtra("nombreProfesor", nombreProfesor);
        startActivity(intent);
    }

    // ============================================================
    // UTILIDADES
    // ============================================================
    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}