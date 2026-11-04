# Configurar y probar la API

Para preparar PHP, Composer, MySQL/Laragon, Android Studio y ejecutar ambos módulos, sigue la guía central [`../../docs/GUIA_EJECUCION_WINDOWS.md`](../../docs/GUIA_EJECUCION_WINDOWS.md).

## Arrancar la API

Desde `cellFreeAppBack-main`:

```powershell
php artisan serve --host=127.0.0.1 --port=8000
```

La base local es `http://127.0.0.1:8000/api`. Swagger UI está en `http://127.0.0.1:8000/api/documentation`.

## Autenticación

- `POST /api/auth/register` crea una cuenta ESTUDIANTE.
- `POST /api/auth/login` devuelve un token Bearer de Laravel Sanctum.
- En las siguientes peticiones protegidas envía `Authorization: Bearer TOKEN`.
- `GET /api/auth/user` consulta la cuenta autenticada.
- `POST /api/auth/logout` invalida el token actual.

El panel administrativo web usa una sesión y es independiente de los tokens de Android. Para crear la cuenta inicial SUPERADMIN ejecuta `php artisan cellfree:superadmin` después de configurar la base y aplicar migraciones.

## Prueba con PowerShell

```powershell
$body = @{ email = 'estudiante@example.com'; password = 'contraseña-de-prueba' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8000/api/auth/login' -ContentType 'application/json' -Body $body
$headers = @{ Authorization = "Bearer $($login.data.access_token)" }
Invoke-RestMethod -Method Get -Uri 'http://127.0.0.1:8000/api/auth/user' -Headers $headers
```

Usa una cuenta de prueba local. No pegues tokens o contraseñas en documentos compartidos ni en repositorios.
