package com.graficar.colegio.ui;

import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrimeroActivity extends AppCompatActivity {
    private TableLayout tablaNotas;
    private List<Estudiante> listaEstudiantes;
    private DatabaseReference databaseReference;
    private Map<String, Map<String, Object>> cambiosPendientes = new HashMap<>();
    private TextView tvTituloCurso;
    private View btnGuardar;

    private String cursoActual = "Matemática";

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
            40,     // N° (aumentado un poco)
            160,    // ESTUDIANTE (aumentado)
            90,     // PRÁCTICAS B1
            90,     // FINAL B1
            90,     // PRÁCTICAS B2
            90,     // FINAL B2
            90,     // PRÁCTICAS B3
            90,     // FINAL B3
            90,     // PRÁCTICAS B4
            90,     // FINAL B4
            100     // PROMEDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primero);

        tablaNotas = findViewById(R.id.tablaNotas);
        tvTituloCurso = findViewById(R.id.tvTituloCurso);

        // Botón de guardar (puedes agregar un botón en tu layout o usar el de back con otra función)
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            guardarTodosLosCambios();
            finish();
        });

        // Agregar botón de guardar en el header (opcional - si quieres un botón específico)
        // Si no tienes un botón de guardar, puedes usar un long press en el título
        tvTituloCurso.setOnLongClickListener(v -> {
            guardarTodosLosCambios();
            return true;
        });

        // Inicializar Firebase
        try {
            databaseReference = FirebaseDatabase.getInstance().getReference("alumnos");
            Log.d("Firebase", "Referencia creada: " + databaseReference);
        } catch (Exception e) {
            Log.e("Firebase", "Error al inicializar Firebase: " + e.getMessage());
            Toast.makeText(this, "Error al inicializar Firebase", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Cargando datos...", Toast.LENGTH_SHORT).show();
        cargarDatosDesdeFirebase();
    }

    private void cargarDatosDesdeFirebase() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaEstudiantes = new ArrayList<>();
                int contador = 1;

                Log.d("Firebase", "Total alumnos encontrados: " + snapshot.getChildrenCount());

                // MOSTRAR TODOS LOS ESTUDIANTES, tengan o no la materia
                for (DataSnapshot alumnoSnapshot : snapshot.getChildren()) {
                    String nombreAlumno = alumnoSnapshot.getKey();
                    Log.d("Firebase", "Procesando alumno: " + nombreAlumno);

                    Estudiante estudiante = new Estudiante(contador, nombreAlumno);

                    // Buscar si tiene la materia actual
                    DataSnapshot cursoSnapshot = alumnoSnapshot.child(cursoActual);

                    if (cursoSnapshot.exists()) {
                        Log.d("Firebase", "  ✓ " + nombreAlumno + " tiene " + cursoActual);

                        // Cargar datos de cada bimestre si existen
                        for (int i = 1; i <= 4; i++) {
                            String bimestre = "B" + i;
                            DataSnapshot bimestreSnapshot = cursoSnapshot.child(bimestre);

                            if (bimestreSnapshot.exists()) {
                                String practicas = bimestreSnapshot.child("practicas").getValue(String.class);
                                String final_ = bimestreSnapshot.child("final").getValue(String.class);

                                estudiante.setNotasBimestre(i, practicas, final_);
                            }
                        }
                    } else {
                        Log.d("Firebase", "  ✗ " + nombreAlumno + " NO tiene " + cursoActual + " (se mostrará con --)");
                        // El estudiante ya tiene valores por defecto "--"
                    }

                    listaEstudiantes.add(estudiante);
                    contador++;
                }

                Log.d("Firebase", "Total estudiantes en lista: " + listaEstudiantes.size());

                runOnUiThread(() -> {
                    tvTituloCurso.setText(cursoActual + " - " + listaEstudiantes.size() + " alumnos");
                    crearTablaNotas();

                    Toast.makeText(PrimeroActivity.this,
                            "Se cargaron " + listaEstudiantes.size() + " estudiantes",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error al cargar datos: " + error.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(PrimeroActivity.this,
                            "Error al cargar datos: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void crearTablaNotas() {
        tablaNotas.removeAllViews();

        if (listaEstudiantes == null || listaEstudiantes.isEmpty()) {
            TextView tvMensaje = new TextView(this);
            tvMensaje.setText("No hay estudiantes para mostrar");
            tvMensaje.setTextSize(16);
            tvMensaje.setGravity(Gravity.CENTER);
            tvMensaje.setPadding(20, 50, 20, 50);
            tablaNotas.addView(tvMensaje);
            return;
        }

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
            textView.setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10));
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            textView.setTextSize(13);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
            textView.setGravity(Gravity.CENTER);

            if (encabezados[i].contains("\n")) {
                textView.setSingleLine(false);
                textView.setMaxLines(2);
            }

            TableRow.LayoutParams params = new TableRow.LayoutParams(
                    dpToPx(anchosColumnas[i]),
                    dpToPx(50)
            );
            params.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
            textView.setLayoutParams(params);

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
        tvNumero.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[0]), dpToPx(45)));
        fila.addView(tvNumero);

        // Columna 2: Nombre
        TextView tvNombre = crearTextView(estudiante.getNombre());
        tvNombre.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tvNombre.setPadding(dpToPx(10), 0, dpToPx(10), 0);
        tvNombre.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[1]), dpToPx(45)));
        fila.addView(tvNombre);

        // Columnas 3-10: Notas de los 4 bimestres (EDITABLES AHORA)
        for (int i = 0; i < 8; i++) {
            String textoNota = estudiante.getNotaString(i);
            int bimestre = (i / 2) + 1;
            String tipo = (i % 2 == 0) ? "practicas" : "final";

            EditText editText = crearEditText(textoNota, estudiante, bimestre, tipo);
            editText.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[i + 2]), dpToPx(45)));
            fila.addView(editText);
        }

        // Columna 11: Promedio
        TextView tvPromedio = crearTextView(String.format("%.1f", estudiante.calcularPromedio()));
        tvPromedio.setTextColor(ContextCompat.getColor(this, R.color.green));
        tvPromedio.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPromedio.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[10]), dpToPx(45)));
        fila.addView(tvPromedio);

        tablaNotas.addView(fila);
    }

    private void crearFilaTotales() {
        TableRow filaTotales = new TableRow(this);
        filaTotales.setBackgroundColor(ContextCompat.getColor(this, R.color.color_secundario));

        // Celda 1: "TOTALES"
        TextView tvTotales = crearTextView("TOTALES");
        tvTotales.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[0]), dpToPx(45)));
        filaTotales.addView(tvTotales);

        // Celda 2: Vacía
        TextView tvVacia = crearTextView("");
        tvVacia.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[1]), dpToPx(45)));
        filaTotales.addView(tvVacia);

        // Calcular promedios por columna
        double[] promediosColumnas = new double[8];
        int[] contadores = new int[8];

        for (Estudiante estudiante : listaEstudiantes) {
            for (int i = 0; i < 8; i++) {
                double nota = estudiante.getNotaValor(i);
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
            tvPromColumna.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[i + 2]), dpToPx(45)));
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
        tvPromGeneral.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[10]), dpToPx(45)));
        filaTotales.addView(tvPromGeneral);

        tablaNotas.addView(filaTotales);
    }

    private TextView crearTextView(String texto) {
        TextView textView = new TextView(this);
        textView.setText(texto);
        textView.setPadding(dpToPx(6), dpToPx(8), dpToPx(6), dpToPx(8));
        textView.setTextSize(12);
        textView.setGravity(Gravity.CENTER);
        textView.setSingleLine(true);
        return textView;
    }

    private EditText crearEditText(String texto, Estudiante estudiante, int bimestre, String tipo) {
        EditText editText = new EditText(this);
        editText.setText(texto);
        editText.setPadding(dpToPx(6), dpToPx(8), dpToPx(6), dpToPx(8));
        editText.setTextSize(12);
        editText.setGravity(Gravity.CENTER);
        editText.setBackgroundResource(android.R.drawable.edit_text);
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setSingleLine(true);
        editText.setEnabled(true); // ¡HABILITADO PARA EDICIÓN!

        // Agregar listener para guardar cambios
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String nuevoValor = s.toString().trim();
                if (nuevoValor.isEmpty()) {
                    nuevoValor = "--";
                }

                // Guardar el cambio pendiente
                String key = estudiante.getNombre() + "_" + cursoActual + "_B" + bimestre + "_" + tipo;
                Map<String, Object> cambio = new HashMap<>();
                cambio.put("valor", nuevoValor);
                cambio.put("estudiante", estudiante.getNombre());
                cambio.put("bimestre", bimestre);
                cambio.put("tipo", tipo);
                cambiosPendientes.put(key, cambio);

                // Actualizar el objeto Estudiante localmente
                if (tipo.equals("practicas")) {
                    estudiante.setNotaPracticas(bimestre, nuevoValor);
                } else {
                    estudiante.setNotaFinal(bimestre, nuevoValor);
                }

                // Opcional: indicar que hay cambios sin guardar
                tvTituloCurso.setText(cursoActual + " * " + listaEstudiantes.size() + " alumnos");
            }
        });

        return editText;
    }

    private void guardarTodosLosCambios() {
        if (cambiosPendientes.isEmpty()) {
            Toast.makeText(this, "No hay cambios para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Guardando cambios...", Toast.LENGTH_SHORT).show();

        for (Map.Entry<String, Map<String, Object>> entry : cambiosPendientes.entrySet()) {
            Map<String, Object> cambio = entry.getValue();
            String nombreEstudiante = (String) cambio.get("estudiante");
            int bimestre = (int) cambio.get("bimestre");
            String tipo = (String) cambio.get("tipo");
            String valor = (String) cambio.get("valor");

            // Referencia: alumnos/[nombre]/[curso]/B[bimestre]/[tipo]
            DatabaseReference ref = databaseReference
                    .child(nombreEstudiante)
                    .child(cursoActual)
                    .child("B" + bimestre)
                    .child(tipo);

            ref.setValue(valor)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase", "Guardado: " + nombreEstudiante + " B" + bimestre + " " + tipo + " = " + valor);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Error al guardar: " + e.getMessage());
                        Toast.makeText(PrimeroActivity.this,
                                "Error al guardar: " + nombreEstudiante,
                                Toast.LENGTH_SHORT).show();
                    });
        }

        // Limpiar cambios pendientes y actualizar título
        cambiosPendientes.clear();
        tvTituloCurso.setText(cursoActual + " - " + listaEstudiantes.size() + " alumnos");
        Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class Estudiante {
        private int numero;
        private String nombre;
        private String[] notas = new String[8]; // Guardamos como String para mantener "--"

        public Estudiante(int numero, String nombre) {
            this.numero = numero;
            this.nombre = nombre;
            for (int i = 0; i < notas.length; i++) {
                notas[i] = "--";
            }
        }

        public void setNotasBimestre(int bimestre, String practicas, String final_) {
            int baseIndex = (bimestre - 1) * 2;
            if (practicas != null) notas[baseIndex] = practicas;
            if (final_ != null) notas[baseIndex + 1] = final_;
        }

        public void setNotaPracticas(int bimestre, String valor) {
            int index = (bimestre - 1) * 2;
            notas[index] = valor;
        }

        public void setNotaFinal(int bimestre, String valor) {
            int index = (bimestre - 1) * 2 + 1;
            notas[index] = valor;
        }

        public String getNotaString(int indice) {
            if (indice >= 0 && indice < notas.length) {
                return notas[indice];
            }
            return "--";
        }

        public double getNotaValor(int indice) {
            if (indice >= 0 && indice < notas.length) {
                try {
                    return Double.parseDouble(notas[indice]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
            return -1;
        }

        public int getNumero() { return numero; }
        public String getNombre() { return nombre; }

        public double calcularPromedio() {
            double suma = 0;
            int conteo = 0;

            for (String notaStr : notas) {
                try {
                    double nota = Double.parseDouble(notaStr);
                    suma += nota;
                    conteo++;
                } catch (NumberFormatException e) {
                    // Ignorar "--"
                }
            }

            return conteo > 0 ? suma / conteo : 0.0;
        }
    }
}