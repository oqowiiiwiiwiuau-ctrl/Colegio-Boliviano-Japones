package com.graficar.colegio.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.graficar.colegio.R;

public class matematicaVerTrimestresPrimeroA extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_matematica_ver_trimestres_primero_a
        );

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
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

        // =====================================================
        // PRIMER TRIMESTRE
        // =====================================================

        findViewById(R.id.cardPrimerTrimestre)
                .setOnClickListener(v -> {

                    Intent intent = new Intent(
                            matematicaVerTrimestresPrimeroA.this,
                            PrimeroTrimestreUno.class
                    );

                    startActivity(intent);
                });


        // =====================================================
        // SEGUNDO TRIMESTRE
        // =====================================================

        findViewById(R.id.cardSegundoTrimestre)
                .setOnClickListener(v -> {

                    Intent intent = new Intent(
                            matematicaVerTrimestresPrimeroA.this,
                            PrimeroTrimestreDos.class
                    );

                    startActivity(intent);
                });


        // =====================================================
        // TERCER TRIMESTRE
        // =====================================================

        findViewById(R.id.cardTercerTrimestre)
                .setOnClickListener(v -> {

                    Intent intent = new Intent(
                            matematicaVerTrimestresPrimeroA.this,
                            PrimeroTrimestreTres.class
                    );

                    startActivity(intent);
                });

        // =====================================================
        // CENTRALIZADOR
        // =====================================================
        findViewById(R.id.btnCentralizador)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(
                            matematicaVerTrimestresPrimeroA.this,
                            Centralizador.class
                    );
                    startActivity(intent);
                });
    }

}