package com.graficar.colegio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.graficar.colegio.MainActivity;
import com.graficar.colegio.ui.ProfeActivity;
import com.graficar.colegio.R;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewError;

    // Credenciales de prueba
    private static final String VALID_USERNAME = "admi";
    private static final String VALID_PASSWORD = "admi";
    private static final String TEACHER_USERNAME = "profe";
    private static final String TEACHER_PASSWORD = "profe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_login);

        // 1. Inicializar las vistas por sus IDs
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewError = findViewById(R.id.textViewError);

        // 2. Establecer el listener para el botón "Entrar"
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        // Ocultar el mensaje de error al inicio
        if (textViewError != null) {
            textViewError.setVisibility(View.GONE);
        }
    }

    /**
     * Intenta iniciar sesión verificando las credenciales.
     */
    private void attemptLogin() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // 3. Lógica de Validación
        if (username.equals(VALID_USERNAME) && password.equals(VALID_PASSWORD)) {
            // Acceso para administrador normal
            handleSuccessfulLogin(MainActivity.class, "Acceso concedido (Administrador)");

        } else if (username.equals(TEACHER_USERNAME) && password.equals(TEACHER_PASSWORD)) {
            // Acceso para profesor
            handleSuccessfulLogin(ProfeActivity.class, "Acceso concedido (Profesor)");

        } else {
            // Fracaso:
            showError("Usuario o contraseña incorrectos.");
        }
    }

    /**
     * Maneja el inicio de sesión exitoso
     */
    private void handleSuccessfulLogin(Class<?> targetActivity, String successMessage) {
        if (textViewError != null) {
            textViewError.setVisibility(View.GONE);
        }
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();

        // Navegar a la actividad correspondiente
        Intent intent = new Intent(LoginActivity.this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Muestra mensaje de error
     */
    private void showError(String errorMessage) {
        if (textViewError != null) {
            textViewError.setText(errorMessage);
            textViewError.setVisibility(View.VISIBLE);
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
}