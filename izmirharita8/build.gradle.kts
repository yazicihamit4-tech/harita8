// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

// Windows'ta (Unable to delete directory) klasör silme/kilitlenme hatasını azaltmak için
// zorunlu (force) bir Clean Task tanımı eklendi.
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
    // Eğer Windows'ta dosyalar asılı kalıyorsa, bu silme işlemi hataları yoksayacaktır.
    isFollowSymlinks = false
}
