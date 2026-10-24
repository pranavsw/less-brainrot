pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Prebuilt Flutter add-to-app AARs (run `flutter build aar` in flutter_module/).
        maven {
            url = File(rootDir, "flutter_module/build/host/outputs/repo").toURI()
        }
        // Flutter engine artifacts (flutter_embedding, per-ABI binaries).
        maven {
            url = uri("https://storage.googleapis.com/download.flutter.io")
        }
    }
}

rootProject.name = "ContextAwareScrollCounter"
include(":app")
