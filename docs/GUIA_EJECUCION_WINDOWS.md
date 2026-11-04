# Guía para ejecutar CellFree App en Windows

Esta guía prepara una copia local nueva del proyecto con Laragon, Laravel y Android Studio. Se recomienda usar una base de datos MySQL vacía para esta instalación.

## 1. Requisitos

- Windows 10 u 11.
- [Laragon](https://laragon.org/download/) con PHP 8.3, Composer y MySQL/MariaDB. El backend se ha trabajado con PHP 8.3.
- Android Studio, Android SDK Platform 34, Android SDK Build-Tools 34.0.0 y JDK 17 (Gradle JDK).
- Conexión a internet para descargar dependencias de Composer y Gradle.

Inicia MySQL desde el panel de Laragon. Apache no es necesario para desarrollo local si usas `php artisan serve`.

## 2. Preparar MySQL

1. Abre Laragon y selecciona **Start All** (o inicia MySQL).
2. Abre HeidiSQL desde Laragon o usa tu cliente MySQL y crea una base de datos vacía llamada `cellfreeapp_db`, con cotejamiento `utf8mb4_unicode_ci`.
3. Anota el host, puerto, usuario y contraseña que tenga tu instalación local. En Laragon suelen ser `127.0.0.1`, `3306` y usuario `root`; confirma los valores en tu equipo.

No importes `CellFreeApp_Base/CellFreeApp.sql` antes de ejecutar migraciones sobre esta base nueva. Ese archivo es un respaldo aparte. Si tu objetivo es recuperar esa base, restáuralo en una base separada y no ejecutes migraciones sin revisar primero qué versión del esquema contiene.

## 3. Configurar e iniciar Laravel

Abre una terminal PowerShell en la carpeta `PROYECTO CELLFREE APP` y ejecuta:

```powershell
Set-Location .\cellFreeAppBack-main
Copy-Item .env.example .env
composer install
```

Edita `cellFreeAppBack-main/.env` con un editor de texto y configura estos valores con los datos de Laragon:

```dotenv
APP_NAME="CellFree App"
APP_ENV=local
APP_DEBUG=true
APP_URL=http://127.0.0.1:8000

DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=cellfreeapp_db
DB_USERNAME=root
DB_PASSWORD=
```

Deja `DB_PASSWORD` vacío solo si tu instalación local no tiene contraseña. No copies credenciales de otra persona ni compartas el archivo `.env`.

Después, en la misma terminal:

```powershell
php artisan key:generate
php artisan config:clear
php artisan migrate:status
```

`migrate:status` muestra el estado sin cambiar el esquema. En la base vacía, aplica las migraciones del proyecto:

```powershell
php artisan migrate
```

No uses `migrate:fresh`, `migrate:refresh` ni `db:wipe`: esos comandos pueden borrar datos. Si ves un error de conexión, revisa que MySQL esté iniciado y que el nombre de la base y las credenciales del `.env` sean correctos.

### Crear el superadministrador

Una vez aplicadas las migraciones, crea la cuenta inicial desde esa misma carpeta:

```powershell
php artisan cellfree:superadmin
```

El comando pide nombre, correo y contraseña (mínimo 12 caracteres). La contraseña se almacena con hash. Consérvala en un gestor seguro; el comando no la vuelve a mostrar ni crea un segundo SUPERADMIN si ya existe uno.

Para generar el archivo que usa Swagger UI, ejecuta una vez:

```powershell
php artisan l5-swagger:generate
```

Al cambiar las anotaciones de la API, vuelve a generar esa especificación.

### Iniciar el servidor y abrir el panel

En una terminal de PowerShell ubicada en `cellFreeAppBack-main`:

```powershell
php artisan serve --host=127.0.0.1 --port=8000
```

Mantén esa terminal abierta. Abre:

- Panel: <http://127.0.0.1:8000/admin/login>
- API: <http://127.0.0.1:8000/api>
- Swagger: <http://127.0.0.1:8000/api/documentation>

Inicia sesión en el panel con el SUPERADMIN creado. Para detener Laravel, vuelve a la terminal y pulsa `Ctrl+C`.

## 4. Abrir y ejecutar Android

1. Abre Android Studio y elige **Open**.
2. Selecciona la carpeta `cellFreeAppAndroid-main` (la que contiene `settings.gradle.kts`).
3. Acepta la sincronización de Gradle. En **Settings > Build Tools > Gradle**, selecciona JDK 17 como Gradle JDK. Desde SDK Manager instala Android SDK Platform 34 y Build-Tools 34.0.0 si Android Studio los solicita.
4. Copia la plantilla de configuración y edítala desde PowerShell en la carpeta del proyecto:

   ```powershell
   Copy-Item .\local.properties.example .\local.properties
   ```

   Para un emulador Android, deja:

   ```properties
   API_BASE_URL=http://10.0.2.2:8000/api/
   ```

   `10.0.2.2` permite al emulador llegar al `localhost` de Windows. No uses `localhost` dentro del emulador: apuntaría al propio Android. Android Studio añade o administra `sdk.dir` en `local.properties`; no lo copies de otro equipo.

5. Inicia un emulador desde **Device Manager** o conecta un dispositivo y pulsa **Run** (▶) para ejecutar la configuración `app`.

El `gradle.properties` del módulo permite la compilación cuando la ruta actual contiene `7°`, porque Android Gradle Plugin detiene por defecto las rutas Windows con caracteres no ASCII. Si clonas el repositorio en una ruta ASCII, esa opción puede permanecer activa.

El backend debe seguir ejecutándose. En el primer inicio en Android, concede manualmente los permisos que solicite la app, como acceso a uso de aplicaciones, mostrar sobre otras apps y notificaciones, según las funciones que vayas a probar.

Para probar un perfil móvil, crea una cuenta desde **Registrarse** en la aplicación; el registro de API crea cuentas con rol ESTUDIANTE. El SUPERADMIN se crea por terminal con el comando anterior y se utiliza para iniciar sesión en el panel web.

### Usar un teléfono físico

El teléfono y el computador deben estar en la misma red Wi-Fi. Obtén la IPv4 del computador con `ipconfig`; inicia Laravel escuchando en la red local:

```powershell
php artisan serve --host=0.0.0.0 --port=8000
```

En `local.properties`, cambia la URL por `http://IP_DEL_COMPUTADOR:8000/api/`. Permite PHP en el firewall de Windows para la red privada si se solicita. No expongas este servidor de desarrollo a internet; HTTP sin cifrar es solo para pruebas locales.

## 5. Pruebas del backend

Desde `cellFreeAppBack-main`:

```powershell
php artisan test
```

La configuración de PHPUnit usa SQLite en memoria, por lo que las pruebas no necesitan la base MySQL de Laragon y no cambian sus datos.

## 6. Solución de problemas

| Síntoma | Revisión |
|---|---|
| `could not find driver` | Activa `pdo_mysql` en la versión de PHP que usa Laragon y reinicia la terminal. Comprueba `php --ini` y `php -m`. |
| Error de conexión MySQL | Inicia MySQL en Laragon y verifica host, puerto, base, usuario y contraseña en `.env`. Luego ejecuta `php artisan config:clear`. |
| Laravel indica que falta `APP_KEY` | En la raíz del backend ejecuta `php artisan key:generate`. No regeneres la clave en una instalación ya usada sin entender el efecto sobre sesiones/cookies cifradas. |
| `composer` no se reconoce | Usa la terminal de Laragon o instala Composer y verifica que `composer --version` funcione. |
| Gradle no sincroniza | Selecciona JDK 17, instala Android SDK 34 y vuelve a sincronizar el proyecto. |
| Android no se conecta | Confirma que Laravel esté ejecutándose, revisa la URL del `local.properties` y usa `10.0.2.2` para emulador o la IPv4 del computador para un teléfono. |
| Puerto 8000 ocupado | Inicia con otro puerto, por ejemplo `php artisan serve --port=8010`, y actualiza `API_BASE_URL` con el mismo puerto. |

## 7. Qué compartir al entregar el proyecto

Comparte el directorio del proyecto completo, incluyendo ambos proyectos, migraciones, documentación y wrappers de Gradle. No compartas `cellFreeAppBack-main/.env`, `cellFreeAppAndroid-main/local.properties`, `vendor/`, `node_modules/`, `.gradle/`, `build/` ni credenciales. La otra persona ejecuta `composer install` y Android Studio descarga Gradle/dependencias cuando sincroniza.
