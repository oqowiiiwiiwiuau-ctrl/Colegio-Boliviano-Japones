package com.graficar.colegio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class ProfeActivity extends AppCompatActivity {

    private static final String TAG = "ProfeActivity";

    // ============================================================
    // UI
    // ============================================================
    private RecyclerView recyclerViewCursos;
    private CursoAdapter cursoAdapter;
    private TextView tvTitulo;
    private TextView tvNombreProfesor;
    private TextView tvEstado;

    // ============================================================
    // FIREBASE
    // ============================================================
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    // ============================================================
    // DATOS DEL PROFESOR
    // ============================================================
    private String authUid;
    private String idLocalProfesor;
    private String nombreProfesor;

    // ============================================================
    // LISTA DE GRADOS (uno por cada grado donde da clases)
    // ============================================================
    private final List<Curso> listaCursos = new ArrayList<>();

    // ============================================================
    // ON CREATE
    // ============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profe);

        // Vistas
        tvTitulo = findViewById(R.id.tvTituloColegio);
        tvNombreProfesor = findViewById(R.id.tvNombreProfesor);
        tvEstado = findViewById(R.id.tvEstado);
        recyclerViewCursos = findViewById(R.id.recyclerViewCursos);

        tvTitulo.setText("Unidad Educativa Técnico Humanístico\n\"Boliviano Japonés\"");

        // RecyclerView con GridLayoutManager de 2 columnas
        recyclerViewCursos.setLayoutManager(new GridLayoutManager(this, 2));

        cursoAdapter = new CursoAdapter(listaCursos);
        recyclerViewCursos.setAdapter(cursoAdapter);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Cargar datos del profesor
        cargarDatosProfesor();
    }

    // ============================================================
    // CARGAR DATOS DEL PROFESOR LOGUEADO
    // ============================================================
    private void cargarDatosProfesor() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            mostrarError("No hay sesión activa");
            return;
        }

        authUid = user.getUid();

        // 1. Buscar en authIndex
        dbRef.child("authIndex").child(authUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            mostrarError("Usuario no registrado en el sistema");
                            return;
                        }

                        String tipo = snapshot.child("tipo").getValue(String.class);
                        idLocalProfesor = snapshot.child("idLocal").getValue(String.class);

                        if (!"profesor".equals(tipo)) {
                            mostrarError("Esta pantalla es solo para profesores");
                            return;
                        }

                        // 2. Cargar datos completos del profesor
                        cargarAsignaciones();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        mostrarError("Error: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // CARGAR ASIGNACIONES DEL PROFESOR
    // ============================================================
    private void cargarAsignaciones() {
        dbRef.child("profesores").child(idLocalProfesor)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            mostrarError("Datos del profesor no encontrados");
                            return;
                        }

                        // Nombre del profesor
                        nombreProfesor = snapshot.child("nombre").getValue(String.class);
                        if (tvNombreProfesor != null && nombreProfesor != null) {
                            tvNombreProfesor.setText("Bienvenido Profesor, " + nombreProfesor);
                        }

                        // Leer asignaciones
                        DataSnapshot asignacionesSnap = snapshot.child("asignaciones");

                        if (!asignacionesSnap.exists()) {
                            mostrarError("No tienes asignaciones registradas");
                            return;
                        }

                        // Agrupar por grado: Map<grado, List<materia>>
                        Map<String, List<String>> gradosConMaterias = new HashMap<>();

                        for (DataSnapshot asig : asignacionesSnap.getChildren()) {
                            String grado = asig.child("grado").getValue(String.class);
                            String materia = asig.child("materia").getValue(String.class);

                            if (grado != null && materia != null) {
                                gradosConMaterias
                                        .computeIfAbsent(grado, k -> new ArrayList<>())
                                        .add(materia);
                            }
                        }

                        if (gradosConMaterias.isEmpty()) {
                            mostrarError("No tienes grados asignados");
                            return;
                        }

                        // 3. Construir la lista de cursos (uno por grado)
                        construirListaCursos(gradosConMaterias);

                        // Ocultar estado y mostrar lista
                        if (tvEstado != null) tvEstado.setVisibility(View.GONE);
                        recyclerViewCursos.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        mostrarError("Error: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // CONSTRUIR LISTA DE CURSOS (UNO POR GRADO)
    // ============================================================
    private void construirListaCursos(Map<String, List<String>> gradosConMaterias) {
        listaCursos.clear();

        for (Map.Entry<String, List<String>> entry : gradosConMaterias.entrySet()) {
            String grado = entry.getKey();           // "Primero A"
            List<String> materias = entry.getValue(); // ["lenguaje", "historia"]

            // Color e icono según el grado
            int colorResId = obtenerColorPorGrado(grado);
            int iconoResId = android.R.drawable.ic_menu_edit;

            // Descripción: cantidad de materias
            String descripcion = materias.size() + " materia" + (materias.size() == 1 ? "" : "s");

            // Cantidad de estudiantes (se cargará después)
            int cantidadEstudiantes = 0;

            Curso curso = new Curso(
                    grado,
                    descripcion,
                    colorResId,
                    iconoResId,
                    cantidadEstudiantes,
                    materias
            );

            listaCursos.add(curso);
        }

        // Ordenar por nombre de grado (Primero, Segundo, Tercero, ...)
        listaCursos.sort((a, b) -> a.getNombre().compareTo(b.getNombre()));

        // Notificar al adapter
        cursoAdapter.notifyDataSetChanged();

        // 4. Cargar cantidad de estudiantes por grado (opcional, mejora UX)
        cargarCantidadEstudiantes();
    }

    // ============================================================
    // OBTENER COLOR SEGÚN EL GRADO
    // ============================================================
    private int obtenerColorPorGrado(String grado) {
        if (grado == null) return R.color.curso_primero;

        String gradoLower = grado.toLowerCase();

        if (gradoLower.contains("primero")) return R.color.curso_primero;
        if (gradoLower.contains("segundo")) return R.color.curso_segundo;
        if (gradoLower.contains("tercero")) return R.color.curso_tercero;
        if (gradoLower.contains("cuarto")) return R.color.curso_cuarto;
        if (gradoLower.contains("quinto")) return R.color.curso_quinto;
        if (gradoLower.contains("sexto")) return R.color.curso_sexto;

        return R.color.curso_primero;
    }

    // ============================================================
    // CARGAR CANTIDAD DE ESTUDIANTES POR GRADO
    // ============================================================
    private void cargarCantidadEstudiantes() {
        dbRef.child("estudiantesPorGrado")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (Curso curso : listaCursos) {
                            String gradoKey = normalizarGradoKey(curso.getNombre());
                            DataSnapshot gradoSnap = snapshot.child(gradoKey);

                            if (gradoSnap.exists()) {
                                curso.setCantidadEstudiantes((int) gradoSnap.getChildrenCount());
                            } else {
                                curso.setCantidadEstudiantes(0);
                            }
                        }
                        cursoAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al cargar estudiantes: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // NORMALIZAR NOMBRE DE GRADO A KEY DE FIREBASE
    // "Primero A" → "primeroA"
    // "Tercero B" → "terceroB"
    // ============================================================
    private String normalizarGradoKey(String grado) {
        if (grado == null) return "";

        // Quitar espacios y convertir a camelCase
        String[] partes = grado.trim().split("\\s+");
        if (partes.length < 2) return grado.toLowerCase();

        String numero = partes[0].toLowerCase();  // "primero"
        String letra = partes[1].toUpperCase();   // "A"

        return numero + letra;
    }

    // ============================================================
    // ABRIR DETALLE DEL CURSO (MateriasActivity)
    // ============================================================
    private void abrirDetalleCurso(Curso curso) {
        Intent intent = new Intent(ProfeActivity.this, MateriasActivity.class);
        intent.putExtra("curso_nombre", curso.getNombre());
        intent.putExtra("curso_color", curso.getColorResId());
        intent.putExtra("curso_estudiantes", curso.getCantidadEstudiantes());
        intent.putExtra("idLocalProfesor", idLocalProfesor);
        intent.putExtra("nombreProfesor", nombreProfesor);

        // Pasar las materias que da el profesor en ese grado
        intent.putStringArrayListExtra("materias",
                new ArrayList<>(curso.getMaterias()));

        startActivity(intent);
    }

    // ============================================================
    // MOSTRAR ERROR
    // ============================================================
    private void mostrarError(String mensaje) {
        if (tvEstado != null) {
            tvEstado.setText(mensaje);
            tvEstado.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvEstado.setVisibility(View.VISIBLE);
        }
        recyclerViewCursos.setVisibility(View.GONE);
    }

    // ============================================================
    // CLASE CURSO
    // ============================================================
    public static class Curso {
        private String nombre;              // "Primero A"
        private String descripcion;         // "2 materias"
        private int colorResId;
        private int iconoResId;
        private int cantidadEstudiantes;
        private List<String> materias;      // ["lenguaje", "historia"]

        public Curso(String nombre, String descripcion, int colorResId,
                     int iconoResId, int cantidadEstudiantes, List<String> materias) {
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.colorResId = colorResId;
            this.iconoResId = iconoResId;
            this.cantidadEstudiantes = cantidadEstudiantes;
            this.materias = materias != null ? materias : new ArrayList<>();
        }

        public String getNombre() { return nombre; }
        public String getDescripcion() { return descripcion; }
        public int getColorResId() { return colorResId; }
        public int getIconoResId() { return iconoResId; }
        public int getCantidadEstudiantes() { return cantidadEstudiantes; }
        public List<String> getMaterias() { return materias; }

        public void setCantidadEstudiantes(int cantidad) {
            this.cantidadEstudiantes = cantidad;
        }
    }

    // ============================================================
    // ADAPTER
    // ============================================================
    private class CursoAdapter extends RecyclerView.Adapter<CursoAdapter.CursoViewHolder> {
        private final List<Curso> cursos;

        public CursoAdapter(List<Curso> cursos) {
            this.cursos = cursos;
        }

        @NonNull
        @Override
        public CursoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_curso, parent, false);
            return new CursoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CursoViewHolder holder, int position) {
            Curso curso = cursos.get(position);
            holder.bind(curso);
        }

        @Override
        public int getItemCount() {
            return cursos.size();
        }

        class CursoViewHolder extends RecyclerView.ViewHolder {
            private final CardView cardView;
            private final TextView tvNombreCurso;
            private final TextView tvDescripcion;
            private final TextView tvEstudiantes;
            private final TextView tvNumeroCurso;

            public CursoViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.cardViewCurso);
                tvNombreCurso = itemView.findViewById(R.id.tvNombreCurso);
                tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
                tvEstudiantes = itemView.findViewById(R.id.tvEstudiantes);
                tvNumeroCurso = itemView.findViewById(R.id.tvNumeroCurso);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        abrirDetalleCurso(cursos.get(position));
                    }
                });
            }

            public void bind(Curso curso) {
                cardView.setCardBackgroundColor(getResources().getColor(curso.getColorResId()));
                tvNombreCurso.setText(curso.getNombre());
                tvDescripcion.setText(curso.getDescripcion());
                tvEstudiantes.setText(curso.getCantidadEstudiantes() + " estudiantes");

                // Número del curso
                String nombre = curso.getNombre();
                if (nombre.contains("Primero")) tvNumeroCurso.setText("1°");
                else if (nombre.contains("Segundo")) tvNumeroCurso.setText("2°");
                else if (nombre.contains("Tercero")) tvNumeroCurso.setText("3°");
                else if (nombre.contains("Cuarto")) tvNumeroCurso.setText("4°");
                else if (nombre.contains("Quinto")) tvNumeroCurso.setText("5°");
                else if (nombre.contains("Sexto")) tvNumeroCurso.setText("6°");
            }
        }
    }
}