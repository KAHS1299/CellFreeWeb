# Panel administrativo e integración móvil

## Arquitectura

El panel Blade y la API móvil viven en el mismo backend Laravel y usan las mismas tablas y reglas. El panel autentica administradores con sesión web; Android mantiene Sanctum Bearer. Los datos de usuarios existentes permanecen en `usuarios`, y los bloqueos siguen viviendo en `aplicaciones_bloqueadas`, por lo que Android puede recibir bloqueos creados desde el panel a través de `GET /api/app-blocks`.

Roles almacenados: `SUPERADMIN`, `ADMINISTRADOR`, `ESTUDIANTE`. Los nuevos registros públicos conservan el rol de estudiante por defecto. El primer superadmin se crea con `php artisan cellfree:superadmin`; el comando no acepta ni instala claves predeterminadas.

## API móvil añadida

Todas las rutas nuevas requieren `Authorization: Bearer <token Sanctum>`.

### Registrar/actualizar dispositivo

`POST /api/v1/devices`

```json
{
  "identificador": "id-persistente-del-dispositivo",
  "modelo": "modelo Android",
  "sistema_operativo": "Android 14",
  "version_app": "1.0"
}
```

La identidad de usuario se toma del token. El identificador es único por usuario.

### Sincronizar inventario

`POST /api/v1/applications/sync`

```json
{
  "dispositivo_id": 1,
  "aplicaciones": [
    {"nombre_paquete": "com.ejemplo.app", "nombre": "Ejemplo", "categoria": "productividad"}
  ]
}
```

El dispositivo debe pertenecer al usuario autenticado. La sincronización actualiza por paquete y dispositivo. El cliente Android todavía debe recopilar y enviar este payload.

### Consultar licencia y funcionalidades

`GET /api/v1/license`

Devuelve si hay licencia activa y las funcionalidades almacenadas en el plan. El endpoint no representa ni verifica pagos; no existe pasarela conectada.

## Migraciones y notas operativas

La migración `2026_11_02_000001_create_cellfree_admin_modules` agrega columnas de rol, relación de responsabilidad, estado y apellidos al usuario existente y crea tablas relacionadas. No crea una segunda base de datos. Una migración anterior del proyecto intentaba volver a crear tablas Sanctum existentes; ahora omite la creación cuando ya están presentes.

Para local: `DB_CONNECTION=sqlite`, `DB_DATABASE=:memory:` solo sirve para un comando de prueba. Para desarrollo persistente, usa un archivo SQLite. Para producción, MySQL es lo probado/configurado por el backend, aunque Laravel dispone de configuración PostgreSQL.

## Alcance pendiente

- Extender Android `ApiClient`/Retrofit para registrar dispositivos, sincronizar apps y consultar licencias. Los endpoints backend ya están disponibles, pero la app Kotlin no los consume todavía.
- Completar la aplicación automática de límites de usuarios de planes y los reportes agregados de uso por día y apps más frecuentes.
- Permitir borrar definitivamente instituciones y añadir gestión de sesiones.
- Completar pruebas de extremo a extremo en Android; el APK y las dependencias del dispositivo no se modificaron en esta iteración.
