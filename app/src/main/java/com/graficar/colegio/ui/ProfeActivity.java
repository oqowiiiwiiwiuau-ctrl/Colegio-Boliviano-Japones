package com.graficar.colegio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.graficar.colegio.R;

import java.util.ArrayList;
import java.util.List;

public class ProfeActivity extends AppCompatActivity {

    private RecyclerView recyclerViewCursos;
    private CursoAdapter cursoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profe);

        // Configurar título
        TextView tvTitulo = findViewById(R.id.tvTituloColegio);
        tvTitulo.setText("Unidad Educativa Técnico Humanístico\n\"Boliviano Japonés\"");

        // Configurar RecyclerView
        recyclerViewCursos = findViewById(R.id.recyclerViewCursos);
        recyclerViewCursos.setLayoutManager(new GridLayoutManager(this, 2));

        // Crear lista de cursos
        List<Curso> cursos = obtenerListaCursos();

        // Configurar adapter
        cursoAdapter = new CursoAdapter(cursos);
        recyclerViewCursos.setAdapter(cursoAdapter);
    }

    private List<Curso> obtenerListaCursos() {
        List<Curso> cursos = new ArrayList<>();

        // Colores formales para los cursos
        int[] colores = {
                R.color.curso_primero,
                R.color.curso_segundo,
                R.color.curso_tercero,
                R.color.curso_cuarto,
                R.color.curso_quinto,
                R.color.curso_sexto
        };

        String[] nombresCursos = {
                "Primero de Secundaria",
                "Segundo de Secundaria",
                "Tercero de Secundaria",
                "Cuarto de Secundaria",
                "Quinto de Secundaria",
                "Sexto de Secundaria"
        };

        String[] descripciones = {
                "Ciclo Básico - Inicial",
                "Ciclo Básico - Intermedio",
                "Ciclo Básico - Avanzado",
                "Ciclo Diversificado - Inicial",
                "Ciclo Diversificado - Intermedio",
                "Ciclo Diversificado - Final"
        };

        int[] iconos = {
                android.R.drawable.ic_menu_edit,      // Icono de editar
                android.R.drawable.ic_menu_save,      // Icono de guardar
                android.R.drawable.ic_menu_search,    // Icono de buscar
                android.R.drawable.ic_menu_help,      // Icono de ayuda
                android.R.drawable.ic_menu_preferences, // Icono de preferencias
                android.R.drawable.ic_menu_gallery    // Icono de galería
        };

        for (int i = 0; i < nombresCursos.length; i++) {
            cursos.add(new Curso(
                    nombresCursos[i],
                    descripciones[i],
                    colores[i],
                    iconos[i],
                    25 + i // Número de estudiantes ficticio
            ));
        }

        return cursos;
    }

    // Clase para representar un curso
    public static class Curso {
        private String nombre;
        private String descripcion;
        private int colorResId;
        private int iconoResId;
        private int cantidadEstudiantes;

        public Curso(String nombre, String descripcion, int colorResId, int iconoResId, int cantidadEstudiantes) {
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.colorResId = colorResId;
            this.iconoResId = iconoResId;
            this.cantidadEstudiantes = cantidadEstudiantes;
        }

        public String getNombre() { return nombre; }
        public String getDescripcion() { return descripcion; }
        public int getColorResId() { return colorResId; }
        public int getIconoResId() { return iconoResId; }
        public int getCantidadEstudiantes() { return cantidadEstudiantes; }
    }

    // Adapter para la lista de cursos
    private class CursoAdapter extends RecyclerView.Adapter<CursoAdapter.CursoViewHolder> {
        private List<Curso> cursos;

        public CursoAdapter(List<Curso> cursos) {
            this.cursos = cursos;
        }

        @Override
        public CursoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_curso, parent, false);
            return new CursoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CursoViewHolder holder, int position) {
            Curso curso = cursos.get(position);
            holder.bind(curso);
        }

        @Override
        public int getItemCount() {
            return cursos.size();
        }

        class CursoViewHolder extends RecyclerView.ViewHolder {
            private CardView cardView;
            private TextView tvNombreCurso;
            private TextView tvDescripcion;
            private TextView tvEstudiantes;
            private TextView tvNumeroCurso;

            public CursoViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.cardViewCurso);
                tvNombreCurso = itemView.findViewById(R.id.tvNombreCurso);
                tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
                tvEstudiantes = itemView.findViewById(R.id.tvEstudiantes);
                tvNumeroCurso = itemView.findViewById(R.id.tvNumeroCurso);

                // Configurar clic en la tarjeta
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            Curso curso = cursos.get(position);
                            abrirDetalleCurso(curso);
                        }
                    }
                });
            }

            public void bind(Curso curso) {
                // Configurar color de fondo
                cardView.setCardBackgroundColor(getResources().getColor(curso.getColorResId()));

                // Configurar textos
                tvNombreCurso.setText(curso.getNombre());
                tvDescripcion.setText(curso.getDescripcion());
                tvEstudiantes.setText(curso.getCantidadEstudiantes() + " estudiantes");

                // Configurar número del curso
                String nombre = curso.getNombre();
                if (nombre.contains("Primero")) tvNumeroCurso.setText("1°");
                else if (nombre.contains("Segundo")) tvNumeroCurso.setText("2°");
                else if (nombre.contains("Tercero")) tvNumeroCurso.setText("3°");
                else if (nombre.contains("Cuarto")) tvNumeroCurso.setText("4°");
                else if (nombre.contains("Quinto")) tvNumeroCurso.setText("5°");
                else if (nombre.contains("Sexto")) tvNumeroCurso.setText("6°");

                // Configurar icono
                // tvNumeroCurso.setCompoundDrawablesWithIntrinsicBounds(curso.getIconoResId(), 0, 0, 0);
            }
        }
    }

    private void abrirDetalleCurso(Curso curso) {
        // Verificar qué curso se seleccionó
        if (curso.getNombre().contains("Primero")) {
            Intent intent = new Intent(ProfeActivity.this, PrimeroSegundaria.class);
            intent.putExtra("curso_nombre", curso.getNombre());
            intent.putExtra("curso_color", curso.getColorResId());
            intent.putExtra("curso_estudiantes", curso.getCantidadEstudiantes());
            startActivity(intent);
        }
        // Aquí puedes agregar más condiciones para otros cursos
        else {
            Toast.makeText(this, "Abriendo: " + curso.getNombre(), Toast.LENGTH_SHORT).show();
        }
    }
}