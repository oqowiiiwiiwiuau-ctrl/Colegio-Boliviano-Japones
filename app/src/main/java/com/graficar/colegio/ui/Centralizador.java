package com.graficar.colegio.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
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
import java.util.List;
import java.util.Locale;

public class Centralizador extends AppCompatActivity {

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

    // ============================================================
    // CONFIGURACIÓN
    // ============================================================

    private static final int ANCHO_NUMERO = 35;
    private static final int ANCHO_NOMBRE = 200;
    private static final int ANCHO_NOTA = 80;

    private static final int ALTURA_FILA = 40;
    private static final int ALTURA_ENCABEZADO = 35;

    private final String gradoActual = "primeroC";
    private final String materiaActual = "matematica";

    // ============================================================
    // ESTRUCTURA DE COLUMNAS
    // ============================================================

    private static class ColumnaConfig {
        String titulo;
        String claveFirebase;
        String trimestre;

        ColumnaConfig(String titulo, String claveFirebase, String trimestre) {
            this.titulo = titulo;
            this.claveFirebase = claveFirebase;
            this.trimestre = trimestre;
        }
    }

    private final List<ColumnaConfig> columnas = new ArrayList<>();

    private void inicializarColumnas() {
        columnas.clear();

        // ============================================
        // TRIMESTRE 1
        // ============================================
        columnas.add(new ColumnaConfig("Nota Parcial 1T", "nota parcial", "trimestre1"));
        columnas.add(new ColumnaConfig("Ponderación 1T", "ponderacion", "trimestre1"));
        columnas.add(new ColumnaConfig("Nota Trimestral 1T", "nota trimestral", "trimestre1"));

        // ============================================
        // TRIMESTRE 2
        // ============================================
        columnas.add(new ColumnaConfig("Nota Parcial 2T", "nota parcial", "trimestre2"));
        columnas.add(new ColumnaConfig("Ponderación 2T", "ponderacion", "trimestre2"));
        columnas.add(new ColumnaConfig("Nota Trimestral 2T", "nota trimestral", "trimestre2"));

        // ============================================
        // TRIMESTRE 3
        // ============================================
        columnas.add(new ColumnaConfig("Nota Parcial 3T", "nota parcial", "trimestre3"));
        columnas.add(new ColumnaConfig("Ponderación 3T", "ponderacion", "trimestre3"));
        columnas.add(new ColumnaConfig("Nota Trimestral 3T", "nota trimestral", "trimestre3"));

        // ============================================
        // PROMEDIO ANUAL
        // ============================================
        columnas.add(new ColumnaConfig("PROMEDIO ANUAL", "promedioAnual", null));
    }

    // ============================================================
    // ESTUDIANTE
    // ============================================================

    private static class Estudiante {
        private final int numero;
        private final String nombre;
        private final String uid;
        private final java.util.Map<String, String> notas = new java.util.HashMap<>();

        Estudiante(int numero, String nombre, String uid) {
            this.numero = numero;
            this.nombre = nombre;
            this.uid = uid;
        }

        public void setNota(String clave, String valor) {
            if (valor == null || valor.trim().isEmpty()) {
                notas.put(clave, "--");
            } else {
                notas.put(clave, valor.trim());
            }
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
        setContentView(R.layout.activity_centralizador);

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
            btnGuardar.setOnClickListener(v -> {
                Toast.makeText(this, "Actualizando datos...", Toast.LENGTH_SHORT).show();
                cargarDatos();
            });
        }
    }

    // ============================================================
    // CARGAR DATOS
    // ============================================================

    private void cargarDatos() {
        dbRef = FirebaseDatabase.getInstance().getReference();
        cargarEstudiantes();
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
                    Toast.makeText(Centralizador.this, "No hay estudiantes", Toast.LENGTH_SHORT).show();
                    mostrarMensajeVacio();
                    return;
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    String nombre = child.child("nombre").getValue(String.class);
                    String grado = child.child("grado").getValue(String.class);

                    if (uid != null && nombre != null && !nombre.trim().isEmpty()
                            && grado != null && grado.equals(gradoActual)) {
                        Estudiante estudiante = new Estudiante(contador, nombre, uid);
                        listaEstudiantes.add(estudiante);
                        contador++;
                    }
                }

                if (listaEstudiantes.isEmpty()) {
                    Toast.makeText(Centralizador.this, "No hay estudiantes en " + gradoActual, Toast.LENGTH_SHORT).show();
                    mostrarMensajeVacio();
                    return;
                }

                cargarCalificaciones();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE", "Error estudiantes: " + error.getMessage());
                Toast.makeText(Centralizador.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ============================================================
    // CARGAR CALIFICACIONES
    // ============================================================

    private void cargarCalificaciones() {
        dbRef.child("calificaciones").child(gradoActual).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("FIREBASE", "Datos de calificaciones obtenidos: " + snapshot.toString());

                if (snapshot.exists()) {
                    for (DataSnapshot uidSnapshot : snapshot.getChildren()) {
                        String uid = uidSnapshot.getKey();
                        Estudiante estudiante = buscarEstudiantePorUid(uid);
                        if (estudiante == null) {
                            Log.d("FIREBASE", "Estudiante no encontrado para UID: " + uid);
                            continue;
                        }

                        Log.d("FIREBASE", "Cargando datos para: " + estudiante.getNombre() + " (UID: " + uid + ")");

                        // Cargar datos de cada trimestre
                        cargarNotasTrimestre(uidSnapshot, estudiante, "trimestre1");
                        cargarNotasTrimestre(uidSnapshot, estudiante, "trimestre2");
                        cargarNotasTrimestre(uidSnapshot, estudiante, "trimestre3");

                        // Cargar promedio anual
                        DataSnapshot promedioAnual = uidSnapshot.child("promedioAnual");
                        if (promedioAnual.exists()) {
                            Double promedio = promedioAnual.child(materiaActual).getValue(Double.class);
                            if (promedio != null) {
                                estudiante.setNota("promedioAnual", String.format(Locale.US, "%.1f", promedio));
                                Log.d("FIREBASE", "Promedio anual cargado: " + promedio + " para " + estudiante.getNombre());
                            }
                        }
                    }
                } else {
                    Log.d("FIREBASE", "No existen datos de calificaciones para " + gradoActual);
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
    // CARGAR NOTAS DE UN TRIMESTRE - CORREGIDO
    // ============================================================

    private void cargarNotasTrimestre(DataSnapshot uidSnapshot, Estudiante estudiante, String trimestre) {
        DataSnapshot trimestreSnapshot = uidSnapshot.child(trimestre);
        if (!trimestreSnapshot.exists()) {
            Log.d("FIREBASE", "No existe " + trimestre + " para " + estudiante.getNombre());
            return;
        }

        DataSnapshot materiaSnapshot = trimestreSnapshot.child(materiaActual);
        if (!materiaSnapshot.exists()) {
            Log.d("FIREBASE", "No existe " + materiaActual + " en " + trimestre + " para " + estudiante.getNombre());
            return;
        }

        // ============================================
        // NOTA PARCIAL - CORREGIDO
        // ============================================
        // La nota parcial está en la ruta: trimestre1/matematica/nota parcial
        Object notaParcialObj = materiaSnapshot.child("nota parcial").getValue();
        String claveNotaParcial = trimestre + ".nota parcial";
        if (notaParcialObj != null) {
            String valor = String.valueOf(notaParcialObj);
            estudiante.setNota(claveNotaParcial, valor);
            Log.d("FIREBASE", "Nota parcial " + trimestre + " para " + estudiante.getNombre() + ": " + valor);
        } else {
            Log.d("FIREBASE", "No hay nota parcial en " + trimestre + " para " + estudiante.getNombre());
            estudiante.setNota(claveNotaParcial, "--");
        }

        // ============================================
        // PONDERACIÓN - CORREGIDO
        // ============================================
        Object ponderacionObj = materiaSnapshot.child("ponderacion").getValue();
        String clavePonderacion = trimestre + ".ponderacion";
        if (ponderacionObj != null) {
            String valor = String.valueOf(ponderacionObj);
            estudiante.setNota(clavePonderacion, valor);
            Log.d("FIREBASE", "Ponderación " + trimestre + " para " + estudiante.getNombre() + ": " + valor);
        } else {
            Log.d("FIREBASE", "No hay ponderación en " + trimestre + " para " + estudiante.getNombre());
            estudiante.setNota(clavePonderacion, "--");
        }

        // ============================================
        // NOTA TRIMESTRAL - CORREGIDO
        // ============================================
        Object notaTrimestralObj = materiaSnapshot.child("nota trimestral").getValue();
        String claveNotaTrimestral = trimestre + ".nota trimestral";
        if (notaTrimestralObj != null) {
            String valor = String.valueOf(notaTrimestralObj);
            estudiante.setNota(claveNotaTrimestral, valor);
            Log.d("FIREBASE", "Nota trimestral " + trimestre + " para " + estudiante.getNombre() + ": " + valor);
        } else {
            Log.d("FIREBASE", "No hay nota trimestral en " + trimestre + " para " + estudiante.getNombre());
            estudiante.setNota(claveNotaTrimestral, "--");
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

        if (listaEstudiantes.isEmpty()) {
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

        tablaNotas.addView(filaEncabezadoNotas);
    }

    // ============================================================
    // CREAR TEXTVIEW ENCABEZADO
    // ============================================================

    private TextView crearTextViewEncabezado(String texto, int ancho) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(9);
        tv.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new TableRow.LayoutParams(dpToPx(ancho), dpToPx(ALTURA_ENCABEZADO)));
        return tv;
    }

    // ============================================================
    // FILA ESTUDIANTE - CORREGIDO
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
        // FILA NOTAS - CORREGIDO
        // ========================================================
        TableRow filaNotas = new TableRow(this);
        filaNotas.setBackgroundColor(colorFondo);

        for (ColumnaConfig columna : columnas) {
            String valor;

            // ============================================
            // OBTENER EL VALOR CORRECTO
            // ============================================
            if (columna.claveFirebase.equals("promedioAnual")) {
                // Para el promedio anual
                valor = estudiante.getNota("promedioAnual");
            } else {
                // Para las notas de trimestre: "trimestre1.nota parcial"
                String clave = columna.trimestre + "." + columna.claveFirebase;
                valor = estudiante.getNota(clave);
            }

            // Si el valor es null o vacío, mostrar "--"
            if (valor == null || valor.isEmpty()) {
                valor = "--";
            }

            TextView tv = new TextView(this);
            tv.setText(valor);
            tv.setTextSize(11);
            tv.setGravity(Gravity.CENTER);

            // Resaltar el promedio anual
            if (columna.claveFirebase.equals("promedioAnual") && !valor.equals("--")) {
                tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            }

            tv.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));
            filaNotas.addView(tv);
        }

        tablaNotas.addView(filaNotas);
    }

    // ============================================================
    // FILA TOTALES - CORREGIDO
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
        // PARTE NOTAS - CORREGIDO
        // ========================================================
        TableRow filaTotalesNotas = new TableRow(this);
        filaTotalesNotas.setBackgroundColor(colorFondo);

        for (ColumnaConfig columna : columnas) {
            double suma = 0;
            int count = 0;

            String claveBuscar;
            if (columna.claveFirebase.equals("promedioAnual")) {
                claveBuscar = "promedioAnual";
            } else {
                claveBuscar = columna.trimestre + "." + columna.claveFirebase;
            }

            for (Estudiante estudiante : listaEstudiantes) {
                double valor = estudiante.getNotaValor(claveBuscar);
                if (valor >= 0) {
                    suma += valor;
                    count++;
                }
            }

            double promedio = count > 0 ? suma / count : 0;
            TextView tvProm = new TextView(this);
            tvProm.setText(count > 0 ? String.format(Locale.US, "%.1f", promedio) : "--");
            tvProm.setTextSize(11);
            tvProm.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvProm.setTypeface(null, android.graphics.Typeface.BOLD);
            tvProm.setGravity(Gravity.CENTER);
            tvProm.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));
            filaTotalesNotas.addView(tvProm);
        }

        tablaNotas.addView(filaTotalesNotas);
    }

    // ============================================================
    // TÍTULO
    // ============================================================

    private void actualizarTituloInicial() {
        if (tvTituloCurso == null) return;

        int conNotas = 0;
        for (Estudiante estudiante : listaEstudiantes) {
            boolean tieneNotas = !estudiante.getNota("trimestre1.nota trimestral").equals("--")
                    || !estudiante.getNota("trimestre2.nota trimestral").equals("--")
                    || !estudiante.getNota("trimestre3.nota trimestral").equals("--");
            if (tieneNotas) conNotas++;
        }

        tvTituloCurso.setText("PRIMERO C - MATEMÁTICA (CENTRALIZADOR) - "
                + listaEstudiantes.size() + " alumnos (" + conNotas + " con notas)");
    }

    // ============================================================
    // DP A PX
    // ============================================================

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}