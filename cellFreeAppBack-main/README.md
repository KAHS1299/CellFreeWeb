# CellFree App Backend

Backend Laravel 8 que contiene el panel administrativo Blade y la API móvil protegida con Laravel Sanctum. La tabla de cuentas de la aplicación se llama `usuarios`.

## Requisitos

- PHP 8.1 o superior (desarrollo del proyecto verificado con PHP 8.3), con `pdo_mysql`, `openssl`, `mbstring`, `fileinfo` y `curl`.
- Composer 2.
- MySQL/MariaDB para desarrollo integrado con Laragon.

## Inicio local en Windows

La guía principal para instalar todo CellFree está en [`../docs/GUIA_EJECUCION_WINDOWS.md`](../docs/GUIA_EJECUCION_WINDOWS.md). Resumen de comandos desde esta carpeta:

```powershell
Copy-Item .env.example .env
composer install
# Configura APP_URL y DB_* en .env antes de continuar.
php artisan key:generate
php artisan migrate:status
php artisan migrate
php artisan cellfree:superadmin
php artisan l5-swagger:generate
php artisan serve --host=127.0.0.1 --port=8000
```

`migrate:status` solo consulta el estado del esquema. `migrate` aplica migraciones pendientes; úsalo en una base vacía o después de confirmar que la base corresponde a este proyecto. No uses `migrate:fresh`, `migrate:refresh` ni `db:wipe` para preparar una instalación que contenga datos.

`key:generate` es para una instalación nueva con `APP_KEY` vacío. No lo vuelvas a ejecutar sobre una instalación ya usada, porque reemplaza la clave con la que se cifran las sesiones y otros valores.

El comando `cellfree:superadmin` solicita los datos de la cuenta por terminal. No guardes la contraseña en esta documentación.

## Rutas locales

- Panel: <http://127.0.0.1:8000/admin/login>
- API: <http://127.0.0.1:8000/api>
- Swagger UI: <http://127.0.0.1:8000/api/documentation>

## Carpetas principales

```text
app/Http/Controllers/       Controladores del panel y API
app/Http/Middleware/        Autenticación y permisos por rol
app/Models/                 Modelos Eloquent
routes/web.php              Rutas del panel con sesión
routes/api.php              Endpoints Sanctum para Android
resources/views/admin/      Vistas Blade
public/css/                 Estilos del panel
database/migrations/        Esquema versionado
tests/                      Pruebas PHPUnit/Feature
docs/                       Documentación funcional y técnica
```

## Verificación

PHPUnit usa SQLite en memoria; no es necesario tener MySQL iniciado para ejecutar las pruebas:

```powershell
php artisan test
```

## Configuración local y seguridad

`.env` es local y no debe compartirse. Android usa Sanctum Bearer tokens en la API; el panel administrativo usa sesión Laravel y protección CSRF. No se instala ni publica otra migración de Sanctum: las migraciones necesarias ya forman parte del proyecto.
