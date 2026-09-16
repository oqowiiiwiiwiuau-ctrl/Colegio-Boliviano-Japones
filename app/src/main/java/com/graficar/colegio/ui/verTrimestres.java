package com.graficar.colegio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.graficar.colegio.R;

public class verTrimestres extends AppCompatActivity {

    private static final String TAG = "VerTrimestres";

    // ============================================================
    // DATOS RECIBIDOS POR INTENT
    // ============================================================
    private String gradoActual;
    private String materiaActual;
    private String idLocalProfesor;
    private String nombreProfesorRecibido;

    // ============================================================
    // VISTAS
    // ============================================================
    private TextView tvCategoria;
    private TextView tvTitulo;
    private TextView tvSubtitulo;
    private TextView tvNombreMaestro;
    private TextView tvEspecialidadMaestro;
    private TextView tvCorreoMaestro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ver_trimestres);

        leerIntent();

        if (gradoActual == null || materiaActual == null) {
            Toast.makeText(this, "Faltan datos (grado o materia)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvCategoria = findViewById(R.id.tvCategoria);
        tvTitulo = findViewById(R.id.tvTitulo);
        tvSubtitulo = findViewById(R.id.tvSubtitulo);
        tvNombreMaestro = findViewById(R.id.tvNombreMaestro);
        tvEspecialidadMaestro = findViewById(R.id.tvEspecialidadMaestro);
        tvCorreoMaestro = findViewById(R.id.tvCorreoMaestro);

        actualizarEncabezado();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.headerContainer),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top,
                            systemBars.right, systemBars.bottom);
                    return insets;
                });

        cargarProfesorDesdeFirebase();

        // Los 3 trimestres apuntan a la MISMA Activity
        findViewById(R.id.cardPrimerTrimestre)
                .setOnClickListener(v -> abrirTrimestre("trimestre1"));

        findViewById(R.id.cardSegundoTrimestre)
                .setOnClickListener(v -> abrirTrimestre("trimestre2"));

        findViewById(R.id.cardTercerTrimestre)
                .setOnClickListener(v -> abrirTrimestre("trimestre3"));

        findViewById(R.id.btnCentralizador)
                .setOnClickListener(v -> abrirCentralizador());
    }

    private void leerIntent() {
        gradoActual = getIntent().getStringExtra("grado");
        materiaActual = getIntent().getStringExtra("materia");
        idLocalProfesor = getIntent().getStringExtra("idLocalProfesor");
        nombreProfesorRecibido = getIntent().getStringExtra("nombreProfesor");
    }

    private void actualizarEncabezado() {
        String emoji = obtenerEmojiMateria(materiaActual);
        tvCategoria.setText(emoji + "  " + materiaActual.toUpperCase());
        tvTitulo.setText(gradoActual);
        tvSubtitulo.setText("Gestión 2026  ·  Curso " + gradoActual);
    }

    private String obtenerEmojiMateria(String materia) {
        if (materia == null) return "📚";
        String m = materia.toLowerCase();
        if (m.contains("matem")) return "📐";
        if (m.contains("lengua") || m.contains("comunic")) return "📖";
        if (m.contains("ciencia")) return "🔬";
        if (m.contains("historia")) return "🏛";
        if (m.contains("fisica") || m.contains("física")) return "⚛";
        if (m.contains("quimica") || m.contains("química")) return "🧪";
        if (m.contains("ingles") || m.contains("inglés")) return "🇬🇧";
        if (m.contains("educacion") || m.contains("educación")) return "🏃";
        return "📚";
    }

    private void abrirTrimestre(String trimestre) {
        Intent intent = new Intent(verTrimestres.this, TrimestreActivity.class);
        intent.putExtra("grado", gradoActual);
        intent.putExtra("materia", materiaActual);
        intent.putExtra("trimestre", trimestre);
        intent.putExtra("idLocalProfesor", idLocalProfesor);
        intent.putExtra("nombreProfesor", nombreProfesorRecibido);
        startActivity(intent);
    }

    private void abrirCentralizador() {
        Intent intent = new Intent(verTrimestres.this, Centralizador.class);
        intent.putExtra("grado", gradoActual);
        intent.putExtra("materia", materiaActual);
        intent.putExtra("idLocalProfesor", idLocalProfesor);
        intent.putExtra("nombreProfesor", nombreProfesorRecibido);
        startActivity(intent);
    }

    private void cargarProfesorDesdeFirebase() {
        if (idLocalProfesor != null && !idLocalProfesor.isEmpty()) {
            cargarProfesorPorId(idLocalProfesor);
        } else {
            buscarProfesorPorMateriaYGrado();
        }
    }

    private void cargarProfesorPorId(String idLocal) {
        DatabaseReference profRef = FirebaseDatabase
                .getInstance()
                .getReference("profesores")
                .child(idLocal);

        profRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvNombreMaestro.setText("Profesor no encontrado");
                    return;
                }

                String nombre = snapshot.child("nombre").getValue(String.class);
                String correo = snapshot.child("email").getValue(String.class);
                String especialidad = extraerEspecialidad(snapshot);

                mostrarProfesor(nombre, especialidad, correo);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al leer profesor: " + error.getMessage());
                tvNombreMaestro.setText("Error al cargar");
            }
        });
    }

    private String extraerEspecialidad(DataSnapshot snapshot) {
        DataSnapshot espSnap = snapshot.child("especialidad");
        if (!espSnap.exists()) return "";

        if (espSnap.getValue() instanceof String) {
            return espSnap.getValue(String.class);
        }

        StringBuilder sb = new StringBuilder();
        for (DataSnapshot e : espSnap.getChildren()) {
            String esp = e.getValue(String.class);
            if (esp != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(esp);
            }
        }
        return sb.toString();
    }

    private void buscarProfesorPorMateriaYGrado() {
        DatabaseReference profesoresRef = FirebaseDatabase
                .getInstance()
                .getReference("profesores");

        profesoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nombre = null;
                String especialidad = null;
                String correo = null;

                for (DataSnapshot profSnapshot : snapshot.getChildren()) {
                    boolean imparteMateria = false;

                    for (DataSnapshot asig : profSnapshot.child("asignaciones").getChildren()) {
                        String grado = asig.child("grado").getValue(String.class);
                        String materia = asig.child("materia").getValue(String.class);

                        if (grado != null && materia != null
                                && grado.equalsIgnoreCase(gradoActual)
                                && materia.equalsIgnoreCase(materiaActual)) {
                            imparteMateria = true;
                            break;
                        }
                    }

                    if (imparteMateria) {
                        nombre = profSnapshot.child("nombre").getValue(String.class);
                        correo = profSnapshot.child("email").getValue(String.class);
                        especialidad = extraerEspecialidad(profSnapshot);
                        break;
                    }
                }

                mostrarProfesor(nombre, especialidad, correo);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error: " + error.getMessage());
            }
        });
    }

    private void mostrarProfesor(String nombre, String especialidad, String correo) {
        if (nombre != null) {
            tvNombreMaestro.setText(nombre);
            tvEspecialidadMaestro.setText(
                    especialidad != null && !especialidad.isEmpty()
                            ? "Especialidad: " + especialidad
                            : "Especialidad: —");
            tvCorreoMaestro.setText(
                    correo != null && !correo.isEmpty() ? "✉  " + correo : "");
        } else {
            tvNombreMaestro.setText("Profesor no asignado");
            tvEspecialidadMaestro.setText("");
            tvCorreoMaestro.setText("");
        }
    }
}