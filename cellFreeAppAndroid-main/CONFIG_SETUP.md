# Configuración local de CellFree Android

Android lee los valores del backend desde `local.properties` en la raíz de este módulo.

## Crear el archivo local

Desde la carpeta `cellFreeAppAndroid-main`:

```powershell
Copy-Item .\local.properties.example .\local.properties
```

Android Studio puede agregar `sdk.dir` a ese mismo archivo para indicar dónde está instalado el SDK de Android. Esa ruta depende de cada computador; no copies el `local.properties` de otro equipo ni agregues `sdk.dir` a la plantilla de ejemplo.

## URL del backend

```properties
API_BASE_URL=http://10.0.2.2:8000/api/
API_TIMEOUT_SECONDS=30
API_ENABLE_LOGGING=true
APP_DEBUG_MODE=true
```

- **Emulador Android:** `10.0.2.2` redirige al `localhost` de Windows.
- **Teléfono físico:** usa `http://IP_DEL_COMPUTADOR:8000/api/`, en la misma red Wi-Fi. Inicia Laravel con `php artisan serve --host=0.0.0.0 --port=8000` y permite PHP en el firewall para la red privada.
- **Otro puerto:** actualiza el número de puerto en la URL para que coincida con `php artisan serve`.

Inicia primero el backend; revisa [`../docs/GUIA_EJECUCION_WINDOWS.md`](../docs/GUIA_EJECUCION_WINDOWS.md). HTTP sin TLS es solo para desarrollo local; no uses estas direcciones para una publicación pública.

`API_TIMEOUT_SECONDS` controla el tiempo de espera HTTP. Los flags `API_ENABLE_LOGGING` y `APP_DEBUG_MODE` habilitan diagnóstico local; los builds release desactivan el logging.
