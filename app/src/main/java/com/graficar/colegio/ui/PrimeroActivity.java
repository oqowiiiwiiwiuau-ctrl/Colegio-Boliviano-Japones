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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PrimeroActivity extends AppCompatActivity {

    // ============================================================
    // UI COMPONENTS
    // ============================================================
    private TableLayout tablaNotas;
    private TableLayout tablaFija;
    private ScrollView scrollVertical;
    private HorizontalScrollView scrollHorizontal;
    private ScrollView scrollFijaVertical;
    private TextView tvTituloCurso;
    private Button btnGuardar;

    // ============================================================
    // DATA
    // ============================================================
    private List<Estudiante> listaEstudiantes;
    private DatabaseReference dbRef;
    private Map<String, Map<String, Object>> cambiosPendientes = new HashMap<>();
    private Map<String, String> mapaObservaciones = new HashMap<>();
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

    private String gradoActual = "primeroC";
    private String trimestreActual = "trimestre1";  // ← Solo trabajamos con Trimestre 1
    private String materiaActual = "matematica";

    // ============================================================
    // COLUMNAS
    // ============================================================
    private static class ColumnaConfig {
        String titulo;
        String claveFirebase;
        boolean editable;

        public ColumnaConfig(String titulo, String claveFirebase, boolean editable) {
            this.titulo = titulo;
            this.claveFirebase = claveFirebase;
            this.editable = editable;
        }
    }

    private List<ColumnaConfig> columnas = new ArrayList<>();

    private void inicializarColumnas() {
        columnas.add(new ColumnaConfig("SER", "ser", true));
        columnas.add(new ColumnaConfig("SABER", "saber", true));
        columnas.add(new ColumnaConfig("HACER", "hacer", true));
        columnas.add(new ColumnaConfig("PROMEDIO", "promedio", false));
        columnas.add(new ColumnaConfig("TOTAL", "total", false));
        columnas.add(new ColumnaConfig("AUTOEV.", "autoevaluacion", false));
        columnas.add(new ColumnaConfig("NOTA PARCIAL", "nota parcial", false));
        columnas.add(new ColumnaConfig("PONDERACIÓN", "ponderacion", false));
        columnas.add(new ColumnaConfig("NOTA TRIMESTRAL", "nota trimestral", false));
    }

    // ============================================================
    // ESTUDIANTE CLASS
    // ============================================================
    private static class Estudiante {
        private int numero;
        private String nombre;
        private String uid;
        private Map<String, String> notas;

        public Estudiante(int numero, String nombre, String uid) {
            this.numero = numero;
            this.nombre = nombre;
            this.uid = uid;
            this.notas = new HashMap<>();
            for (String key : new String[]{"ser", "saber", "hacer", "promedio", "total",
                    "autoevaluacion", "nota parcial", "ponderacion", "nota trimestral"}) {
                notas.put(key, "--");
            }
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
        public String getUid() { return uid; }
    }

    // ============================================================
    // LIFECYCLE
    // ============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primero);

        inicializarColumnas();
        inicializarViews();
        configurarListeners();
        cargarDatos();
    }

    private void inicializarViews() {
        tablaNotas = findViewById(R.id.tablaNotas);
        tablaFija = findViewById(R.id.tablaFija);
        scrollVertical = findViewById(R.id.scrollVertical);
        scrollHorizontal = findViewById(R.id.scrollHorizontal);
        scrollFijaVertical = findViewById(R.id.scrollFijaVertical);
        tvTituloCurso = findViewById(R.id.tvTituloCurso);
        btnGuardar = findViewById(R.id.btnGuardar);
    }

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
    // FIREBASE - CARGA DE DATOS
    // ============================================================
    private void cargarDatos() {
        dbRef = FirebaseDatabase.getInstance().getReference();
        cargarObservaciones();
    }

    // ============================================================
    // CARGAR OBSERVACIONES DEL TRIMESTRE 1
    // ============================================================
    private void cargarObservaciones() {
        dbRef.child("observaciones").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mapaObservaciones.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot uidSnapshot : snapshot.getChildren()) {
                        String uid = uidSnapshot.getKey();

                        // ✅ Buscar solo en trimestre1
                        DataSnapshot trimestreSnapshot = uidSnapshot.child(trimestreActual);
                        if (trimestreSnapshot.exists()) {
                            String texto = trimestreSnapshot.child("texto").getValue(String.class);
                            if (texto != null && !texto.isEmpty()) {
                                mapaObservaciones.put(uid, texto);
                                Log.d("FIREBASE", "📝 Observación T1 para " + uid + ": " + texto);
                            }
                        }
                    }
                }

                cargarEstudiantes();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                cargarEstudiantes();
            }
        });
    }

    private void cargarEstudiantes() {
        dbRef.child("estudiantes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaEstudiantes = new ArrayList<>();
                int contador = 1;

                if (!snapshot.exists()) {
                    Toast.makeText(PrimeroActivity.this, "No hay estudiantes", Toast.LENGTH_SHORT).show();
                    mostrarMensajeVacio();
                    return;
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    String nombre = child.child("nombre").getValue(String.class);

                    if (nombre != null && !nombre.isEmpty()) {
                        Estudiante estudiante = new Estudiante(contador, nombre, uid);
                        listaEstudiantes.add(estudiante);
                        contador++;
                    }
                }

                cargarCalificaciones();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE", "Error: " + error.getMessage());
                Toast.makeText(PrimeroActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

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
                            for (ColumnaConfig columna : columnas) {
                                DataSnapshot notaSnapshot = materiaSnapshot.child(columna.claveFirebase);
                                if (notaSnapshot.exists()) {
                                    Object valor = notaSnapshot.getValue();
                                    estudiante.setNota(columna.claveFirebase,
                                            valor != null ? valor.toString() : "--");
                                }
                            }
                        }
                    }
                }

                runOnUiThread(() -> {
                    int conNotas = 0;
                    for (Estudiante e : listaEstudiantes) {
                        if (!e.getNota("ser").equals("--")) conNotas++;
                    }

                    if (tvTituloCurso != null) {
                        tvTituloCurso.setText("PRIMERO C - MATEMÁTICA (TRIMESTRE 1) - " +
                                listaEstudiantes.size() + " alumnos (" + conNotas + " con notas)");
                    }

                    crearTablas();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> crearTablas());
            }
        });
    }

    private Estudiante buscarEstudiantePorUid(String uid) {
        for (Estudiante estudiante : listaEstudiantes) {
            if (estudiante.getUid().equals(uid)) {
                return estudiante;
            }
        }
        return null;
    }

    // ============================================================
    // CREACIÓN DE TABLAS
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

        TableRow filaEncabezadoFija = new TableRow(this);
        filaEncabezadoFija.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));

        TextView tvNumero = new TextView(this);
        tvNumero.setText("N°");
        tvNumero.setTextSize(12);
        tvNumero.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvNumero.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNumero.setGravity(Gravity.CENTER);
        tvNumero.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NUMERO), dpToPx(ALTURA_ENCABEZADO)));
        filaEncabezadoFija.addView(tvNumero);

        TextView tvApellidos = new TextView(this);
        tvApellidos.setText("APELLIDOS Y NOMBRES");
        tvApellidos.setTextSize(12);
        tvApellidos.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvApellidos.setTypeface(null, android.graphics.Typeface.BOLD);
        tvApellidos.setGravity(Gravity.CENTER);
        tvApellidos.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOMBRE), dpToPx(ALTURA_ENCABEZADO)));
        filaEncabezadoFija.addView(tvApellidos);

        tablaFija.addView(filaEncabezadoFija);

        TableRow filaEncabezadoNotas = new TableRow(this);
        filaEncabezadoNotas.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));

        for (ColumnaConfig columna : columnas) {
            TextView tvCol = new TextView(this);
            tvCol.setText(columna.titulo);
            tvCol.setTextSize(10);
            tvCol.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvCol.setTypeface(null, android.graphics.Typeface.BOLD);
            tvCol.setGravity(Gravity.CENTER);
            tvCol.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOTA), dpToPx(ALTURA_ENCABEZADO)));
            filaEncabezadoNotas.addView(tvCol);
        }

        TextView tvObs = new TextView(this);
        tvObs.setText("OBSERVACIONES (T1)");
        tvObs.setTextSize(10);
        tvObs.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        tvObs.setTypeface(null, android.graphics.Typeface.BOLD);
        tvObs.setGravity(Gravity.CENTER);
        tvObs.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_OBSERVACIONES), dpToPx(ALTURA_ENCABEZADO)));
        filaEncabezadoNotas.addView(tvObs);

        tablaNotas.addView(filaEncabezadoNotas);
    }

    // ============================================================
    // FILAS DE ESTUDIANTES
    // ============================================================
    private void crearFilaEstudiante(Estudiante estudiante) {
        if (tablaFija == null || tablaNotas == null) return;

        int colorFondo = estudiante.getNumero() % 2 == 0
                ? ContextCompat.getColor(this, R.color.gris_claro)
                : ContextCompat.getColor(this, android.R.color.white);

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

        TableRow filaNotas = new TableRow(this);
        filaNotas.setBackgroundColor(colorFondo);

        for (ColumnaConfig columna : columnas) {
            String valor = estudiante.getNota(columna.claveFirebase);

            if (columna.editable) {
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

                        String key = estudiante.getUid() + "_" + columna.claveFirebase;
                        Map<String, Object> cambio = new HashMap<>();
                        cambio.put("valor", nuevoValor);
                        cambio.put("uid", estudiante.getUid());
                        cambio.put("claveFirebase", columna.claveFirebase);
                        cambiosPendientes.put(key, cambio);
                        hayCambios = true;

                        estudiante.setNota(columna.claveFirebase, nuevoValor);

                        if (tvTituloCurso != null) {
                            tvTituloCurso.setText("PRIMERO C - MATEMÁTICA * (" + cambiosPendientes.size() + " cambios)");
                        }
                    }
                });

                editText.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));
                filaNotas.addView(editText);
            } else {
                TextView tv = new TextView(this);
                tv.setText(valor);
                tv.setTextSize(11);
                tv.setGravity(Gravity.CENTER);
                if (columna.claveFirebase.equals("promedio")) {
                    tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                tv.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));
                filaNotas.addView(tv);
            }
        }

        // ============================================================
        // COLUMNA OBSERVACIONES - TRIMESTRE 1
        // ============================================================
        String observacion = mapaObservaciones.getOrDefault(estudiante.getUid(), "");
        EditText editTextObs = new EditText(this);
        editTextObs.setText(observacion);
        editTextObs.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
        editTextObs.setTextSize(11);
        editTextObs.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        editTextObs.setBackgroundResource(android.R.drawable.edit_text);
        editTextObs.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editTextObs.setSingleLine(false);
        editTextObs.setMaxLines(3);
        editTextObs.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_OBSERVACIONES), dpToPx(ALTURA_FILA)));

        editTextObs.addTextChangedListener(new TextWatcher() {
            private String textoAnterior = observacion;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String nuevoTexto = s.toString().trim();

                if (!nuevoTexto.equals(textoAnterior)) {
                    textoAnterior = nuevoTexto;

                    if (nuevoTexto.isEmpty()) {
                        mapaObservaciones.remove(estudiante.getUid());
                    } else {
                        mapaObservaciones.put(estudiante.getUid(), nuevoTexto);
                    }

                    String key = estudiante.getUid() + "_observacion";
                    Map<String, Object> cambio = new HashMap<>();
                    cambio.put("valor", nuevoTexto);
                    cambio.put("uid", estudiante.getUid());
                    cambio.put("claveFirebase", "observacion");
                    cambiosPendientes.put(key, cambio);
                    hayCambios = true;

                    if (tvTituloCurso != null) {
                        tvTituloCurso.setText("PRIMERO C - MATEMÁTICA * (" + cambiosPendientes.size() + " cambios)");
                    }
                }
            }
        });

        filaNotas.addView(editTextObs);

        tablaNotas.addView(filaNotas);
    }

    // ============================================================
    // FILA DE TOTALES
    // ============================================================
    private void crearFilaTotales() {
        if (tablaFija == null || tablaNotas == null) return;

        int colorFondo = ContextCompat.getColor(this, R.color.color_secundario);

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
            tvProm.setText(String.format("%.1f", promedio));
            tvProm.setTextSize(11);
            tvProm.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvProm.setTypeface(null, android.graphics.Typeface.BOLD);
            tvProm.setGravity(Gravity.CENTER);
            tvProm.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_NOTA), dpToPx(ALTURA_FILA)));
            filaTotalesNotas.addView(tvProm);
        }

        TextView tvObsVacia = new TextView(this);
        tvObsVacia.setText("");
        tvObsVacia.setLayoutParams(new TableRow.LayoutParams(dpToPx(ANCHO_OBSERVACIONES), dpToPx(ALTURA_FILA)));
        filaTotalesNotas.addView(tvObsVacia);

        tablaNotas.addView(filaTotalesNotas);
    }

    // ============================================================
    // GUARDAR OBSERVACIÓN - TRIMESTRE 1
    // ============================================================
    private void guardarObservacion(String uid, String texto) {
        if (texto == null || texto.isEmpty()) {
            // Si el texto está vacío, eliminar la observación
            dbRef.child("observaciones").child(uid).child(trimestreActual).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("FIREBASE", "🗑️ Observación eliminada para: " + uid + " - " + trimestreActual);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FIREBASE", "❌ Error al eliminar: " + e.getMessage());
                    });
            return;
        }

        // ✅ Guardar en observaciones/[uid]/trimestre1/texto
        DatabaseReference ref = dbRef
                .child("observaciones")
                .child(uid)
                .child(trimestreActual);

        Map<String, Object> obsData = new HashMap<>();
        obsData.put("texto", texto);

        ref.setValue(obsData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE", "✅ Observación T1 guardada: " + uid + " - " + texto);
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "❌ Error: " + e.getMessage());
                });
    }

    // ============================================================
    // GUARDAR TODOS LOS CAMBIOS
    // ============================================================
    private void guardarTodosLosCambios() {
        if (cambiosPendientes.isEmpty()) {
            Toast.makeText(this, "✅ No hay cambios para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "💾 Guardando " + cambiosPendientes.size() + " cambios...", Toast.LENGTH_SHORT).show();

        for (Map.Entry<String, Map<String, Object>> entry : cambiosPendientes.entrySet()) {
            Map<String, Object> cambio = entry.getValue();
            String uid = (String) cambio.get("uid");
            String claveFirebase = (String) cambio.get("claveFirebase");
            String valor = (String) cambio.get("valor");

            if (claveFirebase.equals("observacion")) {
                guardarObservacion(uid, valor);
            } else {
                DatabaseReference ref = dbRef
                        .child("calificaciones")
                        .child(gradoActual)
                        .child(uid)
                        .child(trimestreActual)
                        .child(materiaActual)
                        .child(claveFirebase);

                Object valorGuardar = valor.equals("--") ? null : valor;

                ref.setValue(valorGuardar)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("FIREBASE", "✅ Guardado: " + uid + " - " + claveFirebase);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FIREBASE", "❌ Error: " + e.getMessage());
                        });
            }
        }

        cambiosPendientes.clear();
        hayCambios = false;

        if (tvTituloCurso != null) {
            tvTituloCurso.setText("PRIMERO C - MATEMÁTICA (TRIMESTRE 1) - " +
                    listaEstudiantes.size() + " alumnos");
        }

        Toast.makeText(this, "✅ Todos los cambios guardados", Toast.LENGTH_SHORT).show();
        cargarObservaciones();
    }

    // ============================================================
    // UTILIDADES
    // ============================================================
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}