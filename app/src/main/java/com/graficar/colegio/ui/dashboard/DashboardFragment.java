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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.graficar.colegio.databinding.FragmentDashboardBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DASHBOARD";

    private FragmentDashboardBinding binding;
    private SharedPreferences prefs;

    // ============================================================
    // CONFIGURACIÓN
    // ============================================================
    private String gradoActual = "primeroC";
    private final String[] trimestres = {"trimestre1", "trimestre2", "trimestre3"};
    private final String[] trimestresNombres = {"Trimestre 1", "Trimestre 2", "Trimestre 3"};
    private final String[] materias = {"Matemática"};

    // ============================================================
    // FIREBASE
    // ============================================================
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;

    // ============================================================
    // DATOS DEL PADRE Y SUS HIJOS
    // ============================================================
    private String authUid;
    private String idLocalPadre;
    private String nombrePadre;

    // Listas paralelas: en la posición i está el UID y el nombre del hijo i
    private final List<String> hijosUids = new ArrayList<>();
    private final List<String> hijosNombres = new ArrayList<>();

    // ============================================================
    // HIJO ACTUALMENTE SELECCIONADO
    // ============================================================
    private String currentStudentUid = "";
    private String currentStudentName = "";

    // ============================================================
    // ON CREATE VIEW
    // ============================================================
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        prefs = requireContext().getSharedPreferences("DASHBOARD_DATA", Context.MODE_PRIVATE);

        // Firebase
        dbRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Visibilidad inicial
        setupInitialVisibility();

        // Configurar spinner de materias
        setupSpinnerMaterias();

        // Configurar botón de PDF
        setupPdfButton();

        // Cargar datos del padre logueado
        cargarPadreYHijos();

        return root;
    }

    // ============================================================
    // VISIBILIDAD INICIAL
    // ============================================================
    private void setupInitialVisibility() {
        // Ocultar elementos hasta que carguen los hijos
        binding.spinnerHijos.setVisibility(View.GONE);
        binding.textStudentName.setVisibility(View.GONE);
        binding.spinnerMaterias.setVisibility(View.GONE);
        binding.cardNotas.setVisibility(View.GONE);
        binding.btnExportarPDF.setVisibility(View.GONE);

        binding.textNotas.setText("🔄 Cargando datos del apoderado...");
        binding.textNotas.setVisibility(View.VISIBLE);
    }

    // ============================================================
    // CARGAR PADRE Y SUS HIJOS
    // ============================================================
    private void cargarPadreYHijos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            binding.textNotas.setText("❌ No hay sesión activa");
            return;
        }

        authUid = user.getUid();

        // 1. Buscar en authIndex
        dbRef.child("authIndex").child(authUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            binding.textNotas.setText("❌ Usuario no registrado en el sistema");
                            return;
                        }

                        String tipo = snapshot.child("tipo").getValue(String.class);
                        idLocalPadre = snapshot.child("idLocal").getValue(String.class);

                        if (!"apoderado".equals(tipo)) {
                            binding.textNotas.setText("⚠️ Esta pantalla es solo para apoderados");
                            return;
                        }

                        cargarDatosPadre();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.textNotas.setText("Error: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // CARGAR DATOS DEL PADRE
    // ============================================================
    private void cargarDatosPadre() {
        dbRef.child("usuarios").child(idLocalPadre)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            binding.textNotas.setText("❌ Datos del apoderado no encontrados");
                            return;
                        }

                        nombrePadre = snapshot.child("nombre").getValue(String.class);

                        // Obtener los UIDs de los hijos
                        hijosUids.clear();
                        for (DataSnapshot hijo : snapshot.child("estudiantes").getChildren()) {
                            String uid = hijo.getValue(String.class);
                            if (uid != null) {
                                hijosUids.add(uid);
                            }
                        }

                        if (hijosUids.isEmpty()) {
                            binding.textNotas.setText("No tienes hijos registrados");
                            return;
                        }

                        // Cargar los nombres de los hijos
                        cargarNombresHijos();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.textNotas.setText("Error: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // CARGAR NOMBRES DE LOS HIJOS
    // ============================================================
    private void cargarNombresHijos() {
        hijosNombres.clear();
        final int total = hijosUids.size();
        final int[] cargados = {0};

        for (String uid : hijosUids) {
            dbRef.child("estudiantes").child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String nombre = snapshot.child("nombre").getValue(String.class);
                            hijosNombres.add(nombre != null ? nombre : "Estudiante");

                            cargados[0]++;
                            if (cargados[0] == total) {
                                // Ya tenemos todos los nombres
                                setupSpinnerHijos();
                                mostrarElementosUI();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            hijosNombres.add("Estudiante");
                            cargados[0]++;
                            if (cargados[0] == total) {
                                setupSpinnerHijos();
                                mostrarElementosUI();
                            }
                        }
                    });
        }
    }

    // ============================================================
    // CONFIGURAR SPINNER DE HIJOS
    // ============================================================
    private void setupSpinnerHijos() {
        if (!isAdded() || binding == null) return;

        Spinner spinner = binding.spinnerHijos;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                hijosNombres
        );
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < hijosUids.size()) {
                    currentStudentUid = hijosUids.get(position);
                    currentStudentName = hijosNombres.get(position);

                    binding.textStudentName.setText("👤 " + currentStudentName);
                    binding.textStudentName.setVisibility(View.VISIBLE);

                    // Cargar las notas del hijo seleccionado
                    String primeraMateria = materias[0];
                    binding.textMateriaTitulo.setText(primeraMateria);
                    cargarDatosEstudiante(currentStudentUid, primeraMateria);

                    // Guardar selección
                    prefs.edit().putInt("LAST_HIJO_INDEX", position).apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Seleccionar el último hijo usado (si existe)
        int lastIndex = prefs.getInt("LAST_HIJO_INDEX", 0);
        if (lastIndex < hijosNombres.size()) {
            spinner.setSelection(lastIndex);
        }
    }

    // ============================================================
    // MOSTRAR ELEMENTOS UI
    // ============================================================
    private void mostrarElementosUI() {
        if (binding == null) return;

        binding.spinnerHijos.setVisibility(View.VISIBLE);
        binding.spinnerMaterias.setVisibility(View.VISIBLE);
        binding.cardNotas.setVisibility(View.VISIBLE);
        binding.btnExportarPDF.setVisibility(View.VISIBLE);
    }

    // ============================================================
    // CONFIGURAR SPINNER DE MATERIAS
    // ============================================================
    private void setupSpinnerMaterias() {
        Spinner spinner = binding.spinnerMaterias;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                materias
        );
        spinner.setAdapter(adapter);

        spinner.setSelection(prefs.getInt("LAST_MATERIA_INDEX", 0));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!TextUtils.isEmpty(currentStudentUid)) {
                    String materiaSeleccionada = materias[position];
                    binding.textMateriaTitulo.setText(materiaSeleccionada);
                    cargarDatosEstudiante(currentStudentUid, materiaSeleccionada);
                    prefs.edit().putInt("LAST_MATERIA_INDEX", position).apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ============================================================
    // CONFIGURAR BOTÓN PDF
    // ============================================================
    private void setupPdfButton() {
        binding.btnExportarPDF.setOnClickListener(v -> exportarPDF());
    }

    // ============================================================
    // CARGAR DATOS DEL ESTUDIANTE
    // ============================================================
    private void cargarDatosEstudiante(String uid, String materia) {
        binding.textNotas.setText("📥 Cargando datos de " + materia + "...");

        DatabaseReference ref = dbRef
                .child("calificaciones")
                .child(gradoActual)
                .child(uid);

        String nombreEstudiante = currentStudentName;

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    binding.textNotas.setText("❌ No se encontraron datos para " + nombreEstudiante);
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("📊 NOTAS DE ").append(nombreEstudiante.toUpperCase()).append("\n");
                sb.append("📚 Materia: ").append(materia).append("\n");
                sb.append("─────────────────────\n\n");

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

                        sb.append("📌 ").append(trimestreNombre).append("\n");
                        sb.append("   • SER: ").append(ser).append("\n");
                        sb.append("   • SABER: ").append(saber).append("\n");
                        sb.append("   • HACER: ").append(hacer).append("\n");
                        sb.append("   • PROMEDIO: ").append(promedio).append("\n");
                        sb.append("   • TOTAL: ").append(total).append("\n");
                        sb.append("   • AUTOEVALUACIÓN: ").append(autoevaluacion).append("\n");
                        sb.append("   • NOTA PARCIAL: ").append(notaParcial).append("\n");
                        sb.append("   • PONDERACIÓN: ").append(ponderacion).append("\n");
                        sb.append("   • NOTA TRIMESTRAL: ").append(notaTrimestral).append("\n\n");
                    } else {
                        sb.append("📌 ").append(trimestreNombre).append("\n");
                        sb.append("   ❌ Sin datos registrados\n\n");
                    }
                }

                binding.textNotas.setText(sb.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.textNotas.setText("Error al cargar datos: " + error.getMessage());
            }
        });
    }

    // ============================================================
    // OBTENER VALOR SEGURO
    // ============================================================
    private String obtenerValorSeguro(DataSnapshot snapshot) {
        if (snapshot.exists() && snapshot.getValue() != null) {
            return snapshot.getValue().toString();
        }
        return "--";
    }

    // ============================================================
    // EXPORTAR A PDF
    // ============================================================
    private void exportarPDF() {
        if (TextUtils.isEmpty(currentStudentUid)) {
            Toast.makeText(requireContext(), "Primero selecciona un estudiante", Toast.LENGTH_SHORT).show();
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
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Título
            paint.setColor(0xFF333333);
            paint.setTextSize(24);
            paint.setFakeBoldText(true);
            canvas.drawText("BOLETÍN DE NOTAS", 150, 60, paint);

            // Info
            paint.setTextSize(16);
            paint.setFakeBoldText(false);
            canvas.drawText("Estudiante: " + currentStudentName, 40, 110, paint);
            canvas.drawText("Materia: " + materia, 40, 140, paint);
            canvas.drawText("Fecha: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date()), 40, 170, paint);

            // Línea
            paint.setStrokeWidth(2);
            canvas.drawLine(40, 190, 555, 190, paint);

            // Contenido
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

            // Guardar
            File directorio = new File(requireContext().getExternalFilesDir(null), "Boletines");
            if (!directorio.exists()) directorio.mkdirs();

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
            pdfDocument.close();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}