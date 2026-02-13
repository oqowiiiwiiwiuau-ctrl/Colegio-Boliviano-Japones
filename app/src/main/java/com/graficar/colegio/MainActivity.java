package com.graficar.colegio;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.graficar.colegio.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);

        // El AppBarConfiguration se usaba para configurar la ActionBar, pero lo mantenemos
        // por si lo necesitas para la navegación de nivel superior:
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        // ⭐ ESTA LÍNEA CAUSA EL CRASH Y DEBE SER ELIMINADA:
        // NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Esta línea conecta correctamente los botones inferiores (BottomNavigationView) con la navegación:
        NavigationUI.setupWithNavController(binding.navView, navController);
    }
}