package com.graficar.colegio.ui;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.graficar.colegio.R;
import java.util.ArrayList;
import java.util.List;
public class PrimeroActivity extends AppCompatActivity {
    private TableLayout tablaNotas;
    private List<Estudiante> listaEstudiantes;
    private String[] nombresEstudiantes = {
            "Juan Pérez López",
            "María González Ríos",
            "Carlos Fernández Méndez",
            "Ana Rodríguez Vargas",
            "Luis Torres Castillo",
            "Sofía Mendoza Paredes",
            "Diego Castro Salinas",
            "Valeria Herrera Flores",
            "Andrés Rojas Quispe",
            "Camila Silva Ortiz"
    };
    private String[] encabezados = {
            "N°",
            "ESTUDIANTE",
            "PRÁCTICA 1",
            "PRÁCTICA 2",
            "PRÁCTICA 3",
            "EXAMEN\nPARCIAL",  // Con salto de línea
            "EXAMEN\nFINAL",
            "TRABAJO\nFINAL",
            "PARTICIPACIÓN",
            "PROMEDIO"
    };
    private int[] anchosColumnas = {
            30,     // N°
            110,    // ESTUDIANTE
            100,    // PRÁCTICA 1
            100,    // PRÁCTICA 2
            100,    // PRÁCTICA 3
            120,    // EXAMEN PARCIAL
            120,    // EXAMEN FINAL
            120,    // TRABAJO FINAL
            120,    // PARTICIPACIÓN (más ancho por palabra larga)
            110     // PROMEDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primero);

        tablaNotas = findViewById(R.id.tablaNotas);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        inicializarEstudiantes();
        crearTablaNotas();
    }
    private void inicializarEstudiantes() {
        listaEstudiantes = new ArrayList<>();

        for (int i = 0; i < nombresEstudiantes.length; i++) {
            Estudiante estudiante = new Estudiante(
                    (i + 1),
                    nombresEstudiantes[i],
                    generarNotaAleatoria(),
                    generarNotaAleatoria(),
                    generarNotaAleatoria(),
                    generarNotaAleatoria(),
                    generarNotaAleatoria(),
                    generarNotaAleatoria(),
                    generarNotaAleatoria()
            );
            listaEstudiantes.add(estudiante);
        }
    }
    private double generarNotaAleatoria() {
        return Math.round((Math.random() * 20) * 10.0) / 10.0;
    }
    private void crearTablaNotas() {
        tablaNotas.removeAllViews();
        crearFilaEncabezados();

        for (Estudiante estudiante : listaEstudiantes) {
            crearFilaEstudiante(estudiante);
        }

        crearFilaTotales();
    }
    private void crearFilaEncabezados() {
        TableRow filaEncabezado = new TableRow(this);
        filaEncabezado.setBackgroundColor(ContextCompat.getColor(this, R.color.curso_primero));

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                dpToPx(70)
        );
        filaEncabezado.setLayoutParams(params);

        for (int i = 0; i < encabezados.length; i++) {
            TextView textView = new TextView(this);
            textView.setText(encabezados[i]);
            textView.setPadding(dpToPx(10), dpToPx(12), dpToPx(10), dpToPx(12));
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            textView.setTextSize(14);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
            textView.setGravity(Gravity.CENTER);

            // Permitir múltiples líneas donde hay \n
            if (encabezados[i].contains("\n")) {
                textView.setSingleLine(false);
                textView.setMaxLines(2);
            } else {
                textView.setSingleLine(true);
            }

            // Configurar ancho
            if (i < anchosColumnas.length) {
                int ancho = dpToPx(anchosColumnas[i]);


                TableRow.LayoutParams tvParams = crearParamsColumna(anchosColumnas[i]);
                    tvParams.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
                    textView.setLayoutParams(tvParams
                    );

                tvParams.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
                textView.setLayoutParams(tvParams);
            }

            filaEncabezado.addView(textView);
        }

        tablaNotas.addView(filaEncabezado);
    }
    private void crearFilaEstudiante(Estudiante estudiante) {
        TableRow fila = new TableRow(this);

        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                dpToPx(60)
        );
        fila.setLayoutParams(rowParams);

        int colorFondo = estudiante.getNumero() % 2 == 0
                ? ContextCompat.getColor(this, R.color.gris_claro)
                : ContextCompat.getColor(this, android.R.color.white);
        fila.setBackgroundColor(colorFondo);

        // Columna 1: Número
        TextView tvNumero = crearTextView(String.valueOf(estudiante.getNumero()));
        tvNumero.setLayoutParams(crearParamsColumna(anchosColumnas[0]));
        fila.addView(tvNumero);

        TextView tvNombre = crearTextView(estudiante.getNombre());
        tvNombre.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tvNombre.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        tvNombre.setLayoutParams(crearParamsColumna(anchosColumnas[1]));
        fila.addView(tvNombre);


        // Columnas 3-9: Notas editables
        for (int i = 0; i < 7; i++) {
            EditText editText = crearEditText(String.valueOf(estudiante.getNotasArray()[i]));
            editText.setMinWidth(dpToPx(anchosColumnas[i + 2]));
            fila.addView(editText);
        }
        // Columna 10: Promedio
        TextView tvPromedio = crearTextView(String.format("%.1f", estudiante.calcularPromedio()));
        tvPromedio.setTextColor(ContextCompat.getColor(this, R.color.green));
        tvPromedio.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPromedio.setMinWidth(dpToPx(anchosColumnas[9]));
        fila.addView(tvPromedio);

        tablaNotas.addView(fila);
    }
    private void crearFilaTotales() {
        TableRow filaTotales = new TableRow(this);
        filaTotales.setBackgroundColor(ContextCompat.getColor(this, R.color.color_secundario));

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                dpToPx(60)
        );
        filaTotales.setLayoutParams(params);

        // Celda 1: "TOTALES"
        TextView tvTotales = crearTextView("TOTALES");
        tvTotales.setLayoutParams(crearParamsColumna(anchosColumnas[0]));
        filaTotales.addView(tvTotales);

        // Celda 2: Vacía
        TextView tvVacia = crearTextView("");
        tvVacia.setMinWidth(dpToPx(anchosColumnas[1]));
        filaTotales.addView(tvVacia);

        // Celdas 3-9: Promedios por columna
        for (int i = 0; i < 7; i++) {
            TextView tvPromColumna = crearTextView("0.0");
            tvPromColumna.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvPromColumna.setTypeface(null, android.graphics.Typeface.BOLD);
            tvPromColumna.setMinWidth(dpToPx(anchosColumnas[i + 2]));
            filaTotales.addView(tvPromColumna);
        }

        // Celda 10: Promedio general
        TextView tvPromGeneral = crearTextView("0.0");
        tvPromGeneral.setTextColor(ContextCompat.getColor(this, R.color.color_accent));
        tvPromGeneral.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPromGeneral.setMinWidth(dpToPx(anchosColumnas[9]));
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
        textView.setMinHeight(dpToPx(50));

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
        editText.setMinHeight(dpToPx(50));

        return editText;
    }
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    private static class Estudiante {
        private int numero;
        private String nombre;
        private double practica1, practica2, practica3;
        private double examenParcial, examenFinal;
        private double trabajoFinal, participacion;
        public Estudiante(int numero, String nombre,
                          double practica1, double practica2, double practica3,
                          double examenParcial, double examenFinal,
                          double trabajoFinal, double participacion) {
            this.numero = numero;
            this.nombre = nombre;
            this.practica1 = practica1;
            this.practica2 = practica2;
            this.practica3 = practica3;
            this.examenParcial = examenParcial;
            this.examenFinal = examenFinal;
            this.trabajoFinal = trabajoFinal;
            this.participacion = participacion;
        }
        public int getNumero() { return numero; }
        public String getNombre() { return nombre; }
        public double[] getNotasArray() {
            return new double[]{
                    practica1, practica2, practica3,
                    examenParcial, examenFinal,
                    trabajoFinal, participacion
            };
        }
        public double calcularPromedio() {
            double[] notas = getNotasArray();
            double suma = 0;
            int conteo = 0;

            for (double nota : notas) {
                if (nota > 0) {
                    suma += nota;
                    conteo++;
                }
            }
            return conteo > 0 ? suma / conteo : 0.0;
        }
    }
    private TableRow.LayoutParams crearParamsColumna(int anchoDp) {
        return new TableRow.LayoutParams(
                dpToPx(anchoDp),
                TableRow.LayoutParams.WRAP_CONTENT
        );
    }
}