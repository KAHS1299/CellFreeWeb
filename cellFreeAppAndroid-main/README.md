# CellFree App Android

Aplicación Android nativa escrita en Kotlin, con vistas XML y llamadas HTTP mediante Retrofit. Este módulo se abre y compila de forma independiente del backend.

## Requisitos

- Android Studio.
- JDK 17 seleccionado como **Gradle JDK**.
- Android SDK Platform 34; `minSdk` del proyecto: 23.
- Backend CellFree ejecutándose para las funciones conectadas a la API.

## Ejecutar

1. Abre esta carpeta en Android Studio (debe contener `settings.gradle.kts`).
2. Sincroniza Gradle e instala Android SDK 34 si el IDE lo solicita.
3. Copia `local.properties.example` a `local.properties`. La plantilla ya configura la URL del backend para el emulador:

   ```powershell
   Copy-Item .\local.properties.example .\local.properties
   ```

4. Inicia el backend Laravel desde `cellFreeAppBack-main`; consulta [`../docs/GUIA_EJECUCION_WINDOWS.md`](../docs/GUIA_EJECUCION_WINDOWS.md).
5. Inicia un emulador o conecta un dispositivo y pulsa **Run** en Android Studio.

`10.0.2.2` apunta desde el emulador al `localhost` de Windows. Para un teléfono físico, cambia `API_BASE_URL` en `local.properties` a la IPv4 del computador, manteniendo ambos en la misma red. No compartas ese archivo: puede contener la ruta local del SDK que configura Android Studio.

## Estructura

```text
app/src/main/java/     Código Kotlin: actividades, API, modelos y servicios
app/src/main/res/      Layouts XML, recursos, temas e iconos
app/src/test/          Pruebas unitarias JVM
app/src/androidTest/   Pruebas en dispositivo/emulador
gradle/                Catálogo de versiones y wrapper
```

La guía de variables y redes está en [`CONFIG_SETUP.md`](CONFIG_SETUP.md). Para compilar desde una terminal con el SDK y JDK configurados, ejecuta `.\gradlew.bat assembleDebug` en esta carpeta.
