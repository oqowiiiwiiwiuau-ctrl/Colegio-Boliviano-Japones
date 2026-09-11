package com.graficar.colegio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.graficar.colegio.R;

public class matematicaVerTrimestresPrimeroA extends AppCompatActivity {

    private static final String TAG = "MatematicaTrimestres";

    // Identificadores de este curso/materia
    private static final String MATERIA = "matematica";
    private static final String GRADO   = "primeroC";

    // Vistas
    private TextView tvNombreMaestro;
    private TextView tvEspecialidadMaestro;
    private TextView tvCorreoMaestro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_matematica_ver_trimestres_primero_a);

        // Referencias
        tvNombreMaestro       = findViewById(R.id.tvNombreMaestro);
        tvEspecialidadMaestro = findViewById(R.id.tvEspecialidadMaestro);
        tvCorreoMaestro       = findViewById(R.id.tvCorreoMaestro);

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.headerContainer).getRootView(),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                }
        );

        // Cargar datos del profesor
        cargarProfesorDesdeFirebase();

        // ============ PRIMER TRIMESTRE ============
        findViewById(R.id.cardPrimerTrimestre)
                .setOnClickListener(v -> startActivity(new Intent(
                        matematicaVerTrimestresPrimeroA.this,
                        PrimeroTrimestreUno.class
                )));

        // ============ SEGUNDO TRIMESTRE ============
        findViewById(R.id.cardSegundoTrimestre)
                .setOnClickListener(v -> startActivity(new Intent(
                        matematicaVerTrimestresPrimeroA.this,
                        PrimeroTrimestreDos.class
                )));

        // ============ TERCER TRIMESTRE ============
        findViewById(R.id.cardTercerTrimestre)
                .setOnClickListener(v -> startActivity(new Intent(
                        matematicaVerTrimestresPrimeroA.this,
                        PrimeroTrimestreTres.class
                )));

        // ============ CENTRALIZADOR ============
        findViewById(R.id.btnCentralizador)
                .setOnClickListener(v -> startActivity(new Intent(
                        matematicaVerTrimestresPrimeroA.this,
                        Centralizador.class
                )));
    }

    /**
     * Busca en el nodo "profesores" al que imparte MATERIA en GRADO
     * y muestra nombre, especialidad y correo.
     */
    private void cargarProfesorDesdeFirebase() {
        DatabaseReference profesoresRef = FirebaseDatabase
                .getInstance()
                .getReference("profesores");

        profesoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String nombre       = null;
                String especialidad = null;
                String correo       = null;

                for (DataSnapshot profSnapshot : snapshot.getChildren()) {

                    // ¿Imparte la materia?
                    boolean imparteMateria = false;
                    for (DataSnapshot m : profSnapshot.child("materias").getChildren()) {
                        String materia = m.getValue(String.class);
                        if (MATERIA.equalsIgnoreCase(materia)) {
                            imparteMateria = true;
                            break;
                        }
                    }

                    // ¿Tiene el grado?
                    boolean tieneGrado = false;
                    for (DataSnapshot g : profSnapshot.child("grados").getChildren()) {
                        String grado = g.getValue(String.class);
                        if (GRADO.equalsIgnoreCase(grado)) {
                            tieneGrado = true;
                            break;
                        }
                    }

                    if (imparteMateria && tieneGrado) {
                        nombre       = profSnapshot.child("nombre").getValue(String.class);
                        especialidad = profSnapshot.child("especialidad").getValue(String.class);
                        correo       = profSnapshot.child("email").getValue(String.class);
                        break;
                    }
                }

                if (nombre != null) {
                    tvNombreMaestro.setText(nombre);

                    if (especialidad != null && !especialidad.isEmpty()) {
                        tvEspecialidadMaestro.setText("Especialidad: " + especialidad);
                    } else {
                        tvEspecialidadMaestro.setText("Especialidad: —");
                    }

                    if (correo != null && !correo.isEmpty()) {
                        tvCorreoMaestro.setText("✉  " + correo);
                    } else {
                        tvCorreoMaestro.setText("");
                    }

                } else {
                    tvNombreMaestro.setText("Profesor no asignado");
                    tvEspecialidadMaestro.setText("");
                    tvCorreoMaestro.setText("");
                    Log.w(TAG, "No se encontró profesor para " + MATERIA + " en " + GRADO);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al leer profesores: " + error.getMessage());
                tvNombreMaestro.setText("Error al cargar");
                tvEspecialidadMaestro.setText("");
                tvCorreoMaestro.setText("");
            }
        });
    }
}