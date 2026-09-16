package com.graficar.colegio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.graficar.colegio.MainActivity;
import com.graficar.colegio.R;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // ============================================================
    // UI
    // ============================================================
    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewError;

    // ============================================================
    // FIREBASE
    // ============================================================
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private CredentialManager credentialManager;

    // ============================================================
    // ON CREATE
    // ============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_login);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();
        credentialManager = CredentialManager.create(this);

        // Inicializar vistas
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewError = findViewById(R.id.textViewError);

        buttonLogin.setOnClickListener(v -> attemptLogin());

        if (textViewError != null) {
            textViewError.setVisibility(View.GONE);
        }

        // Botón de Google
        View btnGoogle = findViewById(R.id.btnGoogle);
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> signInWithGoogle());
        }
    }

    // ============================================================
    // LOGIN POR EMAIL / CONTRASEÑA
    // ============================================================
    private void attemptLogin() {
        String email = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validaciones básicas
        if (email.isEmpty()) {
            showError("Ingresa tu correo.");
            return;
        }

        if (!email.contains("@")) {
            showError("Ingresa un correo válido.");
            return;
        }

        if (password.isEmpty()) {
            showError("Ingresa tu contraseña.");
            return;
        }

        if (password.length() < 6) {
            showError("La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        // Iniciar sesión con Firebase Auth
        signInWithEmail(email, password);
    }

    // ============================================================
    // FIREBASE: EMAIL / CONTRASEÑA
    // ============================================================
    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            verificarRolYRedirigir(user);
                        } else {
                            showError("Error: usuario nulo tras autenticación.");
                        }
                    } else {
                        // Si no existe la cuenta, intentar registrarla
                        // (solo si el email está pre-registrado en la base)
                        verificarSiEmailEstaRegistrado(email, password);
                    }
                });
    }

    // ============================================================
    // SI FALLA EL LOGIN → VERIFICAR SI EL EMAIL EXISTE
    // Si existe en la base pero no tiene cuenta Firebase, se crea.
    // Si ya tiene cuenta, es que la contraseña es incorrecta.
    // ============================================================
    private void verificarSiEmailEstaRegistrado(String email, String password) {
        dbRef.child("profesores")
                .orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                if (child.hasChild("authUid")) {
                                    // Ya tiene cuenta → contraseña incorrecta
                                    showError("Contraseña incorrecta. Intenta de nuevo.");
                                    return;
                                }
                                // No tiene cuenta → crearla
                                crearCuentaConEmail(email, password, "profesor");
                                return;
                            }
                        } else {
                            buscarEnUsuariosParaRegistro(email, password);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError("Error al verificar el correo: " + error.getMessage());
                    }
                });
    }

    private void buscarEnUsuariosParaRegistro(String email, String password) {
        dbRef.child("usuarios")
                .orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                if (child.hasChild("authUid")) {
                                    showError("Contraseña incorrecta. Intenta de nuevo.");
                                    return;
                                }
                                crearCuentaConEmail(email, password, "apoderado");
                                return;
                            }
                        } else {
                            showError("Este correo no está registrado. "
                                    + "Contacta al administrador del colegio.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError("Error al verificar el correo: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // CREAR CUENTA CON EMAIL / CONTRASEÑA
    // ============================================================
    private void crearCuentaConEmail(String email, String password, String tipo) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Cuenta creada: " + email + " (" + tipo + ")");
                            verificarRolYRedirigir(user);
                        }
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Error desconocido";
                        showError("No se pudo crear la cuenta: " + msg);
                    }
                });
    }

    // ============================================================
    // FIREBASE: GOOGLE SIGN-IN
    // ============================================================
    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        runOnUiThread(() -> handleGoogleCredential(result));
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "Error Google Sign-In: " + e.getMessage(), e);
                        runOnUiThread(() ->
                                showError("Error: " + e.getClass().getSimpleName()
                                        + " - " + e.getMessage()));
                    }
                }
        );
    }

    private void handleGoogleCredential(GetCredentialResponse response) {
        Credential credential = response.getCredential();

        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(customCredential.getType())) {

                GoogleIdTokenCredential googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(customCredential.getData());

                String idToken = googleIdTokenCredential.getIdToken();

                AuthCredential firebaseCredential =
                        GoogleAuthProvider.getCredential(idToken, null);

                mAuth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    verificarRolYRedirigir(user);
                                } else {
                                    showError("Error: usuario nulo tras autenticación.");
                                }
                            } else {
                                showError("Error al autenticar con Google.");
                            }
                        });
            }
        }
    }

    // ============================================================
    // VERIFICAR ROL Y REDIRIGIR
    //   1. Busca en authIndex por el authUid
    //   2. Si existe → redirige según el tipo
    //   3. Si no existe → busca por email en profesores/usuarios
    //   4. Si lo encuentra → crea authIndex y redirige
    //   5. Si no lo encuentra → error
    // ============================================================
    private void verificarRolYRedirigir(FirebaseUser user) {
        String authUid = user.getUid();
        String email = user.getEmail();

        if (email == null) {
            showError("No se pudo obtener el correo de la cuenta.");
            mAuth.signOut();
            return;
        }

        dbRef.child("authIndex").child(authUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String tipo = snapshot.child("tipo").getValue(String.class);
                            String idLocal = snapshot.child("idLocal").getValue(String.class);
                            redirigirSegunTipo(tipo, idLocal, user.getDisplayName());
                        } else {
                            vincularPorEmail(authUid, email, user.getDisplayName());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError("Error al verificar el rol: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // VINCULAR POR EMAIL (primera vez)
    // ============================================================
    private void vincularPorEmail(String authUid, String email, String displayName) {
        dbRef.child("profesores")
                .orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String idLocal = child.getKey();
                                crearVinculo(authUid, "profesor", idLocal, email,
                                        ProfeActivity.class, displayName);
                            }
                        } else {
                            vincularComoPadre(authUid, email, displayName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError("Error al buscar profesor: " + error.getMessage());
                    }
                });
    }

    private void vincularComoPadre(String authUid, String email, String displayName) {
        dbRef.child("usuarios")
                .orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String idLocal = child.getKey();
                                crearVinculo(authUid, "apoderado", idLocal, email,
                                        MainActivity.class, displayName);
                            }
                        } else {
                            showError("Tu correo (" + email + ") no está registrado. "
                                    + "Contacta al administrador del colegio.");
                            mAuth.signOut();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError("Error al buscar apoderado: " + error.getMessage());
                    }
                });
    }

    // ============================================================
    // CREAR VÍNCULO (authUid + authIndex)
    // ============================================================
    private void crearVinculo(String authUid, String tipo, String idLocal,
                              String email, Class<?> targetActivity, String displayName) {

        String nodoDestino = tipo.equals("profesor") ? "profesores" : "usuarios";

        Map<String, Object> indexEntry = new HashMap<>();
        indexEntry.put("tipo", tipo);
        indexEntry.put("idLocal", idLocal);
        indexEntry.put("email", email);

        Map<String, Object> cambios = new HashMap<>();
        cambios.put(nodoDestino + "/" + idLocal + "/authUid", authUid);
        cambios.put("authIndex/" + authUid, indexEntry);

        dbRef.updateChildren(cambios)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Vínculo creado: " + tipo + " - " + idLocal);
                    redirigirSegunTipo(tipo, idLocal, displayName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al crear vínculo: " + e.getMessage());
                    showError("Error al vincular la cuenta: " + e.getMessage());
                });
    }

    // ============================================================
    // REDIRIGIR SEGÚN EL TIPO
    // ============================================================
    private void redirigirSegunTipo(String tipo, String idLocal, String displayName) {
        String nombre = (displayName != null && !displayName.isEmpty())
                ? displayName
                : "usuario";

        if ("profesor".equals(tipo)) {
            Intent intent = new Intent(LoginActivity.this, ProfeActivity.class);
            intent.putExtra("idLocal", idLocal);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Toast.makeText(this, "Bienvenido profesor, " + nombre, Toast.LENGTH_SHORT).show();
            startActivity(intent);
            finish();
        } else if ("apoderado".equals(tipo)) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("idLocal", idLocal);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Toast.makeText(this, "Bienvenido, " + nombre, Toast.LENGTH_SHORT).show();
            startActivity(intent);
            finish();
        } else {
            showError("Tipo de usuario no reconocido: " + tipo);
            mAuth.signOut();
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private void showError(String errorMessage) {
        if (textViewError != null) {
            textViewError.setText(errorMessage);
            textViewError.setVisibility(View.VISIBLE);
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
}