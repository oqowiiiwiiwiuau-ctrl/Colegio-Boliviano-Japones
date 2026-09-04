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
    // ============================================================
    // COMPONENTES UI
    // ============================================================
    private TableLayout tablaNotas;
    private TableLayout tablaFija;
    private ScrollView scrollVertical;
    private HorizontalScrollView scrollHorizontal;
    private ScrollView scrollFijaVertical;
    private TextView tvTituloCurso;

    // ============================================================
    // DATOS PRINCIPALES
    // ============================================================
    private List<Estudiante> listaEstudiantes;
    private DatabaseReference databaseReference;
    private Map<String, Map<String, Object>> cambiosPendientes = new HashMap<>();

    // ============================================================
    // CONFIGURACIÓN DE ANCHOS (¡AQUÍ SE AJUSTAN TAMAÑOS!)
    // ============================================================
    private static final int ANCHO_NUMERO = 25;          // Ancho de columna N°
    private static final int ANCHO_NOMBRE = 170;         // Ancho de columna APELLIDOS Y NOMBRES
    private static final int ANCHO_PROMEDIO = 90;        // Ancho de columna de promedio (se puede ajustar por grupo)

    // ============================================================
    // ESTRUCTURA DE COLUMNAS
    // ============================================================
    private static class GrupoColumnas {
        String nombre;
        List<ColumnaHija> columnasHijas;
        String clavePromedio;      // Clave en Firebase para guardar el promedio
        int anchoPromedio;         // Ancho de la columna de promedio (si es 0 usa ANCHO_PROMEDIO)

        public GrupoColumnas(String nombre, List<ColumnaHija> columnasHijas, String clavePromedio, int anchoPromedio) {
            this.nombre = nombre;
            this.columnasHijas = columnasHijas;
            this.clavePromedio = clavePromedio;
            this.anchoPromedio = anchoPromedio > 0 ? anchoPromedio : ANCHO_PROMEDIO;
        }
    }

    private static class ColumnaHija {
        String titulo;
        int ancho;
        String claveFirebase;

        public ColumnaHija(String titulo, int ancho, String claveFirebase) {
            this.titulo = titulo;
            this.ancho = ancho;
            this.claveFirebase = claveFirebase;
        }
    }

    private List<GrupoColumnas> grupos = new ArrayList<>();

    // ============================================================
    // CONFIGURACIÓN DE GRUPOS Y COLUMNAS (¡AQUÍ SE AGREGAN/MODIFICAN!)
    // ============================================================
    private void inicializarEstructura() {
        // GRUPO SER
        List<ColumnaHija> columnasSer = new ArrayList<>();
        columnasSer.add(new ColumnaHija("CARÁTULAS", 80, "caratulas"));
        columnasSer.add(new ColumnaHija("AGENDA", 80, "sellosAgenda"));
        grupos.add(new GrupoColumnas("SER", columnasSer, "promedioSer", 90));

        // GRUPO SABER
        List<ColumnaHija> columnasSaber = new ArrayList<>();
        columnasSaber.add(new ColumnaHija("SELLOS APUNTES", 80, "nroSellosApuntes"));
        columnasSaber.add(new ColumnaHija("PUNTAJE APUNTES", 80, "puntajeApuntes"));
        columnasSaber.add(new ColumnaHija("MAPA CONCEPTO", 80, "mapaConceptoTriangulos"));
        columnasSaber.add(new ColumnaHija("PLANO CARTESIANO", 80, "evPlanoCartesiano"));
        columnasSaber.add(new ColumnaHija("LECTURA MATEMÁTICA", 80, "lecturaMatematica"));
        columnasSaber.add(new ColumnaHija("EV. TRIMESTRAL", 80, "evaluacionTrimestral"));
        grupos.add(new GrupoColumnas("SABER", columnasSaber, "promedioSaber", 90));

        // GRUPO HACER
        List<ColumnaHija> columnasHacer = new ArrayList<>();
        columnasHacer.add(new ColumnaHija("SELLOS PRÁCTICAS", 80, "nroSellosPracticas"));
        columnasHacer.add(new ColumnaHija("PUNTAJE PRÁCTICAS", 80, "puntajePracticas"));
        columnasHacer.add(new ColumnaHija("LIBRO SUDOKU", 80, "practicasLibroSudoku"));
        columnasHacer.add(new ColumnaHija("PRACTICA EMP. N1", 80, "practicaEmpastado1"));
        columnasHacer.add(new ColumnaHija("PRACTICA EMP. N2", 80, "practicaEmpastado2"));
        columnasHacer.add(new ColumnaHija("REFORZAMIENTO", 80, "reforzamiento"));
        grupos.add(new GrupoColumnas("HACER", columnasHacer, "promedioHacer", 90));

        // GRUPO DECIDIR
        List<ColumnaHija> columnasDecidir = new ArrayList<>();
        columnasDecidir.add(new ColumnaHija("AULA ABIERTA", 80, "aulaAbierta"));
        columnasDecidir.add(new ColumnaHija("CALIFICACIÓN DECIDIR", 80, "calificacionDecidir"));
        grupos.add(new GrupoColumnas("DECIDIR", columnasDecidir, "promedioDecidir", 90));

        // GRUPO TOTALES (sin promedio)
        List<ColumnaHija> columnasTotales = new ArrayList<>();
        columnasTotales.add(new ColumnaHija("AUTOEVALUACIÓN", 80, "autoevaluacion"));
        columnasTotales.add(new ColumnaHija("NOTA PARCIAL", 80, "notaParcial"));
        columnasTotales.add(new ColumnaHija("PONDERACIONES", 80, "ponderaciones"));
        columnasTotales.add(new ColumnaHija("NOTA TRIMESTRAL", 90, "notaTrimestral100"));
        grupos.add(new GrupoColumnas("TOTALES", columnasTotales, null, 0));
    }

    // ============================================================
    // CLASE ESTUDIANTE
    // ============================================================
    private static class Estudiante {
        private int numero;
        private String nombre;
        private String keyFirebase;
        private Map<String, String> notas;

        public Estudiante(int numero, String nombre, String keyFirebase) {
            this.numero = numero;
            this.nombre = nombre;
            this.keyFirebase = keyFirebase;
            this.notas = new HashMap<>();
        }

        public void setNota(String clave, String valor) {
            notas.put(clave, valor);
        }

        public String getNota(String clave) {
            return notas.getOrDefault(clave, "--");
        }

        public double getNotaValor(String clave) {
            String valor = notas.get(clave);
            if (valor == null || valor.equals("--")) return -1;
            try {
                return Double.parseDouble(valor);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        public int getNumero() { return numero; }
        public String getNombre() { return nombre; }
        public String getKeyFirebase() { return keyFirebase; }
    }

    // ============================================================
    // CICLO DE VIDA
    // ============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primero);

        inicializarEstructura();
        inicializarViews();
        configurarListeners();
        inicializarFirebase();
    }

    private void inicializarViews() {
        tablaNotas = findViewById(R.id.tablaNotas);
        tablaFija = findViewById(R.id.tablaFija);
        scrollVertical = findViewById(R.id.scrollVertical);
        scrollHorizontal = findViewById(R.id.scrollHorizontal);
        scrollFijaVertical = findViewById(R.id.scrollFijaVertical);
        tvTituloCurso = findViewById(R.id.tvTituloCurso);
    }

    private void configurarListeners() {
        scrollVertical.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollFijaVertical != null) {
                scrollFijaVertical.scrollTo(0, scrollY);
            }
        });

        scrollFijaVertical.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollVertical != null) {
                scrollVertical.scrollTo(0, scrollY);
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            guardarTodosLosCambios();
            finish();
        });

        tvTituloCurso.setOnLongClickListener(v -> {
            guardarTodosLosCambios();
            return true;
        });
    }

    // ============================================================
    // FIREBASE
    // ============================================================
    private void inicializarFirebase() {
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

                for (DataSnapshot alumnoSnapshot : snapshot.getChildren()) {
                    String nombreAlumno = alumnoSnapshot.getKey();
                    String nombreMostrar = nombreAlumno.replace("_", " ");
                    Estudiante estudiante = new Estudiante(contador, nombreMostrar, nombreAlumno);

                    DataSnapshot calificacionesSnapshot = alumnoSnapshot.child("calificaciones");
                    if (calificacionesSnapshot.exists()) {
                        for (GrupoColumnas grupo : grupos) {
                            // Cargar hijas
                            for (ColumnaHija columna : grupo.columnasHijas) {
                                DataSnapshot notaSnapshot = calificacionesSnapshot.child(columna.claveFirebase);
                                if (notaSnapshot.exists()) {
                                    Object valor = notaSnapshot.getValue();
                                    estudiante.setNota(columna.claveFirebase,
                                            valor != null ? valor.toString() : "--");
                                }
                            }
                            // Cargar promedio si existe
                            if (grupo.clavePromedio != null) {
                                DataSnapshot promSnapshot = calificacionesSnapshot.child(grupo.clavePromedio);
                                if (promSnapshot.exists()) {
                                    Object valor = promSnapshot.getValue();
                                    estudiante.setNota(grupo.clavePromedio,
                                            valor != null ? valor.toString() : "--");
                                }
                            }
                        }
                    }

                    listaEstudiantes.add(estudiante);
                    contador++;
                }

                runOnUiThread(() -> {
                    tvTituloCurso.setText("PRIMERO C - " + listaEstudiantes.size() + " alumnos");
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

    // ============================================================
    // CREACIÓN DE TABLAS
    // ============================================================
    private void crearTablas() {
        tablaNotas.removeAllViews();
        tablaFija.removeAllViews();

        if (listaEstudiantes == null || listaEstudiantes.isEmpty()) {
            mostrarMensajeVacio();
            return;
        }

        crearEncabezados();
        for (Estudiante estudiante : listaEstudiantes) {
            crearFilaEstudiante(estudiante);
        }
        crearFilaTotales();
    }

    private void mostrarMensajeVacio() {
        TextView tvMensaje = new TextView(this);
        tvMensaje.setText("No hay estudiantes para mostrar");
        tvMensaje.setTextSize(16);
        tvMensaje.setGravity(Gravity.CENTER);
        tvMensaje.setPadding(20, 50, 20, 50);
        tablaNotas.addView(tvMensaje);
    }

    // ============================================================
    // ENCABEZADOS
    // ============================================================
    private void crearEncabezados() {
        // FILA 1: Nombres de grupos (con ancho total incluyendo promedio)
        TableRow filaGrupos = new TableRow(this);
        filaGrupos.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));
        filaGrupos.setGravity(Gravity.CENTER);

        // Espacios para N° y NOMBRE (vacíos)
        TextView tvVacio1 = new TextView(this);
        tvVacio1.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NUMERO), dpToPx(35)));
        filaGrupos.addView(tvVacio1);

        TextView tvVacio2 = new TextView(this);
        tvVacio2.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOMBRE), dpToPx(35)));
        filaGrupos.addView(tvVacio2);

        for (GrupoColumnas grupo : grupos) {
            int anchoTotal = 0;
            for (ColumnaHija columna : grupo.columnasHijas) {
                anchoTotal += columna.ancho;
            }
            // Si tiene promedio, sumar su ancho
            if (grupo.clavePromedio != null) {
                anchoTotal += grupo.anchoPromedio;
            }
            TextView tvGrupo = new TextView(this);
            tvGrupo.setText(grupo.nombre);
            tvGrupo.setTextSize(14);
            tvGrupo.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvGrupo.setTypeface(null, android.graphics.Typeface.BOLD);
            tvGrupo.setGravity(Gravity.CENTER);
            tvGrupo.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));
            tvGrupo.setLayoutParams(new TableRow.LayoutParams(dpToPx(anchoTotal), dpToPx(35)));
            filaGrupos.addView(tvGrupo);
        }
        tablaNotas.addView(filaGrupos);

        // FILA 1 (fija): N° y NOMBRE
        TableRow filaEncabezadoFija = new TableRow(this);
        filaEncabezadoFija.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));

        TextView tvNumero = new TextView(this);
        tvNumero.setText("N°");
        tvNumero.setTextSize(12);
        tvNumero.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvNumero.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNumero.setGravity(Gravity.CENTER);
        tvNumero.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NUMERO), dpToPx(35)));
        filaEncabezadoFija.addView(tvNumero);

        TextView tvApellidos = new TextView(this);
        tvApellidos.setText("APELLIDOS Y NOMBRES");
        tvApellidos.setTextSize(12);
        tvApellidos.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvApellidos.setTypeface(null, android.graphics.Typeface.BOLD);
        tvApellidos.setGravity(Gravity.CENTER);
        tvApellidos.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOMBRE), dpToPx(35)));
        filaEncabezadoFija.addView(tvApellidos);

        tablaFija.addView(filaEncabezadoFija);

        // FILA 2: Nombres de columnas hijas + promedio
        TableRow filaColumnas = new TableRow(this);
        filaColumnas.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));

        for (GrupoColumnas grupo : grupos) {
            // Columnas hijas
            for (ColumnaHija columna : grupo.columnasHijas) {
                TextView tvCol = new TextView(this);
                tvCol.setText(columna.titulo);
                tvCol.setTextSize(10);
                tvCol.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                tvCol.setGravity(Gravity.CENTER);
                tvCol.setLayoutParams(new TableRow.LayoutParams(dpToPx(columna.ancho), dpToPx(30)));
                filaColumnas.addView(tvCol);
            }
            // Columna de promedio (si existe)
            if (grupo.clavePromedio != null) {
                TextView tvProm = new TextView(this);
                tvProm.setText("PROMEDIO");
                tvProm.setTextSize(10);
                tvProm.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                tvProm.setTypeface(null, android.graphics.Typeface.BOLD);
                tvProm.setGravity(Gravity.CENTER);
                tvProm.setLayoutParams(new TableRow.LayoutParams(dpToPx(grupo.anchoPromedio), dpToPx(30)));
                filaColumnas.addView(tvProm);
            }
        }
        tablaNotas.addView(filaColumnas);

        // FILA 2 (fija): vacía (para alinear)
        TableRow filaEncabezadoFija2 = new TableRow(this);
        filaEncabezadoFija2.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));
        TextView tvVacioF1 = new TextView(this);
        tvVacioF1.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NUMERO), dpToPx(30)));
        filaEncabezadoFija2.addView(tvVacioF1);
        TextView tvVacioF2 = new TextView(this);
        tvVacioF2.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOMBRE), dpToPx(30)));
        filaEncabezadoFija2.addView(tvVacioF2);
        tablaFija.addView(filaEncabezadoFija2);
    }

    // ============================================================
    // FILAS DE ESTUDIANTES
    // ============================================================
    private void crearFilaEstudiante(Estudiante estudiante) {
        int colorFondo = estudiante.getNumero() % 2 == 0
                ? ContextCompat.getColor(this, R.color.gris_claro)
                : ContextCompat.getColor(this, android.R.color.white);

        // FILA FIJA: N° y NOMBRE
        TableRow filaFija = new TableRow(this);
        filaFija.setBackgroundColor(colorFondo);

        TextView tvNumero = new TextView(this);
        tvNumero.setText(String.valueOf(estudiante.getNumero()));
        tvNumero.setTextSize(12);
        tvNumero.setGravity(Gravity.CENTER);
        tvNumero.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NUMERO), dpToPx(35)));
        filaFija.addView(tvNumero);

        TextView tvNombre = new TextView(this);
        tvNombre.setText(estudiante.getNombre());
        tvNombre.setTextSize(11);
        tvNombre.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tvNombre.setPadding(dpToPx(8), 0, dpToPx(8), 0);
        tvNombre.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOMBRE), dpToPx(35)));
        filaFija.addView(tvNombre);

        tablaFija.addView(filaFija);

        // FILA DE NOTAS + PROMEDIOS
        TableRow filaNotas = new TableRow(this);
        filaNotas.setBackgroundColor(colorFondo);

        for (GrupoColumnas grupo : grupos) {
            // Columnas hijas
            for (ColumnaHija columna : grupo.columnasHijas) {
                String valor = estudiante.getNota(columna.claveFirebase);
                // Determinar si es editable o no (solo las de TOTALES no editables)
                boolean editable = !(columna.claveFirebase.equals("notaTrimestral100") ||
                        columna.claveFirebase.equals("ponderaciones") ||
                        columna.claveFirebase.equals("autoevaluacion") ||
                        columna.claveFirebase.equals("notaParcial"));

                if (editable) {
                    EditText editText = new EditText(this);
                    editText.setText(valor);
                    editText.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
                    editText.setTextSize(11);
                    editText.setGravity(Gravity.CENTER);
                    editText.setBackgroundResource(android.R.drawable.edit_text);
                    editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    editText.setSingleLine(true);

                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}

                        @Override
                        public void afterTextChanged(Editable s) {
                            String nuevoValor = s.toString().trim();
                            if (nuevoValor.isEmpty()) nuevoValor = "--";

                            // Guardar cambio de la hija
                            String key = estudiante.getKeyFirebase() + "_" + columna.claveFirebase;
                            Map<String, Object> cambio = new HashMap<>();
                            cambio.put("valor", nuevoValor);
                            cambio.put("estudianteKey", estudiante.getKeyFirebase());
                            cambio.put("claveFirebase", columna.claveFirebase);
                            cambiosPendientes.put(key, cambio);

                            estudiante.setNota(columna.claveFirebase, nuevoValor);

                            // Recalcular y guardar promedio del grupo (si existe)
                            if (grupo.clavePromedio != null) {
                                double promedio = calcularPromedioGrupo(estudiante, grupo);
                                String valorProm = String.format("%.1f", promedio);
                                String keyProm = estudiante.getKeyFirebase() + "_" + grupo.clavePromedio;
                                Map<String, Object> cambioProm = new HashMap<>();
                                cambioProm.put("valor", valorProm);
                                cambioProm.put("estudianteKey", estudiante.getKeyFirebase());
                                cambioProm.put("claveFirebase", grupo.clavePromedio);
                                cambiosPendientes.put(keyProm, cambioProm);
                                estudiante.setNota(grupo.clavePromedio, valorProm);

                                // Actualizar la celda de promedio visualmente (opcional, pero se refresca al recargar)
                                // Para simplificar, recargamos toda la tabla al perder foco o al guardar.
                                // Aquí podrías forzar un refresh, pero es costoso. Mejor al guardar.
                            }
                        }
                    });

                    editText.setLayoutParams(new TableRow.LayoutParams(dpToPx(columna.ancho), dpToPx(35)));
                    filaNotas.addView(editText);
                } else {
                    // No editable (TextView)
                    TextView tv = new TextView(this);
                    tv.setText(valor);
                    tv.setTextSize(11);
                    tv.setGravity(Gravity.CENTER);
                    tv.setLayoutParams(new TableRow.LayoutParams(dpToPx(columna.ancho), dpToPx(35)));
                    filaNotas.addView(tv);
                }
            }

            // Columna de PROMEDIO (si existe)
            if (grupo.clavePromedio != null) {
                String valorProm = estudiante.getNota(grupo.clavePromedio);
                // Si no existe, calcular y asignar
                if (valorProm.equals("--")) {
                    double promedio = calcularPromedioGrupo(estudiante, grupo);
                    valorProm = String.format("%.1f", promedio);
                    estudiante.setNota(grupo.clavePromedio, valorProm);
                }
                TextView tvProm = new TextView(this);
                tvProm.setText(valorProm);
                tvProm.setTextSize(12);
                tvProm.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
                tvProm.setTypeface(null, android.graphics.Typeface.BOLD);
                tvProm.setGravity(Gravity.CENTER);
                tvProm.setLayoutParams(new TableRow.LayoutParams(dpToPx(grupo.anchoPromedio), dpToPx(35)));
                filaNotas.addView(tvProm);
            }
        }

        tablaNotas.addView(filaNotas);
    }

    // ============================================================
    // CÁLCULO DE PROMEDIO DE UN GRUPO
    // ============================================================
    private double calcularPromedioGrupo(Estudiante estudiante, GrupoColumnas grupo) {
        double suma = 0;
        int count = 0;
        for (ColumnaHija columna : grupo.columnasHijas) {
            double valor = estudiante.getNotaValor(columna.claveFirebase);
            if (valor >= 0) {
                suma += valor;
                count++;
            }
        }
        return count > 0 ? suma / count : 0;
    }

    // ============================================================
    // FILA DE TOTALES (PROMEDIOS POR COLUMNA)
    // ============================================================
    private void crearFilaTotales() {
        int colorFondo = ContextCompat.getColor(this, R.color.color_secundario);

        // FILA TOTALES FIJA
        TableRow filaTotalesFija = new TableRow(this);
        filaTotalesFija.setBackgroundColor(colorFondo);

        TextView tvTotales = new TextView(this);
        tvTotales.setText("PROMEDIOS");
        tvTotales.setTextSize(12);
        tvTotales.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvTotales.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotales.setGravity(Gravity.CENTER);
        tvTotales.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NUMERO), dpToPx(35)));
        filaTotalesFija.addView(tvTotales);

        TextView tvVacia = new TextView(this);
        tvVacia.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOMBRE), dpToPx(35)));
        filaTotalesFija.addView(tvVacia);

        tablaFija.addView(filaTotalesFija);

        // FILA TOTALES DE NOTAS
        TableRow filaTotalesNotas = new TableRow(this);
        filaTotalesNotas.setBackgroundColor(colorFondo);

        for (GrupoColumnas grupo : grupos) {
            // Promedios de columnas hijas
            for (ColumnaHija columna : grupo.columnasHijas) {
                double suma = 0;
                int count = 0;
                for (Estudiante estudiante : listaEstudiantes) {
                    double valor = estudiante.getNotaValor(columna.claveFirebase);
                    if (valor >= 0) {
                        suma += valor;
                        count++;
                    }
                }
                double promedio = count > 0 ? suma / count : 0;
                TextView tvProm = new TextView(this);
                tvProm.setText(String.format("%.1f", promedio));
                tvProm.setTextSize(11);
                tvProm.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                tvProm.setTypeface(null, android.graphics.Typeface.BOLD);
                tvProm.setGravity(Gravity.CENTER);
                tvProm.setLayoutParams(new TableRow.LayoutParams(dpToPx(columna.ancho), dpToPx(35)));
                filaTotalesNotas.addView(tvProm);
            }
            // Promedio del grupo (si existe)
            if (grupo.clavePromedio != null) {
                double suma = 0;
                int count = 0;
                for (Estudiante estudiante : listaEstudiantes) {
                    double valor = estudiante.getNotaValor(grupo.clavePromedio);
                    if (valor >= 0) {
                        suma += valor;
                        count++;
                    }
                }
                double promedio = count > 0 ? suma / count : 0;
                TextView tvProm = new TextView(this);
                tvProm.setText(String.format("%.1f", promedio));
                tvProm.setTextSize(12);
                tvProm.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                tvProm.setTypeface(null, android.graphics.Typeface.BOLD);
                tvProm.setGravity(Gravity.CENTER);
                tvProm.setLayoutParams(new TableRow.LayoutParams(dpToPx(grupo.anchoPromedio), dpToPx(35)));
                filaTotalesNotas.addView(tvProm);
            }
        }

        tablaNotas.addView(filaTotalesNotas);
    }

    // ============================================================
    // GUARDAR EN FIREBASE
    // ============================================================
    private void guardarTodosLosCambios() {
        if (cambiosPendientes.isEmpty()) {
            Toast.makeText(this, "No hay cambios para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Guardando cambios (" + cambiosPendientes.size() + ")", Toast.LENGTH_SHORT).show();

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
        Toast.makeText(this, "Cambios guardados exitosamente", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}