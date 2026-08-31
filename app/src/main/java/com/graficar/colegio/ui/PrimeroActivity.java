package com.graficar.colegio.ui;

import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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
    // 1. COMPONENTES UI
    // ============================================================
    private TableLayout tablaNotas;
    private TableLayout tablaFija;
    private ScrollView scrollVertical;
    private HorizontalScrollView scrollHorizontal;
    private ScrollView scrollFijaVertical;
    private TextView tvTituloCurso;
    private LinearLayout contenedorPrincipal;
    private Button btnAgregarColumna;

    // ============================================================
    // 2. DATOS PRINCIPALES
    // ============================================================
    private List<Estudiante> listaEstudiantes;
    private DatabaseReference databaseReference;
    private Map<String, Map<String, Object>> cambiosPendientes = new HashMap<>();
    private String cursoActual = "Matemática";

    // ============================================================
    // 3. CONFIGURACIÓN DE COLUMNAS
    // ============================================================
    private List<ColumnaConfig> columnasConfig = new ArrayList<>();

    private void inicializarColumnas() {
        // ============================================================
        // SECCIÓN 1: COLUMNAS FIJAS (SIEMPRE VISIBLES)
        // ============================================================
        columnasConfig.add(new ColumnaConfig(
                "N°",
                25,
                TipoColumna.NUMERO,
                null,
                null
        ));

        columnasConfig.add(new ColumnaConfig(
                "APELLIDOS Y NOMBRES",
                170,
                TipoColumna.TEXTO,
                null,
                null
        ));

        // ============================================================
        // SECCIÓN 2: DIMENSIÓN 1 - SELLOS Y CARÁTULAS
        // ============================================================
        columnasConfig.add(new ColumnaConfig(
                "SELLOS DE\nAGENDA",
                60,
                TipoColumna.NOTA,
                "sellosAgenda",
                new int[]{0, 1, 2}
        ));

        columnasConfig.add(new ColumnaConfig(
                "NOTA",
                60,
                TipoColumna.NOTA,
                "nota",
                new int[]{0, 1, 2}
        ));

        columnasConfig.add(new ColumnaConfig(
                "CARÁTULAS",
                60,
                TipoColumna.NOTA,
                "caratulas",
                new int[]{0, 1, 2}
        ));

        // ============================================================
        // SECCIÓN 3: PROMEDIO DIMENSIÓN 1 (5 pts)
        // ============================================================
        columnasConfig.add(new ColumnaConfig(
                "PROMEDIO\nDIMENSIÓN\n(5 pts)",
                80,
                TipoColumna.PROMEDIO,
                "promedioDimension5",
                new int[]{0, 1, 2}
        ));

        // ============================================================
        // SECCIÓN 4: DIMENSIÓN 2 - APUNTES Y MAPAS
        // ============================================================
        columnasConfig.add(new ColumnaConfig(
                "Nº SELLOS\nAPUNTES",
                80,
                TipoColumna.NOTA,
                "nroSellosApuntes",
                new int[]{3, 4, 5, 6, 7, 8}
        ));

        columnasConfig.add(new ColumnaConfig(
                "PUNTAJE DE\nAPUNTES",
                90,
                TipoColumna.NOTA,
                "puntajeApuntes",
                new int[]{3, 4, 5, 6, 7, 8}
        ));

        columnasConfig.add(new ColumnaConfig(
                "MAPA CONCEP.\nDE TRIÁNGULOS",
                90,
                TipoColumna.NOTA,
                "mapaConceptoTriangulos",
                new int[]{3, 4, 5, 6, 7, 8}
        ));

        columnasConfig.add(new ColumnaConfig(
                "EV. PLANO\nCARTESIANO",
                90,
                TipoColumna.NOTA,
                "evPlanoCartesiano",
                new int[]{3, 4, 5, 6, 7, 8}
        ));

        columnasConfig.add(new ColumnaConfig(
                "LECTURA\nMATEMÁTICA",
                90,
                TipoColumna.NOTA,
                "lecturaMatematica",
                new int[]{3, 4, 5, 6, 7, 8}
        ));

        columnasConfig.add(new ColumnaConfig(
                "EVALUACIÓN\nTRIMESTRAL",
                90,
                TipoColumna.NOTA,
                "evaluacionTrimestral",
                new int[]{3, 4, 5, 6, 7, 8}
        ));

        // ============================================================
        // SECCIÓN 5: PROMEDIO DIMENSIÓN 2 (45 pts)
        // ============================================================
        columnasConfig.add(new ColumnaConfig(
                "PROMEDIO\nDIMENSIÓN\n(45 pts)",
                90,
                TipoColumna.PROMEDIO_PONDERADO,
                "promedioDimension45",
                null
        ));

        // ============================================================
        // SECCIÓN 6: DIMENSIÓN 3 - PRÁCTICAS
        // ============================================================
        columnasConfig.add(new ColumnaConfig(
                "Nº SELLOS\nPRÁCTICAS",
                90,
                TipoColumna.NOTA,
                "nroSellosPracticas",
                new int[]{10, 11, 12, 13, 14}
        ));

        columnasConfig.add(new ColumnaConfig(
                "PUNTAJE DE\nPRÁCTICAS",
                90,
                TipoColumna.NOTA,
                "puntajePracticas",
                new int[]{10, 11, 12, 13, 14}
        ));

        columnasConfig.add(new ColumnaConfig(
                "PRACTICAS DE\nLIBRO SUDOKU",
                100,
                TipoColumna.NOTA,
                "practicasLibroSudoku",
                new int[]{10, 11, 12, 13, 14}
        ));

        columnasConfig.add(new ColumnaConfig(
                "EV. DE ANGULOS\nRECTOS Y LLANOS",
                100,
                TipoColumna.NOTA,
                "evAngulosRectosLlanos",
                new int[]{10, 11, 12, 13, 14}
        ));

        columnasConfig.add(new ColumnaConfig(
                "DIV. NUM.\nENTEROS",
                90,
                TipoColumna.NOTA,
                "divNumerosEnteros",
                new int[]{10, 11, 12, 13, 14}
        ));

        // ============================================================
        // SECCIÓN 7: PROMEDIO DIMENSIÓN 3 (40 pts)
        // ============================================================
        columnasConfig.add(new ColumnaConfig(
                "PROMEDIO\nDIMENSIÓN\n(40 pts)",
                90,
                TipoColumna.PROMEDIO_PONDERADO,
                "promedioDimension40",
                null
        ));

        // ============================================================
        // SECCIÓN 8: DIMENSIÓN 4 - AULA ABIERTA Y DECIDIR
        // ============================================================
        columnasConfig.add(new ColumnaConfig(
                "AULA\nABIERTA",
                80,
                TipoColumna.NOTA,
                "aulaAbierta",
                new int[]{16, 17}
        ));

        columnasConfig.add(new ColumnaConfig(
                "CALIFICACIÓN\nDEL DECIDIR",
                90,
                TipoColumna.NOTA,
                "calificacionDecidir",
                new int[]{16, 17}
        ));

        // ============================================================
        // SECCIÓN 9: PROMEDIO DIMENSIÓN 4 (5 pts)
        // ============================================================
        columnasConfig.add(new ColumnaConfig(
                "PROMEDIO\nDIMENSIÓN\n(5 pts)",
                90,
                TipoColumna.PROMEDIO,
                "promedioDimension5_2",
                new int[]{16, 17}
        ));

        // ============================================================
        // SECCIÓN 10: TOTALES
        // ============================================================
        columnasConfig.add(new ColumnaConfig(
                "TOTAL\n(95 pts)",
                80,
                TipoColumna.TOTAL,
                "total95",
                null
        ));

        columnasConfig.add(new ColumnaConfig(
                "AUTO-\nEVALUACIÓN",
                80,
                TipoColumna.NOTA,
                "autoevaluacion",
                null
        ));

        columnasConfig.add(new ColumnaConfig(
                "NOTA\nPARCIAL",
                80,
                TipoColumna.NOTA,
                "notaParcial",
                null
        ));

        columnasConfig.add(new ColumnaConfig(
                "PONDERA-\ncIONES",
                80,
                TipoColumna.NOTA,
                "ponderaciones",
                null
        ));

        // ============================================================
        // SECCIÓN 11: NOTA FINAL
        // ============================================================
        columnasConfig.add(new ColumnaConfig(
                "NOTA\nTRIMESTRAL\n(100 pts)",
                90,
                TipoColumna.NOTA_FINAL,
                "notaTrimestral100",
                null
        ));
    }

    // ============================================================
    // 4. ENUM DE TIPOS DE COLUMNA
    // ============================================================
    private enum TipoColumna {
        NUMERO,
        TEXTO,
        NOTA,
        PROMEDIO,
        PROMEDIO_PONDERADO,
        TOTAL,
        NOTA_FINAL
    }

    // ============================================================
    // 5. CLASE DE CONFIGURACIÓN DE COLUMNA
    // ============================================================
    private static class ColumnaConfig {
        String titulo;
        int ancho;
        TipoColumna tipo;
        String claveFirebase;
        int[] indicesDependientes;

        public ColumnaConfig(String titulo, int ancho, TipoColumna tipo,
                             String claveFirebase, int[] indicesDependientes) {
            this.titulo = titulo;
            this.ancho = ancho;
            this.tipo = tipo;
            this.claveFirebase = claveFirebase;
            this.indicesDependientes = indicesDependientes;
        }
    }

    // ============================================================
    // 6. CLASE ESTUDIANTE
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

        // ============================================================
        // 7. FUNCIONES DE CÁLCULO
        // ============================================================

        public double calcularPromedioSimple(List<String> claves) {
            double suma = 0;
            int count = 0;
            for (String clave : claves) {
                double valor = getNotaValor(clave);
                if (valor >= 0) {
                    suma += valor;
                    count++;
                }
            }
            return count > 0 ? suma / count : 0;
        }

        public double calcularPromedioPonderado(Map<String, Double> pesos) {
            double sumaPonderada = 0;
            double sumaPesos = 0;
            for (Map.Entry<String, Double> entry : pesos.entrySet()) {
                double valor = getNotaValor(entry.getKey());
                if (valor >= 0) {
                    sumaPonderada += valor * entry.getValue();
                    sumaPesos += entry.getValue();
                }
            }
            return sumaPesos > 0 ? sumaPonderada / sumaPesos : 0;
        }

        public double calcularTotal(List<String> claves) {
            double suma = 0;
            for (String clave : claves) {
                double valor = getNotaValor(clave);
                if (valor >= 0) {
                    suma += valor;
                }
            }
            return suma;
        }

        public double calcularNotaFinal() {
            double suma = 0;
            int count = 0;
            for (String clave : notas.keySet()) {
                double valor = getNotaValor(clave);
                if (valor >= 0) {
                    suma += valor;
                    count++;
                }
            }
            return count > 0 ? suma / count : 0;
        }

        public int getNumero() { return numero; }
        public String getNombre() { return nombre; }
        public String getKeyFirebase() { return keyFirebase; }
        public Map<String, String> getNotas() { return notas; }
    }

    // ============================================================
    // 8. CICLO DE VIDA - onCreate
    // ============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primero);

        inicializarColumnas();
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
        contenedorPrincipal = findViewById(R.id.contenedorPrincipal);
        btnAgregarColumna = findViewById(R.id.btnAgregarColumna);
    }

    private void configurarListeners() {
        // Sincronizar scroll vertical
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

        // Botón de guardar
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            guardarTodosLosCambios();
            finish();
        });

        // Long press en título para guardar
        tvTituloCurso.setOnLongClickListener(v -> {
            guardarTodosLosCambios();
            return true;
        });

        // Botón para agregar columna
        btnAgregarColumna.setOnClickListener(v -> {
            mostrarDialogoAgregarColumna();
        });
    }

    // ============================================================
    // 9. DIÁLOGO PARA AGREGAR COLUMNA
    // ============================================================
    private void mostrarDialogoAgregarColumna() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Agregar Nueva Columna");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputNombre = new EditText(this);
        inputNombre.setHint("Nombre de la columna");
        layout.addView(inputNombre);

        final EditText inputClave = new EditText(this);
        inputClave.setHint("Clave Firebase (ej: nuevaNota)");
        layout.addView(inputClave);

        final android.widget.Spinner spinnerTipo = new android.widget.Spinner(this);
        String[] tipos = {"NOTA", "PROMEDIO", "PROMEDIO_PONDERADO", "TOTAL"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, tipos);
        spinnerTipo.setAdapter(adapter);
        layout.addView(spinnerTipo);

        builder.setView(layout);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String nombre = inputNombre.getText().toString().trim();
            String clave = inputClave.getText().toString().trim();
            String tipoStr = spinnerTipo.getSelectedItem().toString();

            if (!nombre.isEmpty() && !clave.isEmpty()) {
                agregarNuevaColumna(nombre, clave, tipoStr);
            } else {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // ============================================================
    // 10. FUNCIÓN PARA AGREGAR COLUMNA
    // ============================================================
    private void agregarNuevaColumna(String nombre, String clave, String tipoStr) {
        TipoColumna tipo;
        switch (tipoStr) {
            case "PROMEDIO":
                tipo = TipoColumna.PROMEDIO;
                break;
            case "PROMEDIO_PONDERADO":
                tipo = TipoColumna.PROMEDIO_PONDERADO;
                break;
            case "TOTAL":
                tipo = TipoColumna.TOTAL;
                break;
            default:
                tipo = TipoColumna.NOTA;
                break;
        }

        ColumnaConfig nuevaColumna = new ColumnaConfig(
                nombre,
                80,
                tipo,
                clave,
                null
        );

        int posicionInsercion = columnasConfig.size() - 2;
        columnasConfig.add(posicionInsercion, nuevaColumna);

        Toast.makeText(this, "Columna agregada: " + nombre, Toast.LENGTH_SHORT).show();

        if (listaEstudiantes != null) {
            for (Estudiante estudiante : listaEstudiantes) {
                estudiante.setNota(clave, "--");
            }
            crearTablas();
        }
    }

    // ============================================================
    // 11. FIREBASE - CARGA DE DATOS
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
                        for (ColumnaConfig columna : columnasConfig) {
                            if (columna.claveFirebase != null &&
                                    columna.tipo == TipoColumna.NOTA) {
                                DataSnapshot notaSnapshot = calificacionesSnapshot.child(columna.claveFirebase);
                                if (notaSnapshot.exists()) {
                                    Object valor = notaSnapshot.getValue();
                                    estudiante.setNota(columna.claveFirebase,
                                            valor != null ? valor.toString() : "--");
                                }
                            }
                        }
                    }

                    listaEstudiantes.add(estudiante);
                    contador++;
                }

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

    // ============================================================
    // 12. CREACIÓN DE TABLAS
    // ============================================================
    private void crearTablas() {
        tablaNotas.removeAllViews();
        tablaFija.removeAllViews();

        if (listaEstudiantes == null || listaEstudiantes.isEmpty()) {
            mostrarMensajeVacio();
            return;
        }

        crearFilaEncabezados();
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

    private void crearFilaEncabezados() {
        List<ColumnaConfig> columnasDatos = new ArrayList<>();
        for (int i = 2; i < columnasConfig.size(); i++) {
            columnasDatos.add(columnasConfig.get(i));
        }

        // --- ENCABEZADO PARA TABLA FIJA ---
        TableRow filaEncabezadoFija = new TableRow(this);
        filaEncabezadoFija.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));

        TextView tvNumero = crearTextViewEncabezado("N°", false);
        TableRow.LayoutParams paramsNumero = new TableRow.LayoutParams(
                dpToPx(columnasConfig.get(0).ancho), dpToPx(55));
        paramsNumero.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
        tvNumero.setLayoutParams(paramsNumero);
        filaEncabezadoFija.addView(tvNumero);

        TextView tvNombre = crearTextViewEncabezado("APELLIDOS Y NOMBRES", false);
        TableRow.LayoutParams paramsNombre = new TableRow.LayoutParams(
                dpToPx(columnasConfig.get(1).ancho), dpToPx(55));
        paramsNombre.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
        tvNombre.setLayoutParams(paramsNombre);
        filaEncabezadoFija.addView(tvNombre);

        tablaFija.addView(filaEncabezadoFija);

        // --- ENCABEZADO PARA TABLA DE NOTAS ---
        TableRow filaEncabezadoNotas = new TableRow(this);
        filaEncabezadoNotas.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));

        for (ColumnaConfig columna : columnasDatos) {
            TextView textView = crearTextViewEncabezado(columna.titulo, true);
            TableRow.LayoutParams params = new TableRow.LayoutParams(
                    dpToPx(columna.ancho), dpToPx(55));
            params.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
            textView.setLayoutParams(params);
            filaEncabezadoNotas.addView(textView);
        }

        tablaNotas.addView(filaEncabezadoNotas);
    }

    private TextView crearTextViewEncabezado(String texto, boolean multilinea) {
        TextView textView = new TextView(this);
        textView.setText(texto);
        textView.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        textView.setTextSize(11);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        textView.setGravity(Gravity.CENTER);
        if (multilinea && texto.contains("\n")) {
            textView.setSingleLine(false);
            textView.setMaxLines(3);
        }
        return textView;
    }

    private void crearFilaEstudiante(Estudiante estudiante) {
        List<ColumnaConfig> columnasDatos = new ArrayList<>();
        for (int i = 2; i < columnasConfig.size(); i++) {
            columnasDatos.add(columnasConfig.get(i));
        }

        int colorFondo = estudiante.getNumero() % 2 == 0
                ? ContextCompat.getColor(this, R.color.gris_claro)
                : ContextCompat.getColor(this, android.R.color.white);

        // --- FILA FIJA ---
        TableRow filaFija = new TableRow(this);
        filaFija.setBackgroundColor(colorFondo);

        TextView tvNumero = crearTextViewSimple(String.valueOf(estudiante.getNumero()));
        tvNumero.setLayoutParams(new TableRow.LayoutParams(
                dpToPx(columnasConfig.get(0).ancho), dpToPx(40)));
        filaFija.addView(tvNumero);

        TextView tvNombre = crearTextViewSimple(estudiante.getNombre());
        tvNombre.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tvNombre.setPadding(dpToPx(6), 0, dpToPx(6), 0);
        tvNombre.setTextSize(11);
        tvNombre.setLayoutParams(new TableRow.LayoutParams(
                dpToPx(columnasConfig.get(1).ancho), dpToPx(40)));
        filaFija.addView(tvNombre);

        tablaFija.addView(filaFija);

        // --- FILA DE NOTAS ---
        TableRow filaNotas = new TableRow(this);
        filaNotas.setBackgroundColor(colorFondo);

        for (ColumnaConfig columna : columnasDatos) {
            View celda = crearCelda(estudiante, columna);
            celda.setLayoutParams(new TableRow.LayoutParams(
                    dpToPx(columna.ancho), dpToPx(40)));
            filaNotas.addView(celda);
        }

        tablaNotas.addView(filaNotas);
    }

    // ============================================================
    // 13. CREACIÓN DE CELDAS
    // ============================================================
    private View crearCelda(Estudiante estudiante, ColumnaConfig columna) {
        switch (columna.tipo) {
            case NOTA:
                return crearCeldaNota(estudiante, columna);
            case PROMEDIO:
                return crearCeldaPromedio(estudiante, columna);
            case PROMEDIO_PONDERADO:
                return crearCeldaPromedioPonderado(estudiante, columna);
            case TOTAL:
                return crearCeldaTotal(estudiante, columna);
            case NOTA_FINAL:
                return crearCeldaNotaFinal(estudiante, columna);
            default:
                return crearTextViewSimple("--");
        }
    }

    // ============================================================
    // 14. CELDA DE NOTA (EDITABLE)
    // ============================================================
    private View crearCeldaNota(Estudiante estudiante, ColumnaConfig columna) {
        String texto = estudiante.getNota(columna.claveFirebase);
        EditText editText = new EditText(this);
        editText.setText(texto);
        editText.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
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

                String key = estudiante.getKeyFirebase() + "_" + columna.claveFirebase;
                Map<String, Object> cambio = new HashMap<>();
                cambio.put("valor", nuevoValor);
                cambio.put("estudianteKey", estudiante.getKeyFirebase());
                cambio.put("claveFirebase", columna.claveFirebase);
                cambiosPendientes.put(key, cambio);

                estudiante.setNota(columna.claveFirebase, nuevoValor);
                actualizarCeldasCalculadas(estudiante);

                tvTituloCurso.setText("Primero C * " + listaEstudiantes.size() + " alumnos");
            }
        });

        return editText;
    }

    // ============================================================
    // 15. CELDA DE PROMEDIO SIMPLE
    // ============================================================
    private View crearCeldaPromedio(Estudiante estudiante, ColumnaConfig columna) {
        List<String> clavesDependientes = new ArrayList<>();
        if (columna.indicesDependientes != null) {
            for (int idx : columna.indicesDependientes) {
                if (idx < columnasConfig.size()) {
                    String clave = columnasConfig.get(idx).claveFirebase;
                    if (clave != null) {
                        clavesDependientes.add(clave);
                    }
                }
            }
        }

        double promedio = estudiante.calcularPromedioSimple(clavesDependientes);
        TextView textView = crearTextViewCalculado(String.format("%.1f", promedio));
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        return textView;
    }

    // ============================================================
    // 16. CELDA DE PROMEDIO PONDERADO
    // ============================================================
    private View crearCeldaPromedioPonderado(Estudiante estudiante, ColumnaConfig columna) {
        Map<String, Double> pesos = new HashMap<>();

        if (columna.claveFirebase.equals("promedioDimension45")) {
            pesos.put("nroSellosApuntes", 0.1);
            pesos.put("puntajeApuntes", 0.15);
            pesos.put("mapaConceptoTriangulos", 0.15);
            pesos.put("evPlanoCartesiano", 0.15);
            pesos.put("lecturaMatematica", 0.2);
            pesos.put("evaluacionTrimestral", 0.25);
        } else if (columna.claveFirebase.equals("promedioDimension40")) {
            pesos.put("nroSellosPracticas", 0.1);
            pesos.put("puntajePracticas", 0.15);
            pesos.put("practicasLibroSudoku", 0.2);
            pesos.put("evAngulosRectosLlanos", 0.25);
            pesos.put("divNumerosEnteros", 0.3);
        }

        double promedio = estudiante.calcularPromedioPonderado(pesos);
        TextView textView = crearTextViewCalculado(String.format("%.1f", promedio));
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_purple));
        return textView;
    }

    // ============================================================
    // 17. CELDA DE TOTAL
    // ============================================================
    private View crearCeldaTotal(Estudiante estudiante, ColumnaConfig columna) {
        List<String> clavesNotas = new ArrayList<>();
        for (ColumnaConfig col : columnasConfig) {
            if (col.tipo == TipoColumna.NOTA && col.claveFirebase != null) {
                clavesNotas.add(col.claveFirebase);
            }
        }

        double total = estudiante.calcularTotal(clavesNotas);
        TextView textView = crearTextViewCalculado(String.format("%.1f", total));
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        return textView;
    }

    // ============================================================
    // 18. CELDA DE NOTA FINAL
    // ============================================================
    private View crearCeldaNotaFinal(Estudiante estudiante, ColumnaConfig columna) {
        double notaFinal = estudiante.calcularNotaFinal();
        TextView textView = crearTextViewCalculado(String.format("%.1f", notaFinal));
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        return textView;
    }

    // ============================================================
    // 19. ACTUALIZAR CELDAS CALCULADAS
    // ============================================================
    private void actualizarCeldasCalculadas(Estudiante estudiante) {
        crearTablas();
    }

    // ============================================================
    // 20. UTILIDADES
    // ============================================================
    private TextView crearTextViewSimple(String texto) {
        TextView textView = new TextView(this);
        textView.setText(texto);
        textView.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
        textView.setTextSize(11);
        textView.setGravity(Gravity.CENTER);
        textView.setSingleLine(true);
        return textView;
    }

    private TextView crearTextViewCalculado(String texto) {
        TextView textView = new TextView(this);
        textView.setText(texto);
        textView.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
        textView.setTextSize(12);
        textView.setGravity(Gravity.CENTER);
        textView.setSingleLine(true);
        textView.setBackgroundResource(android.R.drawable.edit_text);
        textView.setEnabled(false);
        return textView;
    }

    // ============================================================
    // 21. FILA DE TOTALES
    // ============================================================
    private void crearFilaTotales() {
        List<ColumnaConfig> columnasDatos = new ArrayList<>();
        for (int i = 2; i < columnasConfig.size(); i++) {
            columnasDatos.add(columnasConfig.get(i));
        }

        int colorFondo = ContextCompat.getColor(this, R.color.color_secundario);

        // --- FILA TOTALES FIJA ---
        TableRow filaTotalesFija = new TableRow(this);
        filaTotalesFija.setBackgroundColor(colorFondo);

        TextView tvTotales = crearTextViewSimple("PROMEDIOS");
        tvTotales.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvTotales.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotales.setLayoutParams(new TableRow.LayoutParams(
                dpToPx(columnasConfig.get(0).ancho), dpToPx(40)));
        filaTotalesFija.addView(tvTotales);

        TextView tvVacia = crearTextViewSimple("");
        tvVacia.setLayoutParams(new TableRow.LayoutParams(
                dpToPx(columnasConfig.get(1).ancho), dpToPx(40)));
        filaTotalesFija.addView(tvVacia);

        tablaFija.addView(filaTotalesFija);

        // --- FILA TOTALES DE NOTAS ---
        TableRow filaTotalesNotas = new TableRow(this);
        filaTotalesNotas.setBackgroundColor(colorFondo);

        for (ColumnaConfig columna : columnasDatos) {
            if (columna.tipo == TipoColumna.NOTA) {
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
                TextView tvProm = crearTextViewSimple(String.format("%.1f", promedio));
                tvProm.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                tvProm.setTypeface(null, android.graphics.Typeface.BOLD);
                tvProm.setLayoutParams(new TableRow.LayoutParams(
                        dpToPx(columna.ancho), dpToPx(40)));
                filaTotalesNotas.addView(tvProm);
            } else {
                TextView tvVaciaCalc = crearTextViewSimple("");
                tvVaciaCalc.setLayoutParams(new TableRow.LayoutParams(
                        dpToPx(columna.ancho), dpToPx(40)));
                filaTotalesNotas.addView(tvVaciaCalc);
            }
        }

        tablaNotas.addView(filaTotalesNotas);
    }

    // ============================================================
    // 22. GUARDAR EN FIREBASE
    // ============================================================
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
}