package com.graficar.colegio.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.graficar.colegio.R;

import java.util.ArrayList;
import java.util.List;

public class PrimeroActivity extends AppCompatActivity {
    private TableLayout tablaNotas;
    private List<Estudiante> listaEstudiantes;
    private DatabaseReference databaseReference;

    // Probemos con diferentes variantes del nombre
    private String[] posiblesCursos = {"Matemática", "Matematica", "matemática", "matematica"};
    private String cursoActual = "Matemática"; // Empezamos con esta

    private String[] encabezados = {
            "N°",
            "ESTUDIANTE",
            "PRÁCTICAS\nB1",
            "FINAL\nB1",
            "PRÁCTICAS\nB2",
            "FINAL\nB2",
            "PRÁCTICAS\nB3",
            "FINAL\nB3",
            "PRÁCTICAS\nB4",
            "FINAL\nB4",
            "PROMEDIO"
    };

    private int[] anchosColumnas = {
            30,     // N°
            130,    // ESTUDIANTE
            100,    // PRÁCTICAS B1
            100,    // FINAL B1
            100,    // PRÁCTICAS B2
            100,    // FINAL B2
            100,    // PRÁCTICAS B3
            100,    // FINAL B3
            100,    // PRÁCTICAS B4
            100,    // FINAL B4
            110     // PROMEDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primero);

        tablaNotas = findViewById(R.id.tablaNotas);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Verificar que la tabla existe
        if (tablaNotas == null) {
            Log.e("ERROR", "tablaNotas es null");
            Toast.makeText(this, "Error: No se encontró la tabla", Toast.LENGTH_LONG).show();
            return;
        }

        // Inicializar Firebase
        try {
            databaseReference = FirebaseDatabase.getInstance().getReference("alumnos");
            Log.d("Firebase", "Referencia creada: " + databaseReference.toString());
        } catch (Exception e) {
            Log.e("Firebase", "Error al inicializar Firebase: " + e.getMessage());
            Toast.makeText(this, "Error al inicializar Firebase", Toast.LENGTH_LONG).show();
            return;
        }

        // Mostrar mensaje de carga
        Toast.makeText(this, "Cargando datos...", Toast.LENGTH_SHORT).show();

        // Cargar datos desde Firebase
        cargarDatosDesdeFirebase();
    }

    private void cargarDatosDesdeFirebase() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("Firebase", "=== DATOS RECIBIDOS DE FIREBASE ===");
                Log.d("Firebase", "Snapshot existe: " + (snapshot != null));
                Log.d("Firebase", "Key del snapshot: " + snapshot.getKey());
                Log.d("Firebase", "Children count: " + snapshot.getChildrenCount());

                // Mostrar toda la estructura para depuración
                Log.d("Firebase", "=== ESTRUCTURA COMPLETA ===");
                for (DataSnapshot alumno : snapshot.getChildren()) {
                    Log.d("Firebase", "Alumno: " + alumno.getKey());
                    for (DataSnapshot curso : alumno.getChildren()) {
                        Log.d("Firebase", "  Curso: " + curso.getKey());
                        for (DataSnapshot bimestre : curso.getChildren()) {
                            Log.d("Firebase", "    Bimestre: " + bimestre.getKey());
                            Log.d("Firebase", "      practicas: " + bimestre.child("practicas").getValue());
                            Log.d("Firebase", "      final: " + bimestre.child("final").getValue());
                            Log.d("Firebase", "      promedio: " + bimestre.child("promedio").getValue());
                        }
                    }
                }

                listaEstudiantes = new ArrayList<>();
                int contador = 1;

                for (DataSnapshot alumnoSnapshot : snapshot.getChildren()) {
                    String nombreAlumno = alumnoSnapshot.getKey();
                    Log.d("Firebase", "Procesando alumno: " + nombreAlumno);

                    // Buscar en TODOS los cursos para ver qué hay disponible
                    boolean encontrado = false;
                    for (DataSnapshot cursoSnapshot : alumnoSnapshot.getChildren()) {
                        String nombreCurso = cursoSnapshot.getKey();
                        Log.d("Firebase", "  Curso encontrado: '" + nombreCurso + "'");

                        // Comparar con nuestro curso actual (ignorando mayúsculas/minúsculas y acentos)
                        if (nombreCurso != null && nombreCurso.equalsIgnoreCase(cursoActual)) {
                            Log.d("Firebase", "  ¡COINCIDENCIA ENCONTRADA! Procesando curso: " + nombreCurso);
                            encontrado = true;

                            Estudiante estudiante = new Estudiante(contador, nombreAlumno);

                            // Cargar datos de cada bimestre
                            for (int i = 1; i <= 4; i++) {
                                String bimestre = "B" + i;
                                DataSnapshot bimestreSnapshot = cursoSnapshot.child(bimestre);

                                if (bimestreSnapshot.exists()) {
                                    String practicas = bimestreSnapshot.child("practicas").getValue(String.class);
                                    String final_ = bimestreSnapshot.child("final").getValue(String.class);

                                    Log.d("Firebase", "    " + bimestre + " - Prácticas: " + practicas + ", Final: " + final_);

                                    estudiante.setNotasBimestre(i, practicas, final_);
                                }
                            }

                            listaEstudiantes.add(estudiante);
                            contador++;
                            break; // Salir del bucle de cursos
                        }
                    }

                    if (!encontrado) {
                        Log.d("Firebase", "  No se encontró el curso '" + cursoActual + "' para este alumno");
                    }
                }

                Log.d("Firebase", "Total estudiantes cargados: " + listaEstudiantes.size());

                // Crear la tabla con los datos reales
                runOnUiThread(() -> {
                    crearTablaNotas();

                    if (listaEstudiantes.isEmpty()) {
                        Toast.makeText(PrimeroActivity.this,
                                "No se encontraron datos para '" + cursoActual + "'.\nRevisa los logs para ver los cursos disponibles.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(PrimeroActivity.this,
                                "Se cargaron " + listaEstudiantes.size() + " estudiantes de " + cursoActual,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error al cargar datos: " + error.getMessage());
                Log.e("Firebase", "Detalles: " + error.getDetails());

                runOnUiThread(() -> {
                    Toast.makeText(PrimeroActivity.this,
                            "Error al cargar datos: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();

                    // Mostrar mensaje de error en la tabla
                    TextView tvError = new TextView(PrimeroActivity.this);
                    tvError.setText("Error de conexión con Firebase");
                    tvError.setTextSize(18);
                    tvError.setGravity(Gravity.CENTER);
                    tvError.setPadding(20, 50, 20, 50);
                    tablaNotas.addView(tvError);
                });
            }
        });
    }

    private void crearTablaNotas() {
        tablaNotas.removeAllViews();

        if (listaEstudiantes == null || listaEstudiantes.isEmpty()) {
            Log.d("UI", "No hay estudiantes para mostrar");

            // Mostrar mensaje de que no hay datos
            TextView tvMensaje = new TextView(this);
            tvMensaje.setText("No hay datos disponibles para '" + cursoActual + "'\nVerifica los logs para ver los cursos disponibles");
            tvMensaje.setTextSize(16);
            tvMensaje.setGravity(Gravity.CENTER);
            tvMensaje.setPadding(20, 50, 20, 50);
            tvMensaje.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            tablaNotas.addView(tvMensaje);
            return;
        }

        Log.d("UI", "Creando tabla con " + listaEstudiantes.size() + " estudiantes");

        crearFilaEncabezados();

        for (Estudiante estudiante : listaEstudiantes) {
            crearFilaEstudiante(estudiante);
        }

        crearFilaTotales();
    }

    private void crearFilaEncabezados() {
        TableRow filaEncabezado = new TableRow(this);
        filaEncabezado.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));

        for (int i = 0; i < encabezados.length; i++) {
            TextView textView = new TextView(this);
            textView.setText(encabezados[i]);
            textView.setPadding(dpToPx(10), dpToPx(12), dpToPx(10), dpToPx(12));
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            textView.setTextSize(14);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
            textView.setGravity(Gravity.CENTER);

            if (encabezados[i].contains("\n")) {
                textView.setSingleLine(false);
                textView.setMaxLines(2);
            } else {
                textView.setSingleLine(true);
            }

            TableRow.LayoutParams tvParams = crearParamsColumna(anchosColumnas[i]);
            tvParams.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
            textView.setLayoutParams(tvParams);

            filaEncabezado.addView(textView);
        }

        tablaNotas.addView(filaEncabezado);
    }

    private void crearFilaEstudiante(Estudiante estudiante) {
        TableRow fila = new TableRow(this);

        int colorFondo = estudiante.getNumero() % 2 == 0
                ? ContextCompat.getColor(this, R.color.gris_claro)
                : ContextCompat.getColor(this, android.R.color.white);
        fila.setBackgroundColor(colorFondo);

        // Columna 1: Número
        TextView tvNumero = crearTextView(String.valueOf(estudiante.getNumero()));
        tvNumero.setLayoutParams(crearParamsColumna(anchosColumnas[0]));
        fila.addView(tvNumero);

        // Columna 2: Nombre
        TextView tvNombre = crearTextView(estudiante.getNombre());
        tvNombre.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tvNombre.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        tvNombre.setLayoutParams(crearParamsColumna(anchosColumnas[1]));
        fila.addView(tvNombre);

        // Columnas 3-10: Notas de los 4 bimestres
        for (int i = 0; i < 8; i++) {
            double nota = estudiante.getNotaPorIndice(i);
            String textoNota = (nota == -1) ? "--" : String.valueOf((int)nota);
            EditText editText = crearEditText(textoNota);
            editText.setLayoutParams(crearParamsColumna(anchosColumnas[i + 2]));
            fila.addView(editText);
        }

        // Columna 11: Promedio
        TextView tvPromedio = crearTextView(String.format("%.1f", estudiante.calcularPromedio()));
        tvPromedio.setTextColor(ContextCompat.getColor(this, R.color.green));
        tvPromedio.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPromedio.setLayoutParams(crearParamsColumna(anchosColumnas[10]));
        fila.addView(tvPromedio);

        tablaNotas.addView(fila);
    }

    private void crearFilaTotales() {
        TableRow filaTotales = new TableRow(this);
        filaTotales.setBackgroundColor(ContextCompat.getColor(this, R.color.color_secundario));

        // Celda 1: "TOTALES"
        TextView tvTotales = crearTextView("TOTALES");
        tvTotales.setLayoutParams(crearParamsColumna(anchosColumnas[0]));
        filaTotales.addView(tvTotales);

        // Celda 2: Vacía
        TextView tvVacia = crearTextView("");
        tvVacia.setLayoutParams(crearParamsColumna(anchosColumnas[1]));
        filaTotales.addView(tvVacia);

        // Calcular promedios por columna
        double[] promediosColumnas = new double[8];
        int[] contadores = new int[8];

        for (Estudiante estudiante : listaEstudiantes) {
            for (int i = 0; i < 8; i++) {
                double nota = estudiante.getNotaPorIndice(i);
                if (nota != -1) {
                    promediosColumnas[i] += nota;
                    contadores[i]++;
                }
            }
        }

        // Celdas 3-10: Promedios por columna
        for (int i = 0; i < 8; i++) {
            double promedio = contadores[i] > 0 ? promediosColumnas[i] / contadores[i] : 0;
            TextView tvPromColumna = crearTextView(String.format("%.1f", promedio));
            tvPromColumna.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvPromColumna.setTypeface(null, android.graphics.Typeface.BOLD);
            tvPromColumna.setLayoutParams(crearParamsColumna(anchosColumnas[i + 2]));
            filaTotales.addView(tvPromColumna);
        }

        // Celda 11: Promedio general
        double promedioGeneral = 0;
        int totalEstudiantesConNotas = 0;
        for (Estudiante estudiante : listaEstudiantes) {
            double promEstudiante = estudiante.calcularPromedio();
            if (promEstudiante > 0) {
                promedioGeneral += promEstudiante;
                totalEstudiantesConNotas++;
            }
        }
        promedioGeneral = totalEstudiantesConNotas > 0 ? promedioGeneral / totalEstudiantesConNotas : 0;

        TextView tvPromGeneral = crearTextView(String.format("%.1f", promedioGeneral));
        tvPromGeneral.setTextColor(ContextCompat.getColor(this, R.color.color_accent));
        tvPromGeneral.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPromGeneral.setLayoutParams(crearParamsColumna(anchosColumnas[10]));
        filaTotales.addView(tvPromGeneral);

        tablaNotas.addView(filaTotales);
    }

    private TextView crearTextView(String texto) {
        TextView textView = new TextView(this);
        textView.setText(texto);
        textView.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));
        textView.setTextSize(13);
        textView.setGravity(Gravity.CENTER);
        textView.setSingleLine(true);
        return textView;
    }

    private EditText crearEditText(String texto) {
        EditText editText = new EditText(this);
        editText.setText(texto);
        editText.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));
        editText.setTextSize(13);
        editText.setGravity(Gravity.CENTER);
        editText.setBackgroundResource(android.R.drawable.edit_text);
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setSingleLine(true);
        editText.setEnabled(false);
        return editText;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private TableRow.LayoutParams crearParamsColumna(int anchoDp) {
        return new TableRow.LayoutParams(
                dpToPx(anchoDp),
                TableRow.LayoutParams.WRAP_CONTENT
        );
    }

    private static class Estudiante {
        private int numero;
        private String nombre;
        private double[] notas = new double[8];

        public Estudiante(int numero, String nombre) {
            this.numero = numero;
            this.nombre = nombre;
            for (int i = 0; i < notas.length; i++) {
                notas[i] = -1;
            }
        }

        public void setNotasBimestre(int bimestre, String practicas, String final_) {
            int baseIndex = (bimestre - 1) * 2;

            if (practicas != null && !practicas.equals("--")) {
                try {
                    notas[baseIndex] = Double.parseDouble(practicas);
                } catch (NumberFormatException e) {
                    notas[baseIndex] = -1;
                }
            }

            if (final_ != null && !final_.equals("--")) {
                try {
                    notas[baseIndex + 1] = Double.parseDouble(final_);
                } catch (NumberFormatException e) {
                    notas[baseIndex + 1] = -1;
                }
            }
        }

        public double getNotaPorIndice(int indice) {
            if (indice >= 0 && indice < notas.length) {
                return notas[indice];
            }
            return -1;
        }

        public int getNumero() { return numero; }
        public String getNombre() { return nombre; }

        public double calcularPromedio() {
            double suma = 0;
            int conteo = 0;

            for (double nota : notas) {
                if (nota != -1) {
                    suma += nota;
                    conteo++;
                }
            }

            return conteo > 0 ? suma / conteo : 0.0;
        }
    }
}