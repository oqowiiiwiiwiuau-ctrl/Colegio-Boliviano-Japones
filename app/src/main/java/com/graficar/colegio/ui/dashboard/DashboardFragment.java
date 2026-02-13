package com.graficar.colegio.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.text.TextUtils;
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

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private SharedPreferences prefs;

    // Materias
    private final String[] materias = {
            "Matemática", "Física", "Química", "Historia",
            "Lenguaje", "Educación Física", "Religión"
    };

    // Variable para almacenar el nombre del estudiante actual
    private String currentStudentName = "";

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

        return root;
    }

    /**
     * Configura la visibilidad inicial de los elementos
     */
    private void setupInitialVisibility() {
        binding.textStudentName.setVisibility(View.GONE);
        binding.spinnerMaterias.setVisibility(View.GONE);
        binding.cardNotas.setVisibility(View.GONE);
        binding.btnExportarPDF.setVisibility(View.GONE);
    }

    /**
     * Configura el spinner de materias
     */
    private void setupSpinner() {
        Spinner spinner = binding.spinnerMaterias;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                materias
        );
        spinner.setAdapter(adapter);

        // Cargar selección anterior
        spinner.setSelection(prefs.getInt("LAST_MATERIA_INDEX", 0));

        // Listener para cuando se selecciona una materia
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (!TextUtils.isEmpty(currentStudentName)) {
                    String materiaSeleccionada = materias[position];
                    binding.textMateriaTitulo.setText(materiaSeleccionada);
                    cargarDatosEstudiante(currentStudentName, materiaSeleccionada);

                    // Guardar preferencia
                    prefs.edit().putInt("LAST_MATERIA_INDEX", position).apply();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    /**
     * Configura el botón de búsqueda
     */
    private void setupSearchButton() {
        binding.btnBuscarDatos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buscarDatosEstudiante();
            }
        });
    }

    /**
     * Configura el botón de exportar PDF
     */
    private void setupPdfButton() {
        binding.btnExportarPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportarPDF();
            }
        });
    }

    /**
     * Busca los datos del estudiante ingresado
     */
    private void buscarDatosEstudiante() {
        String nombreEstudiante = binding.editTextStudentName.getText().toString().trim();

        // Validar entrada
        if (TextUtils.isEmpty(nombreEstudiante)) {
            Toast.makeText(requireContext(), "Por favor ingresa un nombre de estudiante", Toast.LENGTH_SHORT).show();
            return;
        }

        // Establecer el nombre del estudiante actual
        currentStudentName = nombreEstudiante;
        binding.textStudentName.setText("Estudiante: " + currentStudentName);

        // Mostrar elementos de la UI
        mostrarElementosUI();

        // Cargar datos de la primera materia por defecto
        String primeraMateria = materias[0];
        binding.textMateriaTitulo.setText(primeraMateria);
        cargarDatosEstudiante(currentStudentName, primeraMateria);
    }

    /**
     * Muestra los elementos de la UI después de la búsqueda
     */
    private void mostrarElementosUI() {
        binding.textStudentName.setVisibility(View.VISIBLE);
        binding.spinnerMaterias.setVisibility(View.VISIBLE);
        binding.cardNotas.setVisibility(View.VISIBLE);
        binding.btnExportarPDF.setVisibility(View.VISIBLE);
    }

    /**
     * Carga los datos del estudiante desde Firebase Realtime Database
     */
    private void cargarDatosEstudiante(String studentName, String materia) {
        // Mostrar estado de carga
        binding.textNotas.setText("Cargando datos de " + materia + "...");

        try {
            // Obtener referencia a Firebase con URL específica
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://colegio-71940-default-rtdb.firebaseio.com/");
            DatabaseReference ref = database.getReference("alumnos")
                    .child(studentName)
                    .child(materia);

            // Leer datos una vez
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        binding.textNotas.setText("No se encontraron datos para " + studentName + " en " + materia);
                        return;
                    }

                    // Procesar y mostrar los datos
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Notas de ").append(studentName).append(" - ").append(materia).append("\n\n");

                    for (DataSnapshot bimestreSnapshot : snapshot.getChildren()) {
                        String nombreBimestre = bimestreSnapshot.getKey();
                        String practicas = obtenerValorSeguro(bimestreSnapshot.child("practicas"));
                        String examenFinal = obtenerValorSeguro(bimestreSnapshot.child("final"));
                        String promedio = obtenerValorSeguro(bimestreSnapshot.child("promedio"));

                        stringBuilder.append("📊 ").append(nombreBimestre).append(":\n")
                                .append("   • Prácticas: ").append(practicas).append("\n")
                                .append("   • Examen Final: ").append(examenFinal).append("\n")
                                .append("   • Promedio: ").append(promedio).append("\n\n");
                    }

                    binding.textNotas.setText(stringBuilder.toString());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    String errorMessage = "Error al cargar datos: " + error.getMessage();
                    binding.textNotas.setText(errorMessage);

                    // Mostrar Toast con más detalles
                    if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                        Toast.makeText(requireContext(),
                                "Error de permisos. Verifica las reglas de seguridad en Firebase Console",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (Exception e) {
            binding.textNotas.setText("Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Obtiene un valor seguro de DataSnapshot
     */
    private String obtenerValorSeguro(DataSnapshot snapshot) {
        if (snapshot.exists() && snapshot.getValue() != null) {
            return snapshot.getValue(String.class);
        }
        return "--";
    }

    /**
     * Exporta los datos a PDF
     */
    private void exportarPDF() {
        if (TextUtils.isEmpty(currentStudentName)) {
            Toast.makeText(requireContext(), "Primero busca un estudiante", Toast.LENGTH_SHORT).show();
            return;
        }

        String materiaActual = binding.textMateriaTitulo.getText().toString();
        String contenidoNotas = binding.textNotas.getText().toString();

        if (TextUtils.isEmpty(contenidoNotas) || contenidoNotas.equals("Cargando datos...")) {
            Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        generarPDF(materiaActual, contenidoNotas);
    }

    /**
     * Genera el archivo PDF con las notas
     */
    private void generarPDF(String materia, String contenido) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();

        try {
            // Crear página A4
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Configurar paint para el título
            paint.setColor(0xFF333333); // Color gris oscuro
            paint.setTextSize(24);
            paint.setFakeBoldText(true);
            canvas.drawText("BOLETÍN DE NOTAS", 150, 60, paint);

            // Información del estudiante y materia
            paint.setTextSize(16);
            paint.setFakeBoldText(false);
            canvas.drawText("Estudiante: " + currentStudentName, 40, 110, paint);
            canvas.drawText("Materia: " + materia, 40, 140, paint);
            canvas.drawText("Fecha: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()), 40, 170, paint);

            // Línea separadora
            paint.setStrokeWidth(2);
            canvas.drawLine(40, 190, 555, 190, paint);

            // Contenido de las notas
            paint.setTextSize(12);
            int posicionY = 220;

            // Dividir el contenido en líneas
            String[] lineas = contenido.split("\n");

            for (String linea : lineas) {
                if (posicionY > 800) { // Nueva página si se llega al final
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

            String nombreArchivo = "Boletin_" + currentStudentName + "_" + materia + ".pdf";
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
            // Cerrar el documento
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