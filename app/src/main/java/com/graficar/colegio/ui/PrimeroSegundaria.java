package com.graficar.colegio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.graficar.colegio.R;

public class PrimeroSegundaria extends AppCompatActivity {

    CardView cardMatematica, cardLengua;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_primero_segundaria);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cardMatematica = findViewById(R.id.cardMatematica);
        cardLengua = findViewById(R.id.cardLengua);

        // 👉 Abrir Activity de Matemática
        cardMatematica.setOnClickListener(v -> {
            Intent intent = new Intent(
                    PrimeroSegundaria.this,
                    PrimeroActivity.class
            );
            startActivity(intent);
        });

        // 👉 Lengua (por ahora solo mensaje)
        cardLengua.setOnClickListener(v ->
                Toast.makeText(this, "Lengua seleccionada", Toast.LENGTH_SHORT).show()
        );
    }
}
