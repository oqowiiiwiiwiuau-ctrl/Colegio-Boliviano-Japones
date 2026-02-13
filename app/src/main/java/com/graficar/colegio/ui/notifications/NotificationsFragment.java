package com.graficar.colegio.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.graficar.colegio.databinding.FragmentNotificationsBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Título
        binding.textNotificationsTitle.setText("Notificaciones recientes");

        // Notificación real
        String titulo = "⚠ Falta a clase";
        String mensaje = "Miguelito no se presentó a clases de Educación Física.";

        String hora = new SimpleDateFormat("dd/MM/yyyy - HH:mm a", Locale.getDefault())
                .format(new Date());

        binding.textNotificationTitle.setText(titulo);
        binding.textNotificationMessage.setText(mensaje);
        binding.textNotificationTime.setText(hora);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
