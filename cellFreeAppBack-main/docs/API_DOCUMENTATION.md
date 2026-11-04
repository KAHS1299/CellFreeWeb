# API de Autenticación y Gestión - Procrastinación Universitaria

Esta documentación describe todos los endpoints disponibles en la API para conectar con aplicaciones móviles o frontend.

## Base URL
```
http://localhost:8000/api
```

## Headers Requeridos
```
Content-Type: application/json
Accept: application/json
```

## Soporte UTF-8
La API tiene **soporte completo para UTF-8**, incluyendo:
- Caracteres especiales (ñ, ç, ü, etc.)
- Acentos y tildes (á, é, í, ó, ú)
- Emojis y caracteres Unicode

### Headers recomendados para UTF-8:
```
Content-Type: application/json; charset=utf-8
```

## Autenticación
Para las rutas protegidas, incluir el header:
```
Authorization: Bearer {token}
```

---

## Endpoints de Autenticación

### 1. Registro de Usuario
**POST** `/auth/register`

Registra un nuevo usuario en el sistema.

#### Parámetros del cuerpo:
```json
{
    "name": "string (requerido, máx: 255)",
    "email": "string (requerido, email válido, único)",
    "password": "string (requerido, mín: 8 caracteres)",
    "password_confirmation": "string (requerido, debe coincidir con password)"
}
```

#### Ejemplo de solicitud:
```json
{
    "name": "Juan Pérez",
    "email": "juan@example.com",
    "password": "password123",
    "password_confirmation": "password123"
}
```

#### Respuesta exitosa (201):
```json
{
    "status": "success",
    "message": "Usuario registrado exitosamente",
    "data": {
        "user": {
            "id": 1,
            "name": "Juan Pérez",
            "email": "juan@example.com",
            "email_verified_at": null,
            "created_at": "2025-11-05T10:00:00.000000Z",
            "updated_at": "2025-11-05T10:00:00.000000Z"
        },
        "access_token": "1|xxxxxxxxxxxxxxxxxxxx",
        "token_type": "Bearer"
    }
}
```

### 2. Inicio de Sesión
**POST** `/auth/login`

Autentica un usuario y devuelve un token de acceso.

#### Parámetros del cuerpo:
```json
{
    "email": "string (requerido, email válido)",
    "password": "string (requerido)"
}
```

#### Respuesta exitosa (200):
```json
{
    "status": "success",
    "message": "Inicio de sesión exitoso",
    "data": {
        "user": {
            "id": 1,
            "name": "Juan Pérez",
            "email": "juan@example.com",
            "email_verified_at": null,
            "created_at": "2025-11-05T10:00:00.000000Z",
            "updated_at": "2025-11-05T10:00:00.000000Z"
        },
        "access_token": "2|xxxxxxxxxxxxxxxxxxxx",
        "token_type": "Bearer"
    }
}
```

### 3. Obtener Información del Usuario
**GET** `/auth/user`

Obtiene la información del usuario autenticado.

### 4. Cerrar Sesión
**POST** `/auth/logout`

Cierra la sesión actual del usuario.

### 5. Actualizar Perfil
**PUT** `/auth/update-profile`

Actualiza la información del perfil del usuario.

### 6. Eliminar Cuenta
**DELETE** `/auth/delete-account`

Elimina permanentemente la cuenta del usuario.

---

## Endpoints de Aplicaciones Bloqueadas

### 1. Listar Aplicaciones Bloqueadas
**GET** `/app-blocks`

Obtiene todas las aplicaciones bloqueadas del usuario.

#### Parámetros de consulta opcionales:
- `categoria` - Filtrar por categoría
- `estado` - Filtrar por estado (true/false)
- `activo_ahora` - Filtrar solo aplicaciones activas en el período actual (true/false)

#### Respuesta exitosa (200):
```json
{
    "status": "success",
    "data": {
        "aplicaciones_bloqueadas": [
            {
                "id": 1,
                "nombre_app": "Instagram",
                "categoria": "social", 
                "tiempo_bloqueo": 60,
                "estado": true,
                "fecha_inicio": "2025-11-10 09:00:00",
                "fecha_fin": "2025-11-10 18:00:00",
                "activo_ahora": true,
                "tiempo_restante": 420,
                "created_at": "2025-11-05T10:00:00.000000Z",
                "updated_at": "2025-11-05T10:00:00.000000Z"
            }
        ]
    }
}
```

### 2. Crear Aplicación Bloqueada
**POST** `/app-blocks`

Agrega una nueva aplicación a la lista de bloqueo.

#### Parámetros del cuerpo:
```json
{
    "nombre_app": "string (requerido, máx: 100)",
    "categoria": "string (opcional, valores: social,juegos,entretenimiento,productividad,otros)",
    "tiempo_bloqueo": "integer (opcional, minutos 1-1440)",
    "estado": "boolean (opcional, default: true)",
    "fecha_inicio": "datetime (opcional, formato: YYYY-MM-DD HH:MM:SS)",
    "fecha_fin": "datetime (opcional, formato: YYYY-MM-DD HH:MM:SS, debe ser posterior a fecha_inicio)"
}
```

#### Ejemplo de solicitud:
```json
{
    "nombre_app": "Instagram",
    "categoria": "social",
    "tiempo_bloqueo": 60,
    "estado": true,
    "fecha_inicio": "2025-11-10 09:00:00",
    "fecha_fin": "2025-11-10 18:00:00"
}
```

#### Ejemplo para bloqueo temporal de estudios:
```json
{
    "nombre_app": "TikTok",
    "categoria": "social", 
    "fecha_inicio": "2025-11-10 14:00:00",
    "fecha_fin": "2025-11-10 17:00:00"
}
```

#### Respuesta exitosa (201):
```json
{
    "status": "success",
    "message": "Aplicación bloqueada agregada exitosamente",
    "data": {
        "aplicacion_bloqueada": {
            "id": 1,
            "nombre_app": "Instagram",
            "categoria": "social",
            "tiempo_bloqueo": 60,
            "estado": true,
            "fecha_inicio": "2025-11-10 09:00:00",
            "fecha_fin": "2025-11-10 18:00:00", 
            "activo_ahora": true,
            "tiempo_restante": 420,
            "created_at": "2025-11-05T10:00:00.000000Z",
            "updated_at": "2025-11-05T10:00:00.000000Z"
        }
    }
}
```

### 3. Obtener Aplicación Específica
**GET** `/app-blocks/{id}`

Obtiene los detalles de una aplicación bloqueada específica.

### 4. Actualizar Aplicación Bloqueada
**PUT** `/app-blocks/{id}`

Actualiza una aplicación bloqueada existente.

### 5. Eliminar Aplicación Bloqueada
**DELETE** `/app-blocks/{id}`

Elimina una aplicación de la lista de bloqueo.

### 6. Activar/Desactivar Aplicación
**PATCH** `/app-blocks/{id}/toggle-status`

Cambia el estado activo/inactivo de una aplicación bloqueada.

### 7. Obtener Aplicaciones Activas
**GET** `/app-blocks/active/list`

Obtiene solo las aplicaciones que están actualmente activas para bloqueo.

### 8. Estadísticas de Aplicaciones
**GET** `/app-blocks/stats/summary`

Obtiene estadísticas sobre las aplicaciones bloqueadas.

#### Respuesta exitosa (200):
```json
{
    "status": "success",
    "data": {
        "estadisticas": {
            "total_aplicaciones": 10,
            "aplicaciones_activas": 8,
            "aplicaciones_inactivas": 2,
            "categorias": {
                "social": 4,
                "juegos": 3,
                "entretenimiento": 2,
                "otros": 1
            }
        }
    }
}
```

---

## Endpoints de Recordatorios/Calendario

### 1. Listar Recordatorios
**GET** `/reminders`

Obtiene todos los recordatorios del usuario.

#### Parámetros de consulta opcionales:
- `tipo` - Filtrar por tipo (tarea, examen, estudio)
- `completado` - Filtrar por estado (true/false)
- `fecha_desde` - Fecha de inicio para filtrar
- `fecha_hasta` - Fecha de fin para filtrar

#### Respuesta exitosa (200):
```json
{
    "status": "success",
    "data": {
        "recordatorios": [
            {
                "id": 1,
                "titulo": "Examen de Matemáticas",
                "descripcion": "Repasar capítulos 1-5",
                "fecha_hora": "2025-11-10 09:00:00",
                "tipo": "examen",
                "completado": false,
                "created_at": "2025-11-05T10:00:00.000000Z",
                "updated_at": "2025-11-05T10:00:00.000000Z"
            }
        ]
    }
}
```

### 2. Crear Recordatorio
**POST** `/reminders`

Crea un nuevo recordatorio. **Soporte completo para UTF-8** (caracteres especiales, tildes, etc.).

#### Parámetros del cuerpo:
```json
{
    "titulo": "string (requerido, no vacío)",
    "descripcion": "string (opcional)",
    "fecha_hora": "datetime (requerido, formato: YYYY-MM-DD HH:MM:SS)",
    "tipo": "string (requerido, valores: tarea, examen, estudio)",
    "completado": "boolean (opcional, default: false)"
}
```

#### Ejemplo de solicitud:
```json
{
    "titulo": "Examen de Matemáticas",
    "descripcion": "Repasar cálculo diferencial y ecuaciones",
    "fecha_hora": "2025-11-10 09:00:00",
    "tipo": "examen"
}
```

#### Ejemplos con UTF-8:
```json
{
    "titulo": "Presentación en Inglés 🎤",
    "descripcion": "Practicar pronunciación y fluidez",
    "fecha_hora": "2025-11-12 14:00:00",
    "tipo": "tarea"
}
```

```json
{
    "titulo": "Estudiar Filosofía",
    "descripcion": "Leer 'Crítica de la Razón Pura' - Capítulo 3",
    "fecha_hora": "2025-11-15 16:30:00",
    "tipo": "estudio"
}
```

#### Headers recomendados para UTF-8:
```
Content-Type: application/json; charset=utf-8
Authorization: Bearer {token}
```

#### Respuesta exitosa (201):
```json
{
    "status": "success",
    "message": "Recordatorio creado exitosamente",
    "data": {
        "recordatorio": {
            "id": 1,
            "titulo": "Examen de Matemáticas",
            "descripcion": "Repasar capítulos 1-5",
            "fecha_hora": "2025-11-10 09:00:00",
            "tipo": "examen",
            "completado": false,
            "created_at": "2025-11-05T10:00:00.000000Z",
            "updated_at": "2025-11-05T10:00:00.000000Z"
        }
    }
}
```

#### Respuestas de error:

**Error 422 - Validación fallida:**
```json
{
    "status": "error",
    "message": "Faltan campos requeridos: titulo, fecha_hora, tipo"
}
```

**Error 422 - Tipo inválido:**
```json
{
    "status": "error",
    "message": "Tipo debe ser: tarea, examen o estudio"
}
```

**Error 500 - Error del servidor:**
```json
{
    "status": "error",
    "message": "Error en el servidor",
    "error": "Detalles del error (solo en modo debug)"
}
```

### 3. Obtener Recordatorio Específico
**GET** `/reminders/{id}`

Obtiene los detalles de un recordatorio específico.

### 4. Actualizar Recordatorio
**PUT** `/reminders/{id}`

Actualiza un recordatorio existente.

### 5. Eliminar Recordatorio
**DELETE** `/reminders/{id}`

Elimina un recordatorio.

### 6. Marcar Como Completado/Pendiente
**PATCH** `/reminders/{id}/toggle-complete`

Cambia el estado de completado del recordatorio.

### 7. Recordatorios de Hoy
**GET** `/reminders/today/list`

Obtiene todos los recordatorios programados para hoy.

### 8. Próximos Recordatorios
**GET** `/reminders/upcoming/list`

Obtiene los próximos recordatorios (por defecto próximos 7 días).

#### Parámetros de consulta opcionales:
- `days` - Número de días hacia adelante (default: 7)

### 9. Recordatorios Vencidos
**GET** `/reminders/overdue/list`

Obtiene todos los recordatorios vencidos y no completados.

### 10. Vista de Calendario por Mes
**GET** `/reminders/calendar/month`

Obtiene recordatorios agrupados por fecha para vista de calendario.

#### Parámetros de consulta requeridos:
- `year` - Año (2020-2030)
- `month` - Mes (1-12)

#### Ejemplo:
```
GET /reminders/calendar/month?year=2025&month=11
```

#### Respuesta exitosa (200):
```json
{
    "status": "success",
    "data": {
        "year": 2025,
        "month": 11,
        "recordatorios_por_fecha": {
            "2025-11-10": [
                {
                    "id": 1,
                    "titulo": "Examen de Matemáticas",
                    "descripcion": "Repasar capítulos 1-5",
                    "fecha_hora": "2025-11-10 09:00:00",
                    "tipo": "examen",
                    "completado": false
                }
            ],
            "2025-11-15": [
                {
                    "id": 2,
                    "titulo": "Entrega de proyecto",
                    "descripcion": "Proyecto final de programación",
                    "fecha_hora": "2025-11-15 23:59:00",
                    "tipo": "tarea",
                    "completado": false
                }
            ]
        }
    }
}
```

### 11. Estadísticas de Recordatorios
**GET** `/reminders/stats/summary`

Obtiene estadísticas sobre los recordatorios.

#### Respuesta exitosa (200):
```json
{
    "status": "success",
    "data": {
        "estadisticas": {
            "total_recordatorios": 15,
            "recordatorios_completados": 8,
            "recordatorios_pendientes": 7,
            "recordatorios_vencidos": 2,
            "recordatorios_hoy": 3,
            "por_tipo": {
                "tarea": 8,
                "examen": 4,
                "estudio": 3
            }
        }
    }
}
```

---

## Códigos de Estado HTTP

- **200** - OK: Solicitud exitosa
- **201** - Created: Recurso creado exitosamente
- **401** - Unauthorized: Token inválido o credenciales incorrectas
- **404** - Not Found: Recurso no encontrado
- **409** - Conflict: Conflicto (ej. aplicación ya existe)
- **422** - Unprocessable Entity: Errores de validación
- **500** - Internal Server Error: Error del servidor

---

## Ejemplos de Uso

### Ejemplo con cURL - Crear aplicación bloqueada
```bash
curl -X POST http://localhost:8000/api/app-blocks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "nombre_app": "Instagram",
    "categoria": "social",
    "tiempo_bloqueo": 60
  }'
```

### Ejemplo con cURL - Crear recordatorio
```bash
curl -X POST http://localhost:8000/api/reminders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "titulo": "Examen de Matemáticas",
    "descripcion": "Repasar capítulos 1-5",
    "fecha_hora": "2025-11-10 09:00:00",
    "tipo": "examen"
  }'
```

### Ejemplo con cURL - Obtener recordatorios del mes
```bash
curl -X GET "http://localhost:8000/api/reminders/calendar/month?year=2025&month=11" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## Notas de Implementación

1. **Tokens**: Los tokens de acceso no tienen expiración por defecto.
2. **Seguridad**: Todos los recursos están protegidos por usuario.
3. **Validación**: Mensajes de error en español.
4. **Filtros**: Múltiples opciones de filtrado disponibles.
5. **Estadísticas**: Endpoints dedicados para análisis de datos.
6. **Control Temporal**: Las aplicaciones bloqueadas soportan fechas de inicio y fin para control temporal preciso.

### 📅 Control Temporal de Aplicaciones Bloqueadas

#### Funcionalidades:
- **Sin fechas**: La aplicación se bloquea permanentemente mientras `estado` sea `true`
- **Solo fecha_inicio**: La aplicación se bloquea desde la fecha especificada en adelante
- **Solo fecha_fin**: La aplicación se bloquea hasta la fecha especificada
- **Ambas fechas**: La aplicación se bloquea solo durante el período especificado

#### Campos adicionales en respuestas:
- `activo_ahora`: Boolean que indica si el bloqueo está activo en este momento
- `tiempo_restante`: Minutos restantes hasta que termine el bloqueo (null si no aplica)
- `fecha_inicio`: Fecha y hora de inicio del bloqueo
- `fecha_fin`: Fecha y hora de fin del bloqueo

#### Casos de uso:
- **Bloqueo durante estudios**: Configurar horarios específicos de concentración
- **Control parental**: Establecer horarios permitidos para ciertas aplicaciones
- **Desintoxicación digital**: Períodos de descanso de redes sociales
- **Productividad laboral**: Bloquear distracciones durante horario de trabajo

---

## 📅 Sincronización de Calendarios Externos

### 1. Listar Sincronizaciones
**GET** `/calendar-sync`

Obtiene todas las sincronizaciones de calendario configuradas por el usuario.

#### Respuesta exitosa (200):
```json
{
    "status": "success", 
    "data": {
        "sincronizaciones": [
            {
                "id": 1,
                "nombre": "Universidad Nacional",
                "url_calendario": "https://universidad.edu.co/calendar.ics",
                "estado": "activo",
                "intervalo_sincronizacion": 60,
                "ultima_sincronizacion": "2025-11-08T04:03:18.000000Z",
                "ultimo_error": null,
                "configuracion": {},
                "created_at": "2025-11-08T03:56:31.000000Z",
                "updated_at": "2025-11-08T04:07:21.000000Z"
            }
        ]
    }
}
```

### 2. Crear Sincronización de Calendario
**POST** `/calendar-sync`

Configura una nueva sincronización de calendario externo.

#### Parámetros del cuerpo:
```json
{
    "nombre": "string (requerido) - Nombre descriptivo",
    "url_calendario": "string (requerido) - URL del calendario iCal",
    "intervalo_sincronizacion": "integer (opcional) - Minutos entre sincronizaciones (15-1440, default: 60)",
    "configuracion": "object (opcional) - Configuraciones adicionales"
}
```

#### Ejemplo de solicitud:
```json
{
    "nombre": "Universidad de Cundinamarca",
    "url_calendario": "https://pregrado.ucundinamarca.edu.co/calendar/export_execute.php?userid=9280&authtoken=de1fa9b082c8bd13ea71462485811d9d6352f143&preset_what=all&preset_time=recentupcoming",
    "intervalo_sincronizacion": 120
}
```

### 3. Obtener Sincronización Específica
**GET** `/calendar-sync/{id}`

Obtiene los detalles de una sincronización específica.

### 4. Actualizar Sincronización
**PUT** `/calendar-sync/{id}`

Actualiza la configuración de una sincronización existente.

### 5. Eliminar Sincronización  
**DELETE** `/calendar-sync/{id}`

Elimina una sincronización. Opcionalmente puede eliminar también los recordatorios importados.

#### Parámetros de consulta opcionales:
- `eliminar_recordatorios=true` - También elimina los recordatorios importados

### 6. Ejecutar Sincronización Manual
**POST** `/calendar-sync/{id}/sync`

Ejecuta inmediatamente la sincronización de un calendario específico.

#### Respuesta exitosa (200) - Ejemplo Real:
```json
{
    "status": "success",
    "message": "Sincronización completada exitosamente", 
    "data": {
        "eventos_importados": 3,
        "total_eventos": 3,
        "sincronizacion": {
            "id": 2,
            "nombre": "Universidad Cundinamarca",
            "url_calendario": "https://pregrado.ucundinamarca.edu.co/calendar/export_execute.php?userid=9280&authtoken=de1fa9b082c8bd13ea71462485811d9d6352f143&preset_what=all&preset_time=recentupcoming",
            "estado": "activo",
            "intervalo_sincronizacion": 60,
            "ultima_sincronizacion": "2025-11-08T04:07:21.000000Z",
            "ultimo_error": null
        }
    }
}
```

#### Ejemplo con cURL:
```bash
curl -X POST "http://localhost:8000/api/calendar-sync/2/sync" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Eventos Importados como Recordatorios

Los eventos del calendario se importan automáticamente como recordatorios con:
- **UID externo**: Para evitar duplicados
- **Tipo automático**: Basado en palabras clave del título
  - "examen", "prueba", "test" → `examen`
  - "tarea", "entrega", "homework" → `tarea`
  - Otros → `estudio`
- **Campo `es_sincronizado`**: `true` para identificar recordatorios sincronizados

### Formatos de Calendario Soportados

- **iCal (.ics)**: Formato estándar soportado
- **URLs soportadas**:
  - Moodle/LMS universitarios
  - Google Calendar (URLs públicas)
  - Outlook/Office 365
  - Apple iCloud

---

## Changelog

### Versión 1.2 - 8 de Noviembre 2025  
#### 🆕 Nueva Funcionalidad: Sincronización de Calendarios
- **6 endpoints nuevos** para gestión completa de sincronizaciones
- **Importación automática** de eventos como recordatorios
- **Soporte iCal completo** con manejo robusto de diferentes formatos
- **Prevención de duplicados** usando UID externos
- **Categorización automática** de eventos por palabras clave
- **Validado con Universidad de Cundinamarca** - 3 eventos importados exitosamente

### Versión 1.1 - 6 de Noviembre 2025
#### ✅ Mejoras en Recordatorios:
- **Soporte completo UTF-8**: Mejorado el manejo de caracteres especiales, tildes y emojis
- **Validación optimizada**: Implementada validación manual para mejor compatibilidad con UTF-8
- **Mensajes de error mejorados**: Respuestas más específicas y descriptivas

#### 🔧 Cambios técnicos:
- Refactorizado el método `store()` en `CalendarController`
- Eliminada dependencia del Laravel Validator para el endpoint de creación de recordatorios
- Implementado manejo directo de datos UTF-8

---

## Configuración Adicional

Para usar esta API en producción:

1. Configurar variables de entorno apropiadas
2. Establecer `APP_DEBUG=false` en producción
3. Configurar HTTPS
4. Implementar rate limiting
5. Configurar CORS según tus necesidades