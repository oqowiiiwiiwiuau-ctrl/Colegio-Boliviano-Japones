package com.graficar.colegio.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.graficar.colegio.databinding.FragmentDashboardBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private SharedPreferences prefs;

    // ============================================================
    // CONFIGURACIÓN
    // ============================================================
    private String gradoActual = "primeroC";
    private String trimestreActual = "trimestre1";
    private String materiaActual = "matematica";
    private String currentDate = "2026-08-20";

    // Mapa para guardar UID -> Nombre
    private Map<String, String> mapaUidANombre = new HashMap<>();
    private Map<String, String> mapaNombreAUid = new HashMap<>();
    private boolean nombresCargados = false;

    // ============================================================
    // LISTA DE TRIMESTRES Y MATERIAS
    // ============================================================
    private final String[] trimestres = {
            "trimestre1", "trimestre2", "trimestre3"
    };

    private final String[] trimestresNombres = {
            "Trimestre 1", "Trimestre 2", "Trimestre 3"
    };

    private final String[] materias = {
            "Matemática"
    };

    // Variable para almacenar el UID del estudiante actual
    private String currentStudentUid = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Inicializar SharedPreferences
        prefs = requireContext().getSharedPreferences("DASHBOARD_DATA", Context.MODE_PRIVATE);

        // Configurar visibilidad inicial
        setupInitialVisibility();

        // Configurar spinner de materias
        setupSpinner();

        // Configurar botón de búsqueda
        setupSearchButton();

        // Configurar botón de exportar PDF
        setupPdfButton();

        // Cargar nombres de estudiantes
        cargarNombresEstudiantes();

        return root;
    }

    // ============================================================
    // CONFIGURACIÓN INICIAL
    // ============================================================
    private void setupInitialVisibility() {
        binding.textStudentName.setVisibility(View.GONE);
        binding.spinnerMaterias.setVisibility(View.GONE);
        binding.cardNotas.setVisibility(View.GONE);
        binding.btnExportarPDF.setVisibility(View.GONE);
    }

    // ============================================================
    // CARGAR NOMBRES DE ESTUDIANTES
    // ============================================================
    private void cargarNombresEstudiantes() {
        FirebaseDatabase.getInstance().getReference("estudiantes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        mapaUidANombre.clear();
                        mapaNombreAUid.clear();
                        nombresCargados = true;

                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String uid = child.getKey();
                                String nombre = child.child("nombre").getValue(String.class);
                                String grado = child.child("grado").getValue(String.class);

                                if (nombre != null && !nombre.isEmpty() && grado != null && grado.equals(gradoActual)) {
                                    String nombreMayusculas = nombre.toUpperCase();
                                    mapaUidANombre.put(uid, nombreMayusculas);
                                    mapaNombreAUid.put(nombreMayusculas, uid);
                                    Log.d("DASHBOARD", "📌 Cargado: " + uid + " -> " + nombreMayusculas);
                                }
                            }
                        }

                        Log.d("DASHBOARD", "📊 Total estudiantes cargados: " + mapaUidANombre.size());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("DASHBOARD", "❌ Error al cargar nombres: " + error.getMessage());
                        nombresCargados = false;
                    }
                });
    }

    // ============================================================
    // CONVERTIR NOMBRE A UID
    // ============================================================
    private String convertirNombreAUid(String nombre) {
        if (!nombresCargados || mapaNombreAUid.isEmpty()) {
            cargarNombresEstudiantes();
            return null;
        }

        String nombreUpper = nombre.trim().toUpperCase();

        if (mapaNombreAUid.containsKey(nombreUpper)) {
            return mapaNombreAUid.get(nombreUpper);
        }

        // Buscar coincidencia parcial
        String mejorCoincidencia = null;
        int minDiferencia = Integer.MAX_VALUE;

        for (String key : mapaNombreAUid.keySet()) {
            int distancia = calcularDistancia(key, nombreUpper);
            if (distancia < minDiferencia && distancia < 5) {
                minDiferencia = distancia;
                mejorCoincidencia = key;
            }
        }

        if (mejorCoincidencia != null) {
            return mapaNombreAUid.get(mejorCoincidencia);
        }

        return null;
    }

    // ============================================================
    // CALCULAR DISTANCIA DE LEVENSHTEIN
    // ============================================================
    private int calcularDistancia(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }

    // ============================================================
    // CONFIGURAR SPINNER DE MATERIAS
    // ============================================================
    private void setupSpinner() {
        Spinner spinner = binding.spinnerMaterias;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                materias
        );
        spinner.setAdapter(adapter);

        spinner.setSelection(prefs.getInt("LAST_MATERIA_INDEX", 0));

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (!TextUtils.isEmpty(currentStudentUid)) {
                    String materiaSeleccionada = materias[position];
                    binding.textMateriaTitulo.setText(materiaSeleccionada);
                    cargarDatosEstudiante(currentStudentUid, materiaSeleccionada);
                    prefs.edit().putInt("LAST_MATERIA_INDEX", position).apply();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    // ============================================================
    // CONFIGURAR BOTÓN DE BÚSQUEDA
    // ============================================================
    private void setupSearchButton() {
        binding.btnBuscarDatos.setOnClickListener(v -> buscarDatosEstudiante());
    }

    // ============================================================
    // CONFIGURAR BOTÓN DE EXPORTAR PDF
    // ============================================================
    private void setupPdfButton() {
        binding.btnExportarPDF.setOnClickListener(v -> exportarPDF());
    }

    // ============================================================
    // BUSCAR DATOS DEL ESTUDIANTE
    // ============================================================
    private void buscarDatosEstudiante() {
        String nombreEstudiante = binding.editTextStudentName.getText().toString().trim();

        if (TextUtils.isEmpty(nombreEstudiante)) {
            Toast.makeText(requireContext(), "Por favor ingresa un nombre de estudiante", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar mensaje de carga
        binding.textNotas.setText("🔍 Buscando estudiante...");

        // Verificar si los nombres están cargados
        if (!nombresCargados || mapaNombreAUid.isEmpty()) {
            Toast.makeText(requireContext(), "Cargando lista de estudiantes...", Toast.LENGTH_SHORT).show();
            cargarNombresEstudiantes();
            // Reintentar después de 1 segundo
            new android.os.Handler().postDelayed(() -> buscarDatosEstudiante(), 1000);
            return;
        }

        // Convertir nombre a UID
        String uid = convertirNombreAUid(nombreEstudiante);

        if (uid == null) {
            Toast.makeText(requireContext(), "❌ Estudiante no encontrado", Toast.LENGTH_SHORT).show();
            binding.textNotas.setText("❌ Estudiante no encontrado en el sistema");
            return;
        }

        currentStudentUid = uid;
        String nombreMostrar = mapaUidANombre.getOrDefault(uid, nombreEstudiante);
        binding.textStudentName.setText("👤 " + nombreMostrar);

        // Mostrar elementos de la UI
        mostrarElementosUI();

        // Cargar datos de la primera materia
        String primeraMateria = materias[0];
        binding.textMateriaTitulo.setText(primeraMateria);
        cargarDatosEstudiante(currentStudentUid, primeraMateria);
    }

    // ============================================================
    // MOSTRAR ELEMENTOS UI
    // ============================================================
    private void mostrarElementosUI() {
        binding.textStudentName.setVisibility(View.VISIBLE);
        binding.spinnerMaterias.setVisibility(View.VISIBLE);
        binding.cardNotas.setVisibility(View.VISIBLE);
        binding.btnExportarPDF.setVisibility(View.VISIBLE);
    }

    // ============================================================
    // CARGAR DATOS DEL ESTUDIANTE (TODOS LOS TRIMESTRES)
    // ============================================================
    private void cargarDatosEstudiante(String uid, String materia) {
        // Mostrar estado de carga
        binding.textNotas.setText("📥 Cargando datos de " + materia + "...");

        // Buscar en calificaciones/[grado]/[uid]/trimestre1/matematica
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("calificaciones")
                .child(gradoActual)
                .child(uid);

        // Obtener el nombre del estudiante para mostrar
        String nombreEstudiante = mapaUidANombre.getOrDefault(uid, uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    binding.textNotas.setText("❌ No se encontraron datos para " + nombreEstudiante);
                    return;
                }

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("📊 NOTAS DE ").append(nombreEstudiante.toUpperCase()).append("\n");
                stringBuilder.append("📚 Materia: ").append(materia).append("\n");
                stringBuilder.append("📅 ").append(currentDate).append("\n");
                stringBuilder.append("─────────────────────\n\n");

                // Recorrer los 3 trimestres
                for (int i = 0; i < trimestres.length; i++) {
                    String trimestreKey = trimestres[i];
                    String trimestreNombre = trimestresNombres[i];

                    DataSnapshot trimestreSnapshot = snapshot.child(trimestreKey).child("matematica");

                    if (trimestreSnapshot.exists()) {
                        String ser = obtenerValorSeguro(trimestreSnapshot.child("ser"));
                        String saber = obtenerValorSeguro(trimestreSnapshot.child("saber"));
                        String hacer = obtenerValorSeguro(trimestreSnapshot.child("hacer"));
                        String promedio = obtenerValorSeguro(trimestreSnapshot.child("promedio"));
                        String total = obtenerValorSeguro(trimestreSnapshot.child("total"));
                        String autoevaluacion = obtenerValorSeguro(trimestreSnapshot.child("autoevaluacion"));
                        String notaParcial = obtenerValorSeguro(trimestreSnapshot.child("nota parcial"));
                        String ponderacion = obtenerValorSeguro(trimestreSnapshot.child("ponderacion"));
                        String notaTrimestral = obtenerValorSeguro(trimestreSnapshot.child("nota trimestral"));

                        stringBuilder.append("📌 ").append(trimestreNombre).append("\n");
                        stringBuilder.append("   • SER: ").append(ser).append("\n");
                        stringBuilder.append("   • SABER: ").append(saber).append("\n");
                        stringBuilder.append("   • HACER: ").append(hacer).append("\n");
                        stringBuilder.append("   • PROMEDIO: ").append(promedio).append("\n");
                        stringBuilder.append("   • TOTAL: ").append(total).append("\n");
                        stringBuilder.append("   • AUTOEVALUACIÓN: ").append(autoevaluacion).append("\n");
                        stringBuilder.append("   • NOTA PARCIAL: ").append(notaParcial).append("\n");
                        stringBuilder.append("   • PONDERACIÓN: ").append(ponderacion).append("\n");
                        stringBuilder.append("   • NOTA TRIMESTRAL: ").append(notaTrimestral).append("\n\n");
                    } else {
                        stringBuilder.append("📌 ").append(trimestreNombre).append("\n");
                        stringBuilder.append("   ❌ Sin datos registrados\n\n");
                    }
                }

                binding.textNotas.setText(stringBuilder.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String errorMessage = "Error al cargar datos: " + error.getMessage();
                binding.textNotas.setText(errorMessage);
                Toast.makeText(requireContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ============================================================
    // OBTENER VALOR SEGURO
    // ============================================================
    private String obtenerValorSeguro(DataSnapshot snapshot) {
        if (snapshot.exists() && snapshot.getValue() != null) {
            Object valor = snapshot.getValue();
            return valor.toString();
        }
        return "--";
    }

    // ============================================================
    // EXPORTAR A PDF
    // ============================================================
    private void exportarPDF() {
        if (TextUtils.isEmpty(currentStudentUid)) {
            Toast.makeText(requireContext(), "Primero busca un estudiante", Toast.LENGTH_SHORT).show();
            return;
        }

        String materiaActual = binding.textMateriaTitulo.getText().toString();
        String contenidoNotas = binding.textNotas.getText().toString();

        if (TextUtils.isEmpty(contenidoNotas) || contenidoNotas.contains("Cargando")) {
            Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        generarPDF(materiaActual, contenidoNotas);
    }

    // ============================================================
    // GENERAR PDF
    // ============================================================
    private void generarPDF(String materia, String contenido) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();

        try {
            // Crear página A4
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Título
            paint.setColor(0xFF333333);
            paint.setTextSize(24);
            paint.setFakeBoldText(true);
            canvas.drawText("BOLETÍN DE NOTAS", 150, 60, paint);

            // Información
            paint.setTextSize(16);
            paint.setFakeBoldText(false);
            String nombreEstudiante = mapaUidANombre.getOrDefault(currentStudentUid, currentStudentUid);
            canvas.drawText("Estudiante: " + nombreEstudiante, 40, 110, paint);
            canvas.drawText("Materia: " + materia, 40, 140, paint);
            canvas.drawText("Fecha: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date()), 40, 170, paint);

            // Línea separadora
            paint.setStrokeWidth(2);
            canvas.drawLine(40, 190, 555, 190, paint);

            // Contenido de notas
            paint.setTextSize(12);
            int posicionY = 220;

            String[] lineas = contenido.split("\n");

            for (String linea : lineas) {
                if (posicionY > 800) {
                    pdfDocument.finishPage(page);
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    posicionY = 40;
                }
                canvas.drawText(linea, 40, posicionY, paint);
                posicionY += 20;
            }

            pdfDocument.finishPage(page);

            // Guardar archivo
            File directorio = new File(requireContext().getExternalFilesDir(null), "Boletines");
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            String nombreArchivo = "Boletin_" + currentStudentUid + "_" + materia + ".pdf";
            File archivoPDF = new File(directorio, nombreArchivo);

            FileOutputStream outputStream = new FileOutputStream(archivoPDF);
            pdfDocument.writeTo(outputStream);
            outputStream.close();

            Toast.makeText(requireContext(),
                    "✅ PDF guardado en:\n" + archivoPDF.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(requireContext(),
                    "❌ Error al crear PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}