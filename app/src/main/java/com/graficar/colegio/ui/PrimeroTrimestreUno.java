package com.graficar.colegio.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
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
import java.util.Locale;
import java.util.Map;

public class PrimeroTrimestreUno extends AppCompatActivity {

    // ============================================================
    // UI
    // ============================================================

    private TableLayout tablaNotas;
    private TableLayout tablaFija;
    private ScrollView scrollVertical;
    private HorizontalScrollView scrollHorizontal;
    private ScrollView scrollFijaVertical;
    private TextView tvTituloCurso;
    private Button btnGuardar;

    // ============================================================
    // DATOS
    // ============================================================

    private List<Estudiante> listaEstudiantes = new ArrayList<>();
    private DatabaseReference dbRef;
    private final Map<String, Map<String, Object>> cambiosPendientes = new HashMap<>();
    private final Map<String, String> mapaObservaciones = new HashMap<>();
    private boolean hayCambios = false;

    // ============================================================
    // CONFIGURACIÓN
    // ============================================================

    private static final int ANCHO_NUMERO = 30;
    private static final int ANCHO_NOMBRE = 180;
    private static final int ANCHO_NOTA = 65;
    private static final int ANCHO_OBSERVACIONES = 200;

    private static final int ALTURA_FILA = 40;
    private static final int ALTURA_ENCABEZADO = 35;

    private final String gradoActual = "primeroC";
    private final String trimestreActual = "trimestre1";
    private final String materiaActual = "matematica";

    // ============================================================
    // COLUMNAS
    // ============================================================

    private static class ColumnaConfig {
        String titulo;
        String claveFirebase;
        boolean editable;

        ColumnaConfig(String titulo, String claveFirebase, boolean editable) {
            this.titulo = titulo;
            this.claveFirebase = claveFirebase;
            this.editable = editable;
        }
    }

    private final List<ColumnaConfig> columnas = new ArrayList<>();

    private void inicializarColumnas() {
        columnas.clear();
        columnas.add(new ColumnaConfig("SER", "ser", true));
        columnas.add(new ColumnaConfig("SABER", "saber", true));
        columnas.add(new ColumnaConfig("HACER", "hacer", true));
        columnas.add(new ColumnaConfig("PROMEDIO", "promedio", false));
        columnas.add(new ColumnaConfig("TOTAL", "total", false));
        columnas.add(new ColumnaConfig("AUTOEV.", "autoevaluacion", true));
        columnas.add(new ColumnaConfig("NOTA PARCIAL", "nota parcial", false));
        columnas.add(new ColumnaConfig("PONDERACIÓN", "ponderacion", true));
        columnas.add(new ColumnaConfig("NOTA TRIMESTRAL", "nota trimestral", false));
    }

    // ============================================================
    // ESTUDIANTE
    // ============================================================

    private static class Estudiante {
        private final int numero;
        private final String nombre;
        private final String uid;
        private final Map<String, String> notas = new HashMap<>();

        Estudiante(int numero, String nombre, String uid) {
            this.numero = numero;
            this.nombre = nombre;
            this.uid = uid;
            String[] claves = {
                    "ser", "saber", "hacer", "promedio", "total",
                    "autoevaluacion", "nota parcial", "ponderacion", "nota trimestral"
            };
            for (String clave : claves) {
                notas.put(clave, "--");
            }
        }

        public void setNota(String clave, String valor) {
            notas.put(clave, valor == null ? "--" : valor);
        }

        public String getNota(String clave) {
            return notas.getOrDefault(clave, "--");
        }

        public double getNotaValor(String clave) {
            String valor = notas.get(clave);
            if (valor == null || valor.equals("--") || valor.trim().isEmpty()) {
                return -1;
            }
            try {
                return Double.parseDouble(valor.trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        public int getNumero() { return numero; }
        public String getNombre() { return nombre; }
        public String getUid() { return uid; }
    }

    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primero_trimestre_uno);
        inicializarColumnas();
        inicializarViews();
        configurarListeners();
        cargarDatos();
    }

    // ============================================================
    // VIEWS
    // ============================================================

    private void inicializarViews() {
        tablaNotas = findViewById(R.id.tablaNotas);
        tablaFija = findViewById(R.id.tablaFija);
        scrollVertical = findViewById(R.id.scrollVertical);
        scrollHorizontal = findViewById(R.id.scrollHorizontal);
        scrollFijaVertical = findViewById(R.id.scrollFijaVertical);
        tvTituloCurso = findViewById(R.id.tvTituloCurso);
        btnGuardar = findViewById(R.id.btnGuardar);
    }

    // ============================================================
    // LISTENERS
    // ============================================================

    private void configurarListeners() {
        if (scrollVertical != null && scrollFijaVertical != null) {
            scrollVertical.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                scrollFijaVertical.scrollTo(0, scrollY);
            });
            scrollFijaVertical.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                scrollVertical.scrollTo(0, scrollY);
            });
        }

        if (btnGuardar != null) {
            btnGuardar.setOnClickListener(v -> guardarTodosLosCambios());
        }

        if (tvTituloCurso != null) {
            tvTituloCurso.setOnLongClickListener(v -> {
                guardarTodosLosCambios();
                return true;
            });
        }
    }

    // ============================================================
    // CARGAR DATOS
    // ============================================================

    private void cargarDatos() {
        dbRef = FirebaseDatabase.getInstance().getReference();
        cargarObservaciones();
    }

    // ============================================================
    // OBSERVACIONES
    // ============================================================

    private void cargarObservaciones() {
        dbRef.child("observaciones").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mapaObservaciones.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot uidSnapshot : snapshot.getChildren()) {
                        String uid = uidSnapshot.getKey();
                        if (uid == null) continue;
                        DataSnapshot trimestreSnapshot = uidSnapshot.child(trimestreActual);
                        if (trimestreSnapshot.exists()) {
                            String texto = trimestreSnapshot.child("texto").getValue(String.class);
                            if (texto != null && !texto.isEmpty()) {
                                mapaObservaciones.put(uid, texto);
                            }
                        }
                    }
                }
                cargarEstudiantes();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE", "Error observaciones: " + error.getMessage());
                cargarEstudiantes();
            }
        });
    }

    // ============================================================
    // ESTUDIANTES
    // ============================================================

    private void cargarEstudiantes() {
        dbRef.child("estudiantes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaEstudiantes.clear();
                int contador = 1;
                if (!snapshot.exists()) {
                    Toast.makeText(PrimeroTrimestreUno.this, "No hay estudiantes", Toast.LENGTH_SHORT).show();
                    mostrarMensajeVacio();
                    return;
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    String nombre = child.child("nombre").getValue(String.class);
                    if (uid != null && nombre != null && !nombre.trim().isEmpty()) {
                        Estudiante estudiante = new Estudiante(contador, nombre, uid);
                        listaEstudiantes.add(estudiante);
                        contador++;
                    }
                }

                cargarCalificaciones();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE", "Error estudiantes: " + error.getMessage());
                Toast.makeText(PrimeroTrimestreUno.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ============================================================
    // CARGAR CALIFICACIONES - CORREGIDO
    // ============================================================

    private void cargarCalificaciones() {
        dbRef.child("calificaciones").child(gradoActual).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot uidSnapshot : snapshot.getChildren()) {
                        String uid = uidSnapshot.getKey();
                        Estudiante estudiante = buscarEstudiantePorUid(uid);
                        if (estudiante == null) continue;

                        DataSnapshot materiaSnapshot = uidSnapshot
                                .child(trimestreActual)
                                .child(materiaActual);

                        if (materiaSnapshot.exists()) {
                            // Cargar SOLO campos EDITABLES
                            cargarNota(materiaSnapshot, estudiante, "ser");
                            cargarNota(materiaSnapshot, estudiante, "saber");
                            cargarNota(materiaSnapshot, estudiante, "hacer");
                            cargarNota(materiaSnapshot, estudiante, "autoevaluacion");
                            cargarPonderacion(materiaSnapshot, estudiante);

                            // Calcular TODAS las notas
                            calcularNotas(estudiante);
                        }
                    }
                }
                runOnUiThread(() -> {
                    actualizarTituloInicial();
                    crearTablas();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE", "Error calificaciones: " + error.getMessage());
                runOnUiThread(() -> crearTablas());
            }
        });
    }

    // ============================================================
    // CARGAR UNA NOTA
    // ============================================================

    private void cargarNota(DataSnapshot materiaSnapshot, Estudiante estudiante, String clave) {
        DataSnapshot notaSnapshot = materiaSnapshot.child(clave);
        if (notaSnapshot.exists()) {
            Object valor = notaSnapshot.getValue();
            if (valor != null) {
                estudiante.setNota(clave, String.valueOf(valor));
                Log.d("FIREBASE", "Cargado " + clave + " = " + valor + " para " + estudiante.getNombre());
            }
        }
    }

    // ============================================================
    // CARGAR PONDERACIÓN
    // ============================================================

    private void cargarPonderacion(DataSnapshot materiaSnapshot, Estudiante estudiante) {
        DataSnapshot ponderacionSnapshot = materiaSnapshot.child("ponderacion");
        if (ponderacionSnapshot.exists()) {
            Object valor = ponderacionSnapshot.getValue();
            if (valor != null) {
                String valorStr;
                if (valor instanceof Long) {
                    valorStr = String.valueOf(((Long) valor).intValue());
                } else if (valor instanceof Double) {
                    valorStr = String.format(Locale.US, "%.1f", (Double) valor);
                } else {
                    valorStr = String.valueOf(valor);
                }
                estudiante.setNota("ponderacion", valorStr);
                Log.d("FIREBASE", "Cargado ponderacion = " + valorStr + " para " + estudiante.getNombre());
            }
        }
    }

    // ============================================================
    // BUSCAR ESTUDIANTE
    // ============================================================

    private Estudiante buscarEstudiantePorUid(String uid) {
        if (uid == null) return null;
        for (Estudiante estudiante : listaEstudiantes) {
            if (uid.equals(estudiante.getUid())) {
                return estudiante;
            }
        }
        return null;
    }

    // ============================================================
    // CREAR TABLAS
    // ============================================================

    private void crearTablas() {
        if (tablaNotas != null) tablaNotas.removeAllViews();
        if (tablaFija != null) tablaFija.removeAllViews();

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

    // ============================================================
    // MENSAJE VACÍO
    // ============================================================

    private void mostrarMensajeVacio() {
        if (tablaNotas == null) return;
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
        if (tablaFija == null || tablaNotas == null) return;

        // ============================================
        // ENCABEZADO FIJO
        // ============================================
        TableRow filaEncabezadoFija = new TableRow(this);
        filaEncabezadoFija.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));
        TextView tvNumero = crearTextViewEncabezado("N°", ANCHO_NUMERO);
        filaEncabezadoFija.addView(tvNumero);
        TextView tvApellidos = crearTextViewEncabezado("APELLIDOS Y NOMBRES", ANCHO_NOMBRE);
        filaEncabezadoFija.addView(tvApellidos);
        tablaFija.addView(filaEncabezadoFija);

        // ============================================
        // ENCABEZADO NOTAS
        // ============================================
        TableRow filaEncabezadoNotas = new TableRow(this);
        filaEncabezadoNotas.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));
        for (ColumnaConfig columna : columnas) {
            TextView tv = crearTextViewEncabezado(columna.titulo, ANCHO_NOTA);
            filaEncabezadoNotas.addView(tv);
        }
        TextView tvObs = crearTextViewEncabezado("OBSERVACIONES (T1)", ANCHO_OBSERVACIONES);
        filaEncabezadoNotas.addView(tvObs);
        tablaNotas.addView(filaEncabezadoNotas);
    }

    // ============================================================
    // CREAR TEXTVIEW ENCABEZADO
    // ============================================================

    private TextView crearTextViewEncabezado(String texto, int ancho) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(10);
        tv.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new TableRow.LayoutParams(dpToPx(ancho), dpToPx(ALTURA_ENCABEZADO)));
        return tv;
    }

    // ============================================================
    // FILA ESTUDIANTE
    // ============================================================

    private void crearFilaEstudiante(Estudiante estudiante) {
        if (tablaFija == null || tablaNotas == null) return;

        int colorFondo = estudiante.getNumero() % 2 == 0
                ? ContextCompat.getColor(this, R.color.gris_claro)
                : ContextCompat.getColor(this, android.R.color.white);

        // ========================================================
        // TABLA FIJA
        // ========================================================
        TableRow filaFija = new TableRow(this);
        filaFija.setBackgroundColor(colorFondo);

        TextView tvNumero = new TextView(this);
        tvNumero.setText(String.valueOf(estudiante.getNumero()));
        tvNumero.setTextSize(12);
        tvNumero.setGravity(Gravity.CENTER);
        tvNumero.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NUMERO), dpToPx(ALTURA_FILA)));
        filaFija.addView(tvNumero);

        TextView tvNombre = new TextView(this);
        tvNombre.setText(estudiante.getNombre());
        tvNombre.setTextSize(11);
        tvNombre.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tvNombre.setPadding(dpToPx(8), 0, dpToPx(8), 0);
        tvNombre.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOMBRE), dpToPx(ALTURA_FILA)));
        filaFija.addView(tvNombre);

        tablaFija.addView(filaFija);

        // ========================================================
        // FILA NOTAS
        // ========================================================
        TableRow filaNotas = new TableRow(this);
        filaNotas.setBackgroundColor(colorFondo);

        // Crear columnas
        for (int indice = 0; indice < columnas.size(); indice++) {
            ColumnaConfig columna = columnas.get(indice);
            String valor = estudiante.getNota(columna.claveFirebase);

            if (columna.editable) {
                // ================================================
                // EDITABLE
                // ================================================
                EditText editText = new EditText(this);
                editText.setText(valor);
                editText.setTextSize(11);
                editText.setGravity(Gravity.CENTER);
                editText.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
                editText.setBackgroundResource(android.R.drawable.edit_text);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editText.setSingleLine(true);
                editText.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));

                // ================================================
                // TEXT WATCHER
                // ================================================
                final String claveFinal = columna.claveFirebase;
                editText.addTextChangedListener(new TextWatcher() {
                    private boolean modificando = false;

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (modificando) return;

                        String nuevoValor = s.toString().trim();
                        if (nuevoValor.isEmpty()) {
                            nuevoValor = "--";
                        }

                        // ====================================
                        // VALIDACIÓN PARA AUTOEVALUACIÓN
                        // ====================================
                        if (claveFinal.equals("autoevaluacion") && !nuevoValor.equals("--")) {
                            try {
                                double valorAuto = Double.parseDouble(nuevoValor);
                                if (valorAuto > 5) {
                                    nuevoValor = "5";
                                    modificando = true;
                                    editText.setText("5");
                                    editText.setSelection(editText.length());
                                    modificando = false;
                                    Toast.makeText(PrimeroTrimestreUno.this,
                                            "La autoevaluación máxima es 5", Toast.LENGTH_SHORT).show();
                                }
                                if (valorAuto < 0) {
                                    nuevoValor = "0";
                                    modificando = true;
                                    editText.setText("0");
                                    editText.setSelection(editText.length());
                                    modificando = false;
                                }
                            } catch (NumberFormatException e) {
                                nuevoValor = "--";
                            }
                        }

                        // ====================================
                        // VALIDACIÓN PARA PONDERACIÓN
                        // ====================================
                        if (claveFinal.equals("ponderacion") && !nuevoValor.equals("--")) {
                            try {
                                double valorPond = Double.parseDouble(nuevoValor);
                                if (valorPond > 10) {
                                    nuevoValor = "10";
                                    modificando = true;
                                    editText.setText("10");
                                    editText.setSelection(editText.length());
                                    modificando = false;
                                    Toast.makeText(PrimeroTrimestreUno.this,
                                            "La ponderación máxima es 10", Toast.LENGTH_SHORT).show();
                                }
                                if (valorPond < 0) {
                                    nuevoValor = "0";
                                    modificando = true;
                                    editText.setText("0");
                                    editText.setSelection(editText.length());
                                    modificando = false;
                                }
                            } catch (NumberFormatException e) {
                                nuevoValor = "--";
                            }
                        }

                        // ====================================
                        // GUARDAR EN ESTUDIANTE
                        // ====================================
                        estudiante.setNota(claveFinal, nuevoValor);

                        // ====================================
                        // REGISTRAR CAMBIO EN PENDIENTES
                        // ====================================
                        String key = estudiante.getUid() + "_" + claveFinal;
                        Map<String, Object> cambio = new HashMap<>();
                        cambio.put("valor", nuevoValor);
                        cambio.put("uid", estudiante.getUid());
                        cambio.put("claveFirebase", claveFinal);
                        cambiosPendientes.put(key, cambio);
                        hayCambios = true;

                        // ====================================
                        // RECALCULAR TODAS LAS NOTAS
                        // ====================================
                        calcularNotas(estudiante);

                        // ====================================
                        // GUARDAR TODOS LOS CAMPOS CALCULADOS EN FIREBASE
                        // ====================================
                        guardarCamposCalculados(estudiante);

                        // ====================================
                        // ACTUALIZAR COLUMNAS CALCULADAS EN UI
                        // ====================================
                        actualizarColumnasCalculadas(filaNotas, estudiante);
                        actualizarTituloCambios();
                    }
                });

                filaNotas.addView(editText);

            } else {
                // ================================================
                // COLUMNA CALCULADA (NO EDITABLE)
                // ================================================
                TextView tv = new TextView(this);
                tv.setText(valor);
                tv.setTextSize(11);
                tv.setGravity(Gravity.CENTER);
                if (columna.claveFirebase.equals("promedio") ||
                        columna.claveFirebase.equals("nota trimestral") ||
                        columna.claveFirebase.equals("nota parcial")) {
                    tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                tv.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));
                filaNotas.addView(tv);
            }
        }

        // ========================================================
        // OBSERVACIÓN
        // ========================================================
        String observacion = mapaObservaciones.getOrDefault(estudiante.getUid(), "");
        EditText editTextObs = new EditText(this);
        editTextObs.setText(observacion);
        editTextObs.setTextSize(11);
        editTextObs.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        editTextObs.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
        editTextObs.setBackgroundResource(android.R.drawable.edit_text);
        editTextObs.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editTextObs.setSingleLine(false);
        editTextObs.setMaxLines(3);
        editTextObs.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_OBSERVACIONES), dpToPx(ALTURA_FILA)));

        editTextObs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String texto = s.toString().trim();
                guardarCambioObservacion(estudiante.getUid(), texto);
            }
        });

        filaNotas.addView(editTextObs);
        tablaNotas.addView(filaNotas);
    }

    // ============================================================
    // GUARDAR TODOS LOS CAMPOS CALCULADOS EN FIREBASE
    // ============================================================

    private void guardarCamposCalculados(Estudiante estudiante) {
        // Guardar PROMEDIO
        String promedio = estudiante.getNota("promedio");
        if (!promedio.equals("--")) {
            guardarNotaEnFirebase(estudiante.getUid(), "promedio", promedio);
        }

        // Guardar TOTAL
        String total = estudiante.getNota("total");
        if (!total.equals("--")) {
            guardarNotaEnFirebase(estudiante.getUid(), "total", total);
        }

        // Guardar NOTA PARCIAL
        String notaParcial = estudiante.getNota("nota parcial");
        if (!notaParcial.equals("--")) {
            guardarNotaEnFirebase(estudiante.getUid(), "nota parcial", notaParcial);
        }

        // Guardar NOTA TRIMESTRAL
        String notaTrimestral = estudiante.getNota("nota trimestral");
        if (!notaTrimestral.equals("--")) {
            guardarNotaEnFirebase(estudiante.getUid(), "nota trimestral", notaTrimestral);
        }
    }

    // ============================================================
    // GUARDAR NOTA EN FIREBASE
    // ============================================================

    private void guardarNotaEnFirebase(String uid, String clave, String valor) {
        DatabaseReference ref = dbRef.child("calificaciones")
                .child(gradoActual)
                .child(uid)
                .child(trimestreActual)
                .child(materiaActual)
                .child(clave);

        try {
            double numValor = Double.parseDouble(valor);
            ref.setValue(numValor)
                    .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Guardado calculado: " + uid + " - " + clave + " = " + numValor))
                    .addOnFailureListener(e -> Log.e("FIREBASE", "Error guardando calculado: " + e.getMessage()));
        } catch (NumberFormatException e) {
            ref.setValue(valor);
        }
    }

    // ============================================================
    // ACTUALIZAR COLUMNAS CALCULADAS EN UI
    // ============================================================

    private void actualizarColumnasCalculadas(TableRow filaNotas, Estudiante estudiante) {
        for (int i = 0; i < columnas.size(); i++) {
            ColumnaConfig columna = columnas.get(i);
            if (columna.editable) continue;
            if (i >= filaNotas.getChildCount()) continue;

            android.view.View vista = filaNotas.getChildAt(i);
            if (vista instanceof TextView && !(vista instanceof EditText)) {
                TextView tv = (TextView) vista;
                tv.setText(estudiante.getNota(columna.claveFirebase));
            }
        }
    }

    // ============================================================
    // CAMBIO OBSERVACIÓN
    // ============================================================

    private void guardarCambioObservacion(String uid, String texto) {
        String key = uid + "_observacion";
        Map<String, Object> cambio = new HashMap<>();
        cambio.put("valor", texto);
        cambio.put("uid", uid);
        cambio.put("claveFirebase", "observacion");
        cambiosPendientes.put(key, cambio);
        hayCambios = true;
        actualizarTituloCambios();
    }

    // ============================================================
    // TÍTULO
    // ============================================================

    private void actualizarTituloInicial() {
        if (tvTituloCurso == null) return;
        int conNotas = 0;
        for (Estudiante estudiante : listaEstudiantes) {
            boolean tieneNotas = !estudiante.getNota("ser").equals("--")
                    || !estudiante.getNota("saber").equals("--")
                    || !estudiante.getNota("hacer").equals("--");
            if (tieneNotas) conNotas++;
        }
        tvTituloCurso.setText("PRIMERO C - MATEMÁTICA (TRIMESTRE 1) - "
                + listaEstudiantes.size() + " alumnos (" + conNotas + " con notas)");
    }

    private void actualizarTituloCambios() {
        if (tvTituloCurso == null) return;
        tvTituloCurso.setText("PRIMERO C - MATEMÁTICA * (" + cambiosPendientes.size() + " cambios)");
    }

    // ============================================================
    // CALCULAR NOTAS - TODOS LOS CAMPOS
    // ============================================================

    private void calcularNotas(Estudiante estudiante) {
        double ser = estudiante.getNotaValor("ser");
        double saber = estudiante.getNotaValor("saber");
        double hacer = estudiante.getNotaValor("hacer");
        double autoevaluacion = estudiante.getNotaValor("autoevaluacion");

        // ========================================================
        // 1. PROMEDIO (Ser + Saber + Hacer) / 3
        // ========================================================
        if (ser >= 0 && saber >= 0 && hacer >= 0) {
            double promedio = (ser + saber + hacer) / 3.0;
            estudiante.setNota("promedio", formatear(promedio));

            // ====================================================
            // 2. TOTAL SOBRE 95
            // ====================================================
            double total = (promedio / 100.0) * 95.0;
            estudiante.setNota("total", formatear(total));
        } else {
            estudiante.setNota("promedio", "--");
            estudiante.setNota("total", "--");
        }

        // ========================================================
        // 3. NOTA PARCIAL = TOTAL + AUTOEVALUACION
        // ========================================================
        double total = estudiante.getNotaValor("total");
        if (autoevaluacion < 0) autoevaluacion = 0;
        if (autoevaluacion > 5) autoevaluacion = 5;

        if (total >= 0) {
            double notaParcial = total + autoevaluacion;
            estudiante.setNota("nota parcial", formatear(notaParcial));
        } else {
            estudiante.setNota("nota parcial", "--");
        }

        // ========================================================
        // 4. NOTA TRIMESTRAL = NOTA PARCIAL + PONDERACION
        // ========================================================
        double notaParcial = estudiante.getNotaValor("nota parcial");
        double ponderacion = estudiante.getNotaValor("ponderacion");

        if (notaParcial >= 0 && ponderacion >= 0) {
            if (ponderacion < 0) ponderacion = 0;
            if (ponderacion > 10) ponderacion = 10;

            double notaTrimestral = notaParcial + ponderacion;
            if (notaTrimestral > 100) notaTrimestral = 100;

            estudiante.setNota("nota trimestral", String.format(Locale.US, "%.1f", notaTrimestral));
        } else {
            estudiante.setNota("nota trimestral", "--");
        }
    }

    // ============================================================
    // FORMATO DECIMAL
    // ============================================================

    private String formatear(double numero) {
        return String.format(Locale.US, "%.1f", numero);
    }

    // ============================================================
    // FILA TOTALES
    // ============================================================

    private void crearFilaTotales() {
        if (tablaFija == null || tablaNotas == null) return;

        int colorFondo = ContextCompat.getColor(this, R.color.color_secundario);

        // ========================================================
        // PARTE FIJA
        // ========================================================
        TableRow filaTotalesFija = new TableRow(this);
        filaTotalesFija.setBackgroundColor(colorFondo);

        TextView tvTotales = new TextView(this);
        tvTotales.setText("PROMEDIOS");
        tvTotales.setTextSize(12);
        tvTotales.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvTotales.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotales.setGravity(Gravity.CENTER);
        tvTotales.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NUMERO), dpToPx(ALTURA_FILA)));
        filaTotalesFija.addView(tvTotales);

        TextView tvVacia = new TextView(this);
        tvVacia.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOMBRE), dpToPx(ALTURA_FILA)));
        filaTotalesFija.addView(tvVacia);

        tablaFija.addView(filaTotalesFija);

        // ========================================================
        // PARTE NOTAS
        // ========================================================
        TableRow filaTotalesNotas = new TableRow(this);
        filaTotalesNotas.setBackgroundColor(colorFondo);

        for (ColumnaConfig columna : columnas) {
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
            tvProm.setText(count > 0 ? formatear(promedio) : "--");
            tvProm.setTextSize(11);
            tvProm.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvProm.setTypeface(null, android.graphics.Typeface.BOLD);
            tvProm.setGravity(Gravity.CENTER);
            tvProm.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));
            filaTotalesNotas.addView(tvProm);
        }

        TextView tvObsVacia = new TextView(this);
        tvObsVacia.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_OBSERVACIONES), dpToPx(ALTURA_FILA)));
        filaTotalesNotas.addView(tvObsVacia);

        tablaNotas.addView(filaTotalesNotas);
    }

    // ============================================================
    // GUARDAR OBSERVACIÓN
    // ============================================================

    private void guardarObservacion(String uid, String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            dbRef.child("observaciones")
                    .child(uid)
                    .child(trimestreActual)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Observación eliminada: " + uid))
                    .addOnFailureListener(e -> Log.e("FIREBASE", "Error eliminar: " + e.getMessage()));
            return;
        }

        DatabaseReference ref = dbRef.child("observaciones")
                .child(uid)
                .child(trimestreActual);
        Map<String, Object> datos = new HashMap<>();
        datos.put("texto", texto);
        ref.setValue(datos)
                .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Observación guardada: " + uid))
                .addOnFailureListener(e -> Log.e("FIREBASE", "Error: " + e.getMessage()));
    }

    // ============================================================
    // GUARDAR TODOS LOS CAMBIOS
    // ============================================================

    private void guardarTodosLosCambios() {
        if (cambiosPendientes.isEmpty()) {
            Toast.makeText(this, "No hay cambios para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        int cantidad = cambiosPendientes.size();
        Toast.makeText(this, "Guardando " + cantidad + " cambios...", Toast.LENGTH_SHORT).show();

        for (Map.Entry<String, Map<String, Object>> entry : cambiosPendientes.entrySet()) {
            Map<String, Object> cambio = entry.getValue();
            String uid = (String) cambio.get("uid");
            String claveFirebase = (String) cambio.get("claveFirebase");
            String valor = (String) cambio.get("valor");

            if (uid == null || claveFirebase == null) continue;

            // ====================================================
            // OBSERVACIÓN
            // ====================================================
            if (claveFirebase.equals("observacion")) {
                guardarObservacion(uid, valor);
                continue;
            }

            // ====================================================
            // CALIFICACIÓN
            // ====================================================
            DatabaseReference ref = dbRef.child("calificaciones")
                    .child(gradoActual)
                    .child(uid)
                    .child(trimestreActual)
                    .child(materiaActual)
                    .child(claveFirebase);

            if (valor == null || valor.equals("--") || valor.trim().isEmpty()) {
                ref.removeValue()
                        .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Eliminado: " + uid + " - " + claveFirebase))
                        .addOnFailureListener(e -> Log.e("FIREBASE", "Error eliminando: " + e.getMessage()));
            } else {
                try {
                    double numValor = Double.parseDouble(valor);
                    ref.setValue(numValor)
                            .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Guardado: " + uid + " - " + claveFirebase + " = " + numValor))
                            .addOnFailureListener(e -> Log.e("FIREBASE", "Error guardando: " + e.getMessage()));
                } catch (NumberFormatException e) {
                    ref.setValue(valor);
                }
            }
        }

        cambiosPendientes.clear();
        hayCambios = false;
        actualizarTituloInicial();
        Toast.makeText(this, "Todos los cambios guardados", Toast.LENGTH_SHORT).show();
    }

    // ============================================================
    // DP A PX
    // ============================================================

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}