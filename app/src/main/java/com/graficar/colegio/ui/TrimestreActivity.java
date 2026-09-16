package com.graficar.colegio.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

public class TrimestreActivity extends AppCompatActivity {

    // ============================================================
    // UI
    // ============================================================

    private LinearLayout tablaNotas;
    private LinearLayout tablaFija;
    private ScrollView scrollVertical;
    private HorizontalScrollView scrollHorizontal;
    private ScrollView scrollFijaVertical;
    private TextView tvTituloCurso;
    private Button btnGuardar;

    // ============================================================
    // ESCALAS
    // ============================================================

    private static final double MAX_SER_HIJA = 10.0;
    private static final double MAX_SER = 5.0;

    private static final double MAX_SABER_HIJA = 45.0;
    private static final double MAX_SABER = 45.0;

    private static final double MAX_HACER_HIJA = 40.0;
    private static final double MAX_HACER = 40.0;

    private static final double MAX_AUTOEVALUACION = 5.0;
    private static final double MAX_PONDERACION = 10.0;
    private static final double MAX_NOTA_FINAL = 100.0;
    private static final double MAX_TOTAL = 90.0;

    // ============================================================
    // DATOS
    // ============================================================

    private List<Estudiante> listaEstudiantes = new ArrayList<>();
    private DatabaseReference dbRef;

    private final Map<String, Map<String, Object>> cambiosPendientes = new HashMap<>();
    private final Map<String, String> mapaObservaciones = new HashMap<>();
    private boolean hayCambios = false;

    // ============================================================
    // CONFIGURACIÓN DE TAMAÑOS
    // ============================================================

    private static final int ANCHO_NUMERO = 30;
    private static final int ANCHO_NOMBRE = 180;
    private static final int ANCHO_NOTA = 65;
    private static final int ANCHO_OBSERVACIONES = 200;

    private static final int ALTURA_FILA = 40;
    private static final int ALTURA_ENCABEZADO = 35;

    // ============================================================
    // VARIABLES DINÁMICAS (vienen del Intent)
    // ============================================================

    private String gradoActual;         // "primeroC"
    private String trimestreActual;     // "trimestre1" / "trimestre2" / "trimestre3"
    private String materiaActual;       // "matematica" / "lenguaje" / "historia"
    private String nombreProfesor;      // opcional, para mostrar
    private String idLocalProfesor;     // opcional

    // ============================================================
    // COLUMNAS FIJAS
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
        columnas.add(new ColumnaConfig("TOTAL", "total", false));
        columnas.add(new ColumnaConfig("AUTOEV.", "autoevaluacion", true));
        columnas.add(new ColumnaConfig("NOTA PARCIAL", "nota parcial", false));
        columnas.add(new ColumnaConfig("PONDERACIÓN", "ponderacion", true));
        columnas.add(new ColumnaConfig("NOTA TRIMESTRAL", "nota trimestral", false));
    }

    // ============================================================
    // COLUMNAS PADRE
    // ============================================================

    private static class SubColumna {
        String id;
        String titulo;

        SubColumna(String id, String titulo) {
            this.id = id;
            this.titulo = titulo;
        }
    }

    private static class ColumnaPadreEditable {
        final String id;
        String titulo;
        final List<SubColumna> hijas = new ArrayList<>();

        ColumnaPadreEditable(String id, String titulo) {
            this.id = id;
            this.titulo = titulo;
        }
    }

    private final List<ColumnaPadreEditable> padresEditables = new ArrayList<>();

    private void inicializarPadresEditables() {
        padresEditables.clear();
        padresEditables.add(new ColumnaPadreEditable("ser", "SER"));
        padresEditables.add(new ColumnaPadreEditable("saber", "SABER"));
        padresEditables.add(new ColumnaPadreEditable("hacer", "HACER"));
    }

    // ============================================================
    // ESTUDIANTE
    // ============================================================

    private static class Estudiante {
        private final int numero;
        private final String nombre;
        private final String uid;
        private final Map<String, String> notas = new HashMap<>();
        private final Map<String, Map<String, String>> componentes = new HashMap<>();

        Estudiante(int numero, String nombre, String uid) {
            this.numero = numero;
            this.nombre = nombre;
            this.uid = uid;
            String[] claves = {
                    "ser", "saber", "hacer", "total",
                    "autoevaluacion", "nota parcial",
                    "ponderacion", "nota trimestral"
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
            if (valor == null || valor.equals("--") || valor.trim().isEmpty()) return -1;
            try {
                return Double.parseDouble(valor.trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        public void setComponente(String padreId, String hijaId, String valor) {
            componentes.computeIfAbsent(padreId, k -> new HashMap<>())
                    .put(hijaId, valor == null ? "--" : valor);
        }

        public String getComponente(String padreId, String hijaId) {
            Map<String, String> mapa = componentes.get(padreId);
            if (mapa == null) return "--";
            return mapa.getOrDefault(hijaId, "--");
        }

        public double getComponenteValor(String padreId, String hijaId) {
            String valor = getComponente(padreId, hijaId);
            if (valor == null || valor.equals("--") || valor.trim().isEmpty()) return -1;
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
        setContentView(R.layout.activity_trimestre);

        // 1. Leer datos del Intent
        leerIntent();

        // 2. Validar
        if (gradoActual == null || gradoActual.isEmpty()) {
            Toast.makeText(this, "Falta el grado", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (materiaActual == null || materiaActual.isEmpty()) {
            Toast.makeText(this, "Falta la materia", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (trimestreActual == null || trimestreActual.isEmpty()) {
            Toast.makeText(this, "Falta el trimestre", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d("TrimestreActivity", "Grado: " + gradoActual);
        Log.d("TrimestreActivity", "Materia: " + materiaActual);
        Log.d("TrimestreActivity", "Trimestre: " + trimestreActual);

        // 3. Inicializar
        inicializarColumnas();
        inicializarViews();
        configurarListeners();
        cargarDatos();
    }

    // ============================================================
    // LEER INTENT
    // ============================================================

    private void leerIntent() {
        gradoActual = normalizarGradoKey(
                getIntent().getStringExtra("grado"));
        materiaActual = normalizarMateriaKey(
                getIntent().getStringExtra("materia"));
        trimestreActual = getIntent().getStringExtra("trimestre");
        nombreProfesor = getIntent().getStringExtra("nombreProfesor");
        idLocalProfesor = getIntent().getStringExtra("idLocalProfesor");
    }

    private String normalizarGradoKey(String grado) {
        if (grado == null) return null;
        String[] partes = grado.trim().split("\\s+");
        if (partes.length < 2) return grado.toLowerCase();
        return partes[0].toLowerCase() + partes[1].toUpperCase();
    }

    private String normalizarMateriaKey(String materia) {
        if (materia == null) return null;
        return materia.toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u");
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
            scrollVertical.setOnScrollChangeListener(
                    (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                            scrollFijaVertical.scrollTo(0, scrollY));

            scrollFijaVertical.setOnScrollChangeListener(
                    (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                            scrollVertical.scrollTo(0, scrollY));
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
        inicializarPadresEditables();
        cargarEstructuraColumnas();
    }

    // ============================================================
    // ESTRUCTURA DE COLUMNAS
    // ============================================================

    private void cargarEstructuraColumnas() {
        dbRef.child("estructura_columnas")
                .child(gradoActual)
                .child(materiaActual)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (ColumnaPadreEditable padre : padresEditables) {
                            padre.hijas.clear();
                            DataSnapshot padreSnap = snapshot.child(padre.id);
                            if (padreSnap.exists()) {
                                for (DataSnapshot hijaSnap : padreSnap.getChildren()) {
                                    String id = hijaSnap.getKey();
                                    String titulo = hijaSnap.getValue(String.class);
                                    if (id != null && titulo != null) {
                                        padre.hijas.add(new SubColumna(id, titulo));
                                    }
                                }
                            }
                        }
                        cargarObservaciones();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FIREBASE", "Error estructura columnas: " + error.getMessage());
                        cargarObservaciones();
                    }
                });
    }

    private void guardarEstructuraColumna(ColumnaPadreEditable padre, SubColumna hija, boolean eliminar) {
        DatabaseReference ref = dbRef.child("estructura_columnas")
                .child(gradoActual)
                .child(materiaActual)
                .child(padre.id)
                .child(hija.id);

        if (eliminar) {
            ref.removeValue();
        } else {
            ref.setValue(hija.titulo);
        }
    }

    // ============================================================
    // OBSERVACIONES
    // ============================================================

    private void cargarObservaciones() {
        dbRef.child("observaciones")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        mapaObservaciones.clear();

                        if (snapshot.exists()) {
                            for (DataSnapshot uidSnapshot : snapshot.getChildren()) {
                                String uid = uidSnapshot.getKey();
                                if (uid == null) continue;

                                // Nueva estructura: observaciones/{uid}/{fecha}/{obsId}
                                // Tomamos la observación más reciente de cualquier fecha
                                String texto = obtenerUltimaObservacion(uidSnapshot);

                                if (texto != null && !texto.isEmpty()) {
                                    mapaObservaciones.put(uid, texto);
                                }
                            }
                        }

                        cargarEstudiantes();
                    }

                    private String obtenerUltimaObservacion(DataSnapshot uidSnapshot) {
                        String ultimoTexto = null;
                        String ultimaFecha = "";

                        // Recorrer fechas
                        for (DataSnapshot fechaSnap : uidSnapshot.getChildren()) {
                            String fecha = fechaSnap.getKey();
                            if (fecha == null) continue;

                            // Recorrer observaciones de esa fecha
                            for (DataSnapshot obsSnap : fechaSnap.getChildren()) {
                                String texto = obsSnap.child("texto").getValue(String.class);
                                if (texto != null && !texto.isEmpty()) {
                                    // Si esta fecha es más reciente que la última, guardar
                                    if (fecha.compareTo(ultimaFecha) > 0) {
                                        ultimaFecha = fecha;
                                        ultimoTexto = texto;
                                    }
                                }
                            }
                        }
                        return ultimoTexto;
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
        dbRef.child("estudiantes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaEstudiantes.clear();
                        int contador = 1;

                        if (!snapshot.exists()) {
                            Toast.makeText(TrimestreActivity.this,
                                    "No hay estudiantes", Toast.LENGTH_SHORT).show();
                            mostrarMensajeVacio();
                            return;
                        }

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String uid = child.getKey();
                            String nombre = child.child("nombre").getValue(String.class);
                            String grado = child.child("grado").getValue(String.class);

                            // Filtrar por grado actual
                            if (uid != null && nombre != null && !nombre.trim().isEmpty()
                                    && grado != null && grado.equals(gradoActual)) {
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
                        Toast.makeText(TrimestreActivity.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ============================================================
    // CARGAR CALIFICACIONES
    // ============================================================

    private void cargarCalificaciones() {
        dbRef.child("calificaciones")
                .child(gradoActual)
                .addListenerForSingleValueEvent(new ValueEventListener() {
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
                                    cargarNota(materiaSnapshot, estudiante, "autoevaluacion");
                                    cargarPonderacion(materiaSnapshot, estudiante);

                                    for (ColumnaPadreEditable padre : padresEditables) {
                                        if (padre.hijas.isEmpty()) {
                                            cargarNota(materiaSnapshot, estudiante, padre.id);
                                        } else {
                                            cargarComponentesPadre(materiaSnapshot, estudiante, padre);
                                            recalcularNotaPadre(estudiante, padre);
                                        }
                                    }

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
    // CARGAR COMPONENTES PADRE
    // ============================================================

    private void cargarComponentesPadre(DataSnapshot materiaSnapshot, Estudiante estudiante, ColumnaPadreEditable padre) {
        DataSnapshot componentesSnap = materiaSnapshot.child(padre.id + "_componentes");
        for (SubColumna hija : padre.hijas) {
            DataSnapshot valorSnap = componentesSnap.child(hija.id);
            if (valorSnap.exists()) {
                Object valor = valorSnap.getValue();
                if (valor != null) {
                    estudiante.setComponente(padre.id, hija.id, String.valueOf(valor));
                }
            }
        }
    }

    // ============================================================
    // CARGAR NOTA
    // ============================================================

    private void cargarNota(DataSnapshot materiaSnapshot, Estudiante estudiante, String clave) {
        DataSnapshot notaSnapshot = materiaSnapshot.child(clave);
        if (notaSnapshot.exists()) {
            Object valor = notaSnapshot.getValue();
            if (valor != null) {
                estudiante.setNota(clave, String.valueOf(valor));
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
            }
        }
    }

    // ============================================================
    // BUSCAR ESTUDIANTE
    // ============================================================

    private Estudiante buscarEstudiantePorUid(String uid) {
        if (uid == null) return null;
        for (Estudiante estudiante : listaEstudiantes) {
            if (uid.equals(estudiante.getUid())) return estudiante;
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

        LinearLayout filaEncabezadoFija = crearFilaHorizontal();
        filaEncabezadoFija.setBackgroundColor(
                ContextCompat.getColor(this, R.color.curso_primero));

        filaEncabezadoFija.addView(crearTextViewEncabezado(
                "N°", ANCHO_NUMERO, ALTURA_ENCABEZADO * 2));
        filaEncabezadoFija.addView(crearTextViewEncabezado(
                "APELLIDOS Y NOMBRES", ANCHO_NOMBRE, ALTURA_ENCABEZADO * 2));

        tablaFija.addView(filaEncabezadoFija);

        LinearLayout filaEncabezadoNotas = crearFilaHorizontal();
        filaEncabezadoNotas.setBackgroundColor(
                ContextCompat.getColor(this, R.color.curso_primero));

        for (ColumnaPadreEditable padre : padresEditables) {
            filaEncabezadoNotas.addView(crearCeldaEncabezadoPadre(padre));
        }

        for (ColumnaConfig columna : columnas) {
            filaEncabezadoNotas.addView(crearTextViewEncabezado(
                    columna.titulo, ANCHO_NOTA, ALTURA_ENCABEZADO * 2));
        }

        // Encabezado de observaciones dinámico
        filaEncabezadoNotas.addView(crearTextViewEncabezado(
                "OBSERVACIONES (" + obtenerAbreviaturaTrimestre() + ")",
                ANCHO_OBSERVACIONES,
                ALTURA_ENCABEZADO * 2));

        tablaNotas.addView(filaEncabezadoNotas);
    }

    private String obtenerAbreviaturaTrimestre() {
        if (trimestreActual == null) return "";
        switch (trimestreActual) {
            case "trimestre1": return "T1";
            case "trimestre2": return "T2";
            case "trimestre3": return "T3";
            default: return trimestreActual.toUpperCase();
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private LinearLayout crearFilaHorizontal() {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        return fila;
    }

    private TextView crearTextViewEncabezado(String texto, int ancho, int alto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(10);
        tv.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(ancho), dpToPx(alto)));
        return tv;
    }

    // ============================================================
    // ENCABEZADO PADRE
    // ============================================================

    private View crearCeldaEncabezadoPadre(ColumnaPadreEditable padre) {
        int anchoTotal = padre.hijas.isEmpty()
                ? ANCHO_NOTA
                : (padre.hijas.size() + 1) * ANCHO_NOTA;

        LinearLayout contenedor = new LinearLayout(this);
        contenedor.setOrientation(LinearLayout.VERTICAL);
        contenedor.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(anchoTotal), dpToPx(ALTURA_ENCABEZADO * 2)));

        LinearLayout filaTitulo = new LinearLayout(this);
        filaTitulo.setOrientation(LinearLayout.HORIZONTAL);
        filaTitulo.setGravity(Gravity.CENTER_VERTICAL);
        filaTitulo.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));
        filaTitulo.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(anchoTotal), dpToPx(ALTURA_ENCABEZADO)));

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(padre.titulo);
        tvTitulo.setTextSize(10);
        tvTitulo.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitulo.setGravity(Gravity.CENTER);
        tvTitulo.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        filaTitulo.addView(tvTitulo);

        Button btnAgregar = new Button(this);
        btnAgregar.setText("+");
        btnAgregar.setTextSize(12);
        btnAgregar.setPadding(0, 0, 0, 0);
        btnAgregar.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28)));
        btnAgregar.setOnClickListener(v -> mostrarDialogoAgregarHija(padre));
        filaTitulo.addView(btnAgregar);

        contenedor.addView(filaTitulo);

        if (padre.hijas.isEmpty()) return contenedor;

        LinearLayout filaHijas = new LinearLayout(this);
        filaHijas.setOrientation(LinearLayout.HORIZONTAL);
        filaHijas.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(anchoTotal), dpToPx(ALTURA_ENCABEZADO)));

        for (SubColumna hija : padre.hijas) {
            TextView tvHija = new TextView(this);
            tvHija.setText(hija.titulo);
            tvHija.setTextSize(9);
            tvHija.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvHija.setGravity(Gravity.CENTER);
            tvHija.setBackgroundColor(ContextCompat.getColor(this, R.color.color_secundario));
            tvHija.setLayoutParams(new LinearLayout.LayoutParams(
                    dpToPx(ANCHO_NOTA), dpToPx(ALTURA_ENCABEZADO)));
            tvHija.setOnLongClickListener(v -> {
                mostrarDialogoEliminarHija(padre, hija);
                return true;
            });
            filaHijas.addView(tvHija);
        }

        TextView tvPromedio = new TextView(this);
        tvPromedio.setText("PROMEDIO");
        tvPromedio.setTextSize(9);
        tvPromedio.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvPromedio.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPromedio.setGravity(Gravity.CENTER);
        tvPromedio.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        tvPromedio.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(ANCHO_NOTA), dpToPx(ALTURA_ENCABEZADO)));
        filaHijas.addView(tvPromedio);

        contenedor.addView(filaHijas);
        return contenedor;
    }

    // ============================================================
    // FILA ESTUDIANTE
    // ============================================================

    private void crearFilaEstudiante(Estudiante estudiante) {
        if (tablaFija == null || tablaNotas == null) return;

        int colorFondo = estudiante.getNumero() % 2 == 0
                ? ContextCompat.getColor(this, R.color.gris_claro)
                : ContextCompat.getColor(this, android.R.color.white);

        LinearLayout filaFija = crearFilaHorizontal();
        filaFija.setBackgroundColor(colorFondo);

        TextView tvNumero = new TextView(this);
        tvNumero.setText(String.valueOf(estudiante.getNumero()));
        tvNumero.setTextSize(12);
        tvNumero.setGravity(Gravity.CENTER);
        tvNumero.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(ANCHO_NUMERO), dpToPx(ALTURA_FILA)));
        filaFija.addView(tvNumero);

        TextView tvNombre = new TextView(this);
        tvNombre.setText(estudiante.getNombre());
        tvNombre.setTextSize(11);
        tvNombre.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tvNombre.setPadding(dpToPx(8), 0, dpToPx(8), 0);
        tvNombre.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(ANCHO_NOMBRE), dpToPx(ALTURA_FILA)));
        filaFija.addView(tvNombre);

        tablaFija.addView(filaFija);

        LinearLayout filaNotas = crearFilaHorizontal();
        filaNotas.setBackgroundColor(colorFondo);

        for (ColumnaPadreEditable padre : padresEditables) {
            agregarCeldasPadre(filaNotas, padre, estudiante);
        }

        for (ColumnaConfig columna : columnas) {
            String valor = estudiante.getNota(columna.claveFirebase);

            if (columna.editable) {
                filaNotas.addView(crearEditTextNota(estudiante, columna.claveFirebase, valor));
            } else {
                TextView tv = new TextView(this);
                tv.setText(valor);
                tv.setTextSize(11);
                tv.setGravity(Gravity.CENTER);

                if (columna.claveFirebase.equals("nota trimestral")
                        || columna.claveFirebase.equals("nota parcial")
                        || columna.claveFirebase.equals("total")) {
                    tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                }

                tv.setLayoutParams(new LinearLayout.LayoutParams(
                        dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));
                tv.setTag(columna.claveFirebase);
                filaNotas.addView(tv);
            }
        }

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
        editTextObs.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(ANCHO_OBSERVACIONES), dpToPx(ALTURA_FILA)));

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
    // CELDAS PADRE
    // ============================================================

    private void agregarCeldasPadre(LinearLayout filaNotas, ColumnaPadreEditable padre, Estudiante estudiante) {
        if (padre.hijas.isEmpty()) {
            EditText editText = crearEditTextNota(estudiante, padre.id, estudiante.getNota(padre.id));
            filaNotas.addView(editText);
            return;
        }

        LinearLayout grupo = new LinearLayout(this);
        grupo.setOrientation(LinearLayout.HORIZONTAL);
        grupo.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx((padre.hijas.size() + 1) * ANCHO_NOTA),
                dpToPx(ALTURA_FILA)));

        for (SubColumna hija : padre.hijas) {
            grupo.addView(crearEditTextComponente(estudiante, padre, hija));
        }

        TextView tvPromedio = new TextView(this);
        tvPromedio.setText(estudiante.getNota(padre.id));
        tvPromedio.setTextSize(11);
        tvPromedio.setGravity(Gravity.CENTER);
        tvPromedio.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPromedio.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        tvPromedio.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));
        tvPromedio.setTag(padre.id);
        grupo.addView(tvPromedio);

        filaNotas.addView(grupo);
    }

    private ViewGroup obtenerFilaNotas(View vista) {
        ViewParent parent = vista.getParent();

        while (parent != null) {
            if (parent == tablaNotas) return null;
            if (parent instanceof ViewGroup) {
                ViewGroup grupo = (ViewGroup) parent;
                if (grupo.getParent() == tablaNotas) return grupo;
            }
            parent = parent.getParent();
        }
        return null;
    }

    // ============================================================
    // EDIT TEXT NOTA DIRECTA
    // ============================================================

    private EditText crearEditTextNota(Estudiante estudiante, String clave, String valorInicial) {
        EditText editText = new EditText(this);
        editText.setText(valorInicial);
        editText.setTextSize(11);
        editText.setGravity(Gravity.CENTER);
        editText.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        editText.setBackgroundResource(android.R.drawable.edit_text);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setSingleLine(true);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));

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
                if (nuevoValor.isEmpty()) nuevoValor = "--";

                // Validar autoevaluación
                if (clave.equals("autoevaluacion") && !nuevoValor.equals("--")) {
                    try {
                        double valorAuto = Double.parseDouble(nuevoValor);
                        if (valorAuto > MAX_AUTOEVALUACION) {
                            nuevoValor = formatear(MAX_AUTOEVALUACION);
                            modificando = true;
                            editText.setText(nuevoValor);
                            editText.setSelection(editText.length());
                            modificando = false;
                            Toast.makeText(TrimestreActivity.this,
                                    "La autoevaluación máxima es 5",
                                    Toast.LENGTH_SHORT).show();
                        }
                        if (valorAuto < 0) {
                            nuevoValor = "0";
                            modificando = true;
                            editText.setText(nuevoValor);
                            editText.setSelection(editText.length());
                            modificando = false;
                        }
                    } catch (NumberFormatException e) {
                        nuevoValor = "--";
                    }
                }

                // Validar ponderación
                if (clave.equals("ponderacion") && !nuevoValor.equals("--")) {
                    try {
                        double valorPond = Double.parseDouble(nuevoValor);
                        if (valorPond > MAX_PONDERACION) {
                            nuevoValor = formatear(MAX_PONDERACION);
                            modificando = true;
                            editText.setText(nuevoValor);
                            editText.setSelection(editText.length());
                            modificando = false;
                            Toast.makeText(TrimestreActivity.this,
                                    "La ponderación máxima es 10",
                                    Toast.LENGTH_SHORT).show();
                        }
                        if (valorPond < 0) {
                            nuevoValor = "0";
                            modificando = true;
                            editText.setText(nuevoValor);
                            editText.setSelection(editText.length());
                            modificando = false;
                        }
                    } catch (NumberFormatException e) {
                        nuevoValor = "--";
                    }
                }

                // Validar padres directos sin hijas
                double maximoPadre = obtenerMaximoPadre(clave);
                if (maximoPadre > 0
                        && (clave.equals("ser") || clave.equals("saber") || clave.equals("hacer"))
                        && !nuevoValor.equals("--")) {
                    try {
                        double valor = Double.parseDouble(nuevoValor);
                        if (valor > maximoPadre) {
                            nuevoValor = formatear(maximoPadre);
                            modificando = true;
                            editText.setText(nuevoValor);
                            editText.setSelection(editText.length());
                            modificando = false;
                            Toast.makeText(TrimestreActivity.this,
                                    "El máximo de " + clave.toUpperCase() + " es " + formatear(maximoPadre),
                                    Toast.LENGTH_SHORT).show();
                        }
                        if (valor < 0) {
                            nuevoValor = "0";
                            modificando = true;
                            editText.setText(nuevoValor);
                            editText.setSelection(editText.length());
                            modificando = false;
                        }
                    } catch (NumberFormatException e) {
                        nuevoValor = "--";
                    }
                }

                estudiante.setNota(clave, nuevoValor);

                String key = estudiante.getUid() + "_" + clave;
                Map<String, Object> cambio = new HashMap<>();
                cambio.put("valor", nuevoValor);
                cambio.put("uid", estudiante.getUid());
                cambio.put("claveFirebase", clave);
                cambiosPendientes.put(key, cambio);
                hayCambios = true;

                calcularNotas(estudiante);
                guardarCamposCalculados(estudiante);

                ViewGroup fila = obtenerFilaNotas(editText);
                actualizarCeldasCalculadas(fila, estudiante);
                actualizarTituloCambios();
            }
        });

        return editText;
    }

    private double obtenerMaximoPadre(String padreId) {
        switch (padreId) {
            case "ser": return MAX_SER;
            case "saber": return MAX_SABER;
            case "hacer": return MAX_HACER;
            default: return 0;
        }
    }

    private double obtenerMaximoHija(String padreId) {
        switch (padreId) {
            case "ser": return MAX_SER_HIJA;
            case "saber": return MAX_SABER_HIJA;
            case "hacer": return MAX_HACER_HIJA;
            default: return 0;
        }
    }

    // ============================================================
    // EDIT TEXT COMPONENTE HIJA
    // ============================================================

    private EditText crearEditTextComponente(Estudiante estudiante, ColumnaPadreEditable padre, SubColumna hija) {
        EditText editText = new EditText(this);
        editText.setText(estudiante.getComponente(padre.id, hija.id));
        editText.setTextSize(11);
        editText.setGravity(Gravity.CENTER);
        editText.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        editText.setBackgroundResource(android.R.drawable.edit_text);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setSingleLine(true);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));

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
                if (nuevoValor.isEmpty()) nuevoValor = "--";

                double maximo = obtenerMaximoHija(padre.id);

                if (!nuevoValor.equals("--")) {
                    try {
                        double valor = Double.parseDouble(nuevoValor);
                        if (valor > maximo) {
                            nuevoValor = formatear(maximo);
                            modificando = true;
                            editText.setText(nuevoValor);
                            editText.setSelection(editText.length());
                            modificando = false;
                            Toast.makeText(TrimestreActivity.this,
                                    "La nota máxima de " + padre.titulo + " es " + formatear(maximo),
                                    Toast.LENGTH_SHORT).show();
                        }
                        if (valor < 0) {
                            nuevoValor = "0";
                            modificando = true;
                            editText.setText(nuevoValor);
                            editText.setSelection(editText.length());
                            modificando = false;
                        }
                    } catch (NumberFormatException e) {
                        nuevoValor = "--";
                    }
                }

                estudiante.setComponente(padre.id, hija.id, nuevoValor);

                String key = estudiante.getUid() + "_" + padre.id + "_" + hija.id;
                Map<String, Object> cambio = new HashMap<>();
                cambio.put("valor", nuevoValor);
                cambio.put("uid", estudiante.getUid());
                cambio.put("claveFirebase", padre.id + "_componentes/" + hija.id);
                cambiosPendientes.put(key, cambio);
                hayCambios = true;

                recalcularNotaPadre(estudiante, padre);
                calcularNotas(estudiante);
                guardarCamposCalculados(estudiante);

                ViewGroup fila = obtenerFilaNotas(editText);
                actualizarCeldasCalculadas(fila, estudiante);
                actualizarTituloCambios();
            }
        });

        return editText;
    }

    // ============================================================
    // AGREGAR / ELIMINAR HIJA
    // ============================================================

    private void mostrarDialogoAgregarHija(ColumnaPadreEditable padre) {
        EditText input = new EditText(this);
        input.setHint("Ej: Participación");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Agregar columna a " + padre.titulo)
                .setView(input)
                .setPositiveButton("AGREGAR", (dialog, which) -> {
                    String nombre = input.getText().toString().trim();
                    if (nombre.isEmpty()) return;

                    String id = generarIdHija(nombre);
                    for (SubColumna h : padre.hijas) {
                        if (h.id.equals(id)) {
                            Toast.makeText(this, "Ya existe una columna con ese nombre",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    SubColumna nueva = new SubColumna(id, nombre.toUpperCase(Locale.getDefault()));
                    padre.hijas.add(nueva);
                    guardarEstructuraColumna(padre, nueva, false);

                    for (Estudiante e : listaEstudiantes) {
                        recalcularNotaPadre(e, padre);
                        calcularNotas(e);
                        guardarCamposCalculados(e);
                    }

                    crearTablas();
                })
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    private void mostrarDialogoEliminarHija(ColumnaPadreEditable padre, SubColumna hija) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar columna")
                .setMessage("¿Eliminar \"" + hija.titulo + "\" de " + padre.titulo
                        + "? Se perderán las notas guardadas en esta columna para todos los estudiantes.")
                .setPositiveButton("ELIMINAR", (dialog, which) -> {
                    padre.hijas.remove(hija);
                    guardarEstructuraColumna(padre, hija, true);

                    for (Estudiante e : listaEstudiantes) {
                        recalcularNotaPadre(e, padre);
                        calcularNotas(e);
                        guardarCamposCalculados(e);
                    }

                    crearTablas();
                })
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    private String generarIdHija(String nombre) {
        String id = nombre.toLowerCase(Locale.getDefault())
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[.#$\\[\\]/]", "");
        return id.isEmpty() ? "col_" + System.currentTimeMillis() : id;
    }

    // ============================================================
    // CALCULAR PROMEDIO PADRE
    // ============================================================

    private double calcularPromedioPadre(Estudiante estudiante, ColumnaPadreEditable padre) {
        if (padre.hijas.isEmpty()) {
            return estudiante.getNotaValor(padre.id);
        }

        double suma = 0;
        int count = 0;
        for (SubColumna hija : padre.hijas) {
            double valor = estudiante.getComponenteValor(padre.id, hija.id);
            if (valor >= 0) {
                suma += valor;
                count++;
            }
        }
        return count > 0 ? suma / count : -1;
    }

    private void recalcularNotaPadre(Estudiante estudiante, ColumnaPadreEditable padre) {
        double promedio = calcularPromedioPadre(estudiante, padre);

        if (promedio < 0) {
            estudiante.setNota(padre.id, "--");
            return;
        }

        double maximoHija = obtenerMaximoHija(padre.id);
        double maximoPadre = obtenerMaximoPadre(padre.id);

        double notaPadre = (promedio / maximoHija) * maximoPadre;

        if (notaPadre < 0) notaPadre = 0;
        if (notaPadre > maximoPadre) notaPadre = maximoPadre;

        estudiante.setNota(padre.id, formatear(notaPadre));
    }

    // ============================================================
    // GUARDAR CAMPOS CALCULADOS
    // ============================================================

    private void guardarCamposCalculados(Estudiante estudiante) {
        for (ColumnaPadreEditable padre : padresEditables) {
            if (!padre.hijas.isEmpty()) {
                String valor = estudiante.getNota(padre.id);
                if (!valor.equals("--")) {
                    guardarNotaEnFirebase(estudiante.getUid(), padre.id, valor);
                }
            }
        }

        String total = estudiante.getNota("total");
        if (!total.equals("--")) {
            guardarNotaEnFirebase(estudiante.getUid(), "total", total);
        }

        String notaParcial = estudiante.getNota("nota parcial");
        if (!notaParcial.equals("--")) {
            guardarNotaEnFirebase(estudiante.getUid(), "nota parcial", notaParcial);
        }

        String notaTrimestral = estudiante.getNota("nota trimestral");
        if (!notaTrimestral.equals("--")) {
            guardarNotaEnFirebase(estudiante.getUid(), "nota trimestral", notaTrimestral);
        }
    }

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
                    .addOnSuccessListener(aVoid -> Log.d("FIREBASE",
                            "Guardado calculado: " + uid + " - " + clave + " = " + numValor))
                    .addOnFailureListener(e -> Log.e("FIREBASE",
                            "Error guardando calculado: " + e.getMessage()));
        } catch (NumberFormatException e) {
            ref.setValue(valor);
        }
    }

    // ============================================================
    // ACTUALIZAR CELDAS CALCULADAS
    // ============================================================

    private void actualizarCeldasCalculadas(ViewGroup contenedor, Estudiante estudiante) {
        if (contenedor == null) return;

        for (int i = 0; i < contenedor.getChildCount(); i++) {
            View vista = contenedor.getChildAt(i);

            if (vista instanceof ViewGroup) {
                actualizarCeldasCalculadas((ViewGroup) vista, estudiante);
            } else if (vista instanceof TextView && !(vista instanceof EditText)) {
                Object tag = vista.getTag();
                if (tag instanceof String) {
                    ((TextView) vista).setText(estudiante.getNota((String) tag));
                }
            }
        }
    }

    // ============================================================
    // OBSERVACIÓN
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
    // TÍTULOS DINÁMICOS
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

        String nombreTrimestre = formatearTrimestre(trimestreActual);
        String nombreMateria = capitalizar(materiaActual);
        String nombreGrado = formatearGrado(gradoActual);

        tvTituloCurso.setText(
                nombreGrado + " - " + nombreMateria + " (" + nombreTrimestre + ") - "
                        + listaEstudiantes.size() + " alumnos ("
                        + conNotas + " con notas)"
        );
    }

    private void actualizarTituloCambios() {
        if (tvTituloCurso == null) return;

        String nombreMateria = capitalizar(materiaActual);
        String nombreGrado = formatearGrado(gradoActual);

        tvTituloCurso.setText(
                nombreGrado + " - " + nombreMateria + " * ("
                        + cambiosPendientes.size() + " cambios)"
        );
    }

    private String formatearTrimestre(String trimestre) {
        if (trimestre == null) return "";
        switch (trimestre) {
            case "trimestre1": return "TRIMESTRE 1";
            case "trimestre2": return "TRIMESTRE 2";
            case "trimestre3": return "TRIMESTRE 3";
            default: return trimestre.toUpperCase();
        }
    }

    private String formatearGrado(String gradoKey) {
        if (gradoKey == null || gradoKey.isEmpty()) return "";
        if (gradoKey.length() < 2) return gradoKey.toUpperCase();
        String numero = gradoKey.substring(0, gradoKey.length() - 1);
        String letra = gradoKey.substring(gradoKey.length() - 1);
        return (numero + " " + letra).toUpperCase();
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return "";
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }

    // ============================================================
    // CALCULAR NOTAS
    // ============================================================

    private void calcularNotas(Estudiante estudiante) {
        double ser = estudiante.getNotaValor("ser");
        double saber = estudiante.getNotaValor("saber");
        double hacer = estudiante.getNotaValor("hacer");

        // TOTAL
        if (ser >= 0 && saber >= 0 && hacer >= 0) {
            double total = ser + saber + hacer;
            if (total > MAX_TOTAL) total = MAX_TOTAL;
            estudiante.setNota("total", formatear(total));
        } else {
            estudiante.setNota("total", "--");
        }

        // AUTOEVALUACIÓN
        double autoevaluacion = estudiante.getNotaValor("autoevaluacion");
        if (autoevaluacion < 0) autoevaluacion = 0;
        if (autoevaluacion > MAX_AUTOEVALUACION) autoevaluacion = MAX_AUTOEVALUACION;

        // NOTA PARCIAL
        double total = estudiante.getNotaValor("total");
        if (total >= 0) {
            double notaParcial = total + autoevaluacion;
            double maximoParcial = MAX_TOTAL + MAX_AUTOEVALUACION;
            if (notaParcial > maximoParcial) notaParcial = maximoParcial;
            estudiante.setNota("nota parcial", formatear(notaParcial));
        } else {
            estudiante.setNota("nota parcial", "--");
        }

        // PONDERACIÓN
        double notaParcial = estudiante.getNotaValor("nota parcial");
        double ponderacion = estudiante.getNotaValor("ponderacion");
        if (ponderacion < 0) ponderacion = 0;
        if (ponderacion > MAX_PONDERACION) ponderacion = MAX_PONDERACION;

        // NOTA TRIMESTRAL
        if (notaParcial >= 0) {
            double notaTrimestral = notaParcial + ponderacion;
            if (notaTrimestral > MAX_NOTA_FINAL) notaTrimestral = MAX_NOTA_FINAL;
            estudiante.setNota("nota trimestral", formatear(notaTrimestral));
        } else {
            estudiante.setNota("nota trimestral", "--");
        }
    }

    private String formatear(double numero) {
        return String.format(Locale.US, "%.1f", numero);
    }

    // ============================================================
    // FILA TOTALES
    // ============================================================

    private void crearFilaTotales() {
        if (tablaFija == null || tablaNotas == null) return;

        int colorFondo = ContextCompat.getColor(this, R.color.color_secundario);

        LinearLayout filaTotalesFija = crearFilaHorizontal();
        filaTotalesFija.setBackgroundColor(colorFondo);

        TextView tvTotales = new TextView(this);
        tvTotales.setText("PROMEDIOS");
        tvTotales.setTextSize(12);
        tvTotales.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvTotales.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotales.setGravity(Gravity.CENTER);
        tvTotales.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(ANCHO_NUMERO), dpToPx(ALTURA_FILA)));
        filaTotalesFija.addView(tvTotales);

        TextView tvVacia = new TextView(this);
        tvVacia.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(ANCHO_NOMBRE), dpToPx(ALTURA_FILA)));
        filaTotalesFija.addView(tvVacia);

        tablaFija.addView(filaTotalesFija);

        LinearLayout filaTotalesNotas = crearFilaHorizontal();
        filaTotalesNotas.setBackgroundColor(colorFondo);

        for (ColumnaPadreEditable padre : padresEditables) {
            double suma = 0;
            int count = 0;
            for (Estudiante estudiante : listaEstudiantes) {
                double valor = estudiante.getNotaValor(padre.id);
                if (valor >= 0) {
                    suma += valor;
                    count++;
                }
            }
            double promedio = count > 0 ? suma / count : 0;
            int ancho = padre.hijas.isEmpty()
                    ? ANCHO_NOTA
                    : (padre.hijas.size() + 1) * ANCHO_NOTA;

            TextView tvProm = new TextView(this);
            tvProm.setText(count > 0 ? formatear(promedio) : "--");
            tvProm.setTextSize(11);
            tvProm.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvProm.setTypeface(null, android.graphics.Typeface.BOLD);
            tvProm.setGravity(Gravity.CENTER);
            tvProm.setLayoutParams(new LinearLayout.LayoutParams(
                    dpToPx(ancho), dpToPx(ALTURA_FILA)));
            filaTotalesNotas.addView(tvProm);
        }

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
            tvProm.setLayoutParams(new LinearLayout.LayoutParams(
                    dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));
            filaTotalesNotas.addView(tvProm);
        }

        TextView tvObsVacia = new TextView(this);
        tvObsVacia.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(ANCHO_OBSERVACIONES), dpToPx(ALTURA_FILA)));
        filaTotalesNotas.addView(tvObsVacia);

        tablaNotas.addView(filaTotalesNotas);
    }

    // ============================================================
    // GUARDAR OBSERVACIÓN
    // ============================================================

    private void guardarObservacion(String uid, String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            // No eliminar si está vacío; simplemente no hacer nada
            return;
        }

        // Guardar en la nueva estructura: observaciones/{uid}/{fecha}/obs_XXX
        String fecha = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new java.util.Date());

        DatabaseReference ref = dbRef.child("observaciones")
                .child(uid)
                .child(fecha);

        // Generar un ID único para esta observación
        String obsId = ref.push().getKey();
        if (obsId == null) return;

        Map<String, Object> datos = new HashMap<>();
        datos.put("materia", materiaActual);
        datos.put("profesorId", idLocalProfesor != null ? idLocalProfesor : "desconocido");
        datos.put("texto", texto);

        ref.child(obsId).setValue(datos)
                .addOnSuccessListener(aVoid -> Log.d("FIREBASE",
                        "Observación guardada: " + uid + " - " + obsId))
                .addOnFailureListener(e -> Log.e("FIREBASE",
                        "Error: " + e.getMessage()));
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

            if (claveFirebase.equals("observacion")) {
                guardarObservacion(uid, valor);
                continue;
            }

            DatabaseReference ref = dbRef.child("calificaciones")
                    .child(gradoActual)
                    .child(uid)
                    .child(trimestreActual)
                    .child(materiaActual)
                    .child(claveFirebase);

            if (valor == null || valor.equals("--") || valor.trim().isEmpty()) {
                ref.removeValue()
                        .addOnSuccessListener(aVoid -> Log.d("FIREBASE",
                                "Eliminado: " + uid + " - " + claveFirebase))
                        .addOnFailureListener(e -> Log.e("FIREBASE",
                                "Error eliminando: " + e.getMessage()));
            } else {
                try {
                    double numValor = Double.parseDouble(valor);
                    ref.setValue(numValor)
                            .addOnSuccessListener(aVoid -> Log.d("FIREBASE",
                                    "Guardado: " + uid + " - " + claveFirebase + " = " + numValor))
                            .addOnFailureListener(e -> Log.e("FIREBASE",
                                    "Error guardando: " + e.getMessage()));
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