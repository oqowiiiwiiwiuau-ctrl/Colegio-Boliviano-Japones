// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // Elimina esta línea si no la necesitas:
    // alias(libs.plugins.android.library) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}