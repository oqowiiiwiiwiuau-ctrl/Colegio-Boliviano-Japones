package com.graficar.colegio.ui;

import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    private TableLayout tablaFija;
    private ScrollView scrollVertical;
    private HorizontalScrollView scrollHorizontal;
    private ScrollView scrollFijaVertical;
    private List<Estudiante> listaEstudiantes;
    private DatabaseReference databaseReference;
    private Map<String, Map<String, Object>> cambiosPendientes = new HashMap<>();
    private TextView tvTituloCurso;
    private LinearLayout contenedorPrincipal;

    private String cursoActual = "Matemática";

    // ENCABEZADOS
    private String[] encabezados = {
            "N°",
            "APELLIDOS Y NOMBRES",
            "SELLOS DE\nAGENDA",
            "NOTA",
            "CARÁTULAS",
            "PROMEDIO\nDIMENSIÓN\n(5 pts)",
            "Nº SELLOS\nAPUNTES",
            "PUNTAJE DE\nAPUNTES",
            "MAPA CONCEP.\nDE TRIÁNGULOS",
            "EV. PLANO\nCARTESIANO",
            "LECTURA\nMATEMÁTICA",
            "EVALUACIÓN\nTRIMESTRAL",
            "PROMEDIO\nDIMENSIÓN\n(45 pts)",
            "Nº SELLOS\nPRÁCTICAS",
            "PUNTAJE DE\nPRÁCTICAS",
            "PRACTICAS DE\nLIBRO SUDOKU",
            "EV. DE ANGULOS\nRECTOS Y LLANOS",
            "DIV. NUM.\nENTEROS",
            "PROMEDIO\nDIMENSIÓN\n(40 pts)",
            "AULA\nABIERTA",
            "CALIFICACIÓN\nDEL DECIDIR",
            "PROMEDIO\nDIMENSIÓN\n(5 pts)",
            "TOTAL\n(95 pts)",
            "AUTO-\nEVALUACIÓN",
            "NOTA\nPARCIAL",
            "PONDERA-\ncIONES",
            "NOTA\nTRIMESTRAL\n(100 pts)"
    };

    // ANCHOS DE COLUMNAS
    private int[] anchosColumnas = {
            40,     // N°
            180,    // APELLIDOS Y NOMBRES
            80,     // SELLOS DE AGENDA
            70,     // NOTA
            80,     // CARÁTULAS
            80,     // PROMEDIO DIMENSIÓN (5 pts)
            80,     // Nº SELLOS APUNTES
            90,     // PUNTAJE DE APUNTES
            90,     // MAPA CONCEP. DE TRIÁNGULOS
            90,     // EV. PLANO CARTESIANO
            90,     // LECTURA MATEMÁTICA
            90,     // EVALUACIÓN TRIMESTRAL
            90,     // PROMEDIO DIMENSIÓN (45 pts)
            90,     // Nº SELLOS PRÁCTICAS
            90,     // PUNTAJE DE PRÁCTICAS
            100,    // PRACTICAS DE LIBRO SUDOKU
            100,    // EV. DE ANGULOS RECTOS Y LLANOS
            90,     // DIV. NUM. ENTEROS
            90,     // PROMEDIO DIMENSIÓN (40 pts)
            80,     // AULA ABIERTA
            90,     // CALIFICACIÓN DEL DECIDIR
            90,     // PROMEDIO DIMENSIÓN (5 pts)
            80,     // TOTAL (95 pts)
            80,     // AUTOEVALUACIÓN
            80,     // NOTA PARCIAL
            80,     // PONDERACIONES
            90      // NOTA TRIMESTRAL (100 pts)
    };

    // MAPEO DE CLAVES DE FIREBASE
    private String[] clavesFirebase = {
            "sellosAgenda",
            "nota",
            "caratulas",
            "promedioDimension5",
            "nroSellosApuntes",
            "puntajeApuntes",
            "mapaConceptoTriangulos",
            "evPlanoCartesiano",
            "lecturaMatematica",
            "evaluacionTrimestral",
            "promedioDimension45",
            "nroSellosPracticas",
            "puntajePracticas",
            "practicasLibroSudoku",
            "evAngulosRectosLlanos",
            "divNumerosEnteros",
            "promedioDimension40",
            "aulaAbierta",
            "calificacionDecidir",
            "promedioDimension5_2",
            "total95",
            "autoevaluacion",
            "notaParcial",
            "ponderaciones",
            "notaTrimestral100"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primero);

        // Inicializar vistas
        tablaNotas = findViewById(R.id.tablaNotas);
        tablaFija = findViewById(R.id.tablaFija);
        scrollVertical = findViewById(R.id.scrollVertical);
        scrollHorizontal = findViewById(R.id.scrollHorizontal);
        scrollFijaVertical = findViewById(R.id.scrollFijaVertical);
        tvTituloCurso = findViewById(R.id.tvTituloCurso);
        contenedorPrincipal = findViewById(R.id.contenedorPrincipal);

        // Sincronizar scroll vertical entre ambas tablas
        scrollVertical.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollFijaVertical != null) {
                scrollFijaVertical.scrollTo(0, scrollY);
            }
        });

        // Sincronizar scroll vertical desde la tabla fija hacia la tabla de notas
        scrollFijaVertical.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollVertical != null) {
                scrollVertical.scrollTo(0, scrollY);
            }
        });

        // Botón de guardar
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            guardarTodosLosCambios();
            finish();
        });

        // Long press en el título para guardar
        tvTituloCurso.setOnLongClickListener(v -> {
            guardarTodosLosCambios();
            return true;
        });

        // Inicializar Firebase
        try {
            databaseReference = FirebaseDatabase.getInstance().getReference("alumnosPrimeroC");
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

                for (DataSnapshot alumnoSnapshot : snapshot.getChildren()) {
                    String nombreAlumno = alumnoSnapshot.getKey();
                    String nombreMostrar = nombreAlumno.replace("_", " ");
                    Log.d("Firebase", "Procesando alumno: " + nombreAlumno);

                    Estudiante estudiante = new Estudiante(contador, nombreMostrar, nombreAlumno);

                    DataSnapshot calificacionesSnapshot = alumnoSnapshot.child("calificaciones");

                    if (calificacionesSnapshot.exists()) {
                        Log.d("Firebase", "  ✓ " + nombreAlumno + " tiene calificaciones");

                        for (int i = 0; i < clavesFirebase.length; i++) {
                            String clave = clavesFirebase[i];
                            DataSnapshot notaSnapshot = calificacionesSnapshot.child(clave);

                            if (notaSnapshot.exists()) {
                                Object valor = notaSnapshot.getValue();
                                String valorStr = valor != null ? valor.toString() : "--";
                                estudiante.setNota(i, valorStr);
                            }
                        }
                    } else {
                        Log.d("Firebase", "  ✗ " + nombreAlumno + " NO tiene calificaciones");
                    }

                    listaEstudiantes.add(estudiante);
                    contador++;
                }

                Log.d("Firebase", "Total estudiantes en lista: " + listaEstudiantes.size());

                runOnUiThread(() -> {
                    tvTituloCurso.setText("Primero C - " + listaEstudiantes.size() + " alumnos");
                    crearTablas();
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

    private void crearTablas() {
        tablaNotas.removeAllViews();
        tablaFija.removeAllViews();

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
        // --- ENCABEZADO PARA TABLA FIJA ---
        TableRow filaEncabezadoFija = new TableRow(this);
        filaEncabezadoFija.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));

        // Columna N°
        TextView tvNumero = new TextView(this);
        tvNumero.setText("N°");
        tvNumero.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
        tvNumero.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvNumero.setTextSize(11);
        tvNumero.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNumero.setGravity(Gravity.CENTER);
        TableRow.LayoutParams paramsNumero = new TableRow.LayoutParams(dpToPx(anchosColumnas[0]), dpToPx(55));
        paramsNumero.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
        tvNumero.setLayoutParams(paramsNumero);
        filaEncabezadoFija.addView(tvNumero);

        // Columna NOMBRE
        TextView tvNombre = new TextView(this);
        tvNombre.setText("APELLIDOS Y NOMBRES");
        tvNombre.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
        tvNombre.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvNombre.setTextSize(11);
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNombre.setGravity(Gravity.CENTER);
        TableRow.LayoutParams paramsNombre = new TableRow.LayoutParams(dpToPx(anchosColumnas[1]), dpToPx(55));
        paramsNombre.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
        tvNombre.setLayoutParams(paramsNombre);
        filaEncabezadoFija.addView(tvNombre);

        tablaFija.addView(filaEncabezadoFija);

        // --- ENCABEZADO PARA TABLA DE NOTAS ---
        TableRow filaEncabezadoNotas = new TableRow(this);
        filaEncabezadoNotas.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));

        for (int i = 2; i < encabezados.length; i++) {
            TextView textView = new TextView(this);
            textView.setText(encabezados[i]);
            textView.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            textView.setTextSize(11);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
            textView.setGravity(Gravity.CENTER);

            if (encabezados[i].contains("\n")) {
                textView.setSingleLine(false);
                textView.setMaxLines(3);
            }

            TableRow.LayoutParams params = new TableRow.LayoutParams(
                    dpToPx(anchosColumnas[i]),
                    dpToPx(55)
            );
            params.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
            textView.setLayoutParams(params);

            filaEncabezadoNotas.addView(textView);
        }

        tablaNotas.addView(filaEncabezadoNotas);
    }

    private void crearFilaEstudiante(Estudiante estudiante) {
        int colorFondo = estudiante.getNumero() % 2 == 0
                ? ContextCompat.getColor(this, R.color.gris_claro)
                : ContextCompat.getColor(this, android.R.color.white);

        // --- FILA FIJA ---
        TableRow filaFija = new TableRow(this);
        filaFija.setBackgroundColor(colorFondo);

        TextView tvNumero = crearTextView(String.valueOf(estudiante.getNumero()));
        tvNumero.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[0]), dpToPx(40)));
        filaFija.addView(tvNumero);

        TextView tvNombre = crearTextView(estudiante.getNombre());
        tvNombre.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tvNombre.setPadding(dpToPx(6), 0, dpToPx(6), 0);
        tvNombre.setTextSize(11);
        tvNombre.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[1]), dpToPx(40)));
        filaFija.addView(tvNombre);

        tablaFija.addView(filaFija);

        // --- FILA DE NOTAS ---
        TableRow filaNotas = new TableRow(this);
        filaNotas.setBackgroundColor(colorFondo);

        for (int i = 0; i < clavesFirebase.length; i++) {
            String textoNota = estudiante.getNotaString(i);

            EditText editText = crearEditText(textoNota, estudiante, i);
            editText.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[i + 2]), dpToPx(40)));
            filaNotas.addView(editText);
        }

        tablaNotas.addView(filaNotas);
    }

    private void crearFilaTotales() {
        int colorFondo = ContextCompat.getColor(this, R.color.color_secundario);

        // --- FILA TOTALES FIJA ---
        TableRow filaTotalesFija = new TableRow(this);
        filaTotalesFija.setBackgroundColor(colorFondo);

        TextView tvTotales = crearTextView("PROMEDIOS");
        tvTotales.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvTotales.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotales.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[0]), dpToPx(40)));
        filaTotalesFija.addView(tvTotales);

        TextView tvVacia = crearTextView("");
        tvVacia.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[1]), dpToPx(40)));
        filaTotalesFija.addView(tvVacia);

        tablaFija.addView(filaTotalesFija);

        // --- FILA TOTALES DE NOTAS ---
        TableRow filaTotalesNotas = new TableRow(this);
        filaTotalesNotas.setBackgroundColor(colorFondo);

        double[] promediosColumnas = new double[clavesFirebase.length];
        int[] contadores = new int[clavesFirebase.length];

        for (Estudiante estudiante : listaEstudiantes) {
            for (int i = 0; i < clavesFirebase.length; i++) {
                double nota = estudiante.getNotaValor(i);
                if (nota != -1) {
                    promediosColumnas[i] += nota;
                    contadores[i]++;
                }
            }
        }

        for (int i = 0; i < clavesFirebase.length; i++) {
            double promedio = contadores[i] > 0 ? promediosColumnas[i] / contadores[i] : 0;
            TextView tvPromColumna = crearTextView(String.format("%.1f", promedio));
            tvPromColumna.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvPromColumna.setTypeface(null, android.graphics.Typeface.BOLD);
            tvPromColumna.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchosColumnas[i + 2]), dpToPx(40)));
            filaTotalesNotas.addView(tvPromColumna);
        }

        tablaNotas.addView(filaTotalesNotas);
    }

    private TextView crearTextView(String texto) {
        TextView textView = new TextView(this);
        textView.setText(texto);
        textView.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
        textView.setTextSize(11);
        textView.setGravity(Gravity.CENTER);
        textView.setSingleLine(true);
        return textView;
    }

    private EditText crearEditText(String texto, Estudiante estudiante, int indiceNota) {
        EditText editText = new EditText(this);
        editText.setText(texto);
        editText.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
        editText.setTextSize(11);
        editText.setGravity(Gravity.CENTER);
        editText.setBackgroundResource(android.R.drawable.edit_text);
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setSingleLine(true);
        editText.setEnabled(true);

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

                String key = estudiante.getKeyFirebase() + "_" + clavesFirebase[indiceNota];
                Map<String, Object> cambio = new HashMap<>();
                cambio.put("valor", nuevoValor);
                cambio.put("estudianteKey", estudiante.getKeyFirebase());
                cambio.put("indiceNota", indiceNota);
                cambio.put("claveFirebase", clavesFirebase[indiceNota]);
                cambiosPendientes.put(key, cambio);

                estudiante.setNota(indiceNota, nuevoValor);

                tvTituloCurso.setText("Primero C * " + listaEstudiantes.size() + " alumnos");
            }
        });

        return editText;
    }

    private void guardarTodosLosCambios() {
        if (cambiosPendientes.isEmpty()) {
            Toast.makeText(this, "No hay cambios para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Guardando cambios (" + cambiosPendientes.size() + ")...", Toast.LENGTH_SHORT).show();

        for (Map.Entry<String, Map<String, Object>> entry : cambiosPendientes.entrySet()) {
            Map<String, Object> cambio = entry.getValue();
            String estudianteKey = (String) cambio.get("estudianteKey");
            String claveFirebase = (String) cambio.get("claveFirebase");
            String valor = (String) cambio.get("valor");

            DatabaseReference ref = databaseReference
                    .child(estudianteKey)
                    .child("calificaciones")
                    .child(claveFirebase);

            Object valorGuardar = valor.equals("--") ? null : valor;

            ref.setValue(valorGuardar)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase", "Guardado: " + estudianteKey + " - " + claveFirebase + " = " + valor);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Error al guardar: " + e.getMessage());
                        Toast.makeText(PrimeroActivity.this,
                                "Error al guardar: " + estudianteKey,
                                Toast.LENGTH_SHORT).show();
                    });
        }

        cambiosPendientes.clear();
        tvTituloCurso.setText("Primero C - " + listaEstudiantes.size() + " alumnos");
        Toast.makeText(this, "Cambios guardados exitosamente", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class Estudiante {
        private int numero;
        private String nombre;
        private String keyFirebase;
        private String[] notas;

        public Estudiante(int numero, String nombre, String keyFirebase) {
            this.numero = numero;
            this.nombre = nombre;
            this.keyFirebase = keyFirebase;
            this.notas = new String[25];
            for (int i = 0; i < notas.length; i++) {
                notas[i] = "--";
            }
        }

        public void setNota(int indice, String valor) {
            if (indice >= 0 && indice < notas.length) {
                notas[indice] = valor;
            }
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
        public String getKeyFirebase() { return keyFirebase; }
    }
}