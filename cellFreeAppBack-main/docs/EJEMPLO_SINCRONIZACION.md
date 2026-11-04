# Ejemplo Práctico: Sincronizar Calendario de Ucundinamarca

Este es un ejemplo paso a paso de cómo sincronizar tu calendario universitario de Ucundinamarca con la aplicación.

## Paso 1: Obtener la URL del calendario

1. **Inicia sesión en el campus virtual de Ucundinamarca**
2. **Ve a la sección de Calendario**
3. **Busca la opción "Exportar calendario" o "Suscribirse al calendario"**
4. **Copia la URL que se ve así**:
   ```
   https://pregrado.ucundinamarca.edu.co/calendar/export_execute.php?userid=9280&authtoken=de1fa9b082c8bd13ea71462485811d9d6352f143&preset_what=all&preset_time=recentupcoming
   ```

## Paso 2: Configurar la sincronización en la app

### Usando cURL (para testing):
```bash
curl -X POST "http://localhost:8000/api/calendar-sync" \
  -H "Authorization: Bearer TU_TOKEN_AQUI" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Calendario Ucundinamarca",
    "url_calendario": "https://pregrado.ucundinamarca.edu.co/calendar/export_execute.php?userid=9280&authtoken=de1fa9b082c8bd13ea71462485811d9d6352f143&preset_what=all&preset_time=recentupcoming",
    "intervalo_sincronizacion": 120
  }'
```

### Respuesta esperada:
```json
{
    "status": "success",
    "message": "Sincronización creada exitosamente",
    "data": {
        "sincronizacion": {
            "id": 1,
            "nombre": "Calendario Ucundinamarca",
            "url_calendario": "https://pregrado.ucundinamarca.edu.co/calendar/export_execute.php?userid=9280&authtoken=...",
            "estado": "activo",
            "intervalo_sincronizacion": 120,
            "ultima_sincronizacion": null,
            "ultimo_error": null,
            "created_at": "2025-11-08T03:47:40.000000Z",
            "updated_at": "2025-11-08T03:47:40.000000Z"
        }
    }
}
```

## Paso 3: Ejecutar la primera sincronización

```bash
curl -X POST "http://localhost:8000/api/calendar-sync/1/sync" \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

### Respuesta esperada:
```json
{
    "status": "success",
    "message": "Sincronización completada exitosamente",
    "data": {
        "eventos_importados": 15,
        "total_eventos": 18,
        "sincronizacion": {
            "id": 1,
            "nombre": "Calendario Ucundinamarca",
            "estado": "activo",
            "ultima_sincronizacion": "2025-11-08T03:50:15.000000Z",
            "ultimo_error": null
        }
    }
}
```

## Paso 4: Verificar los recordatorios importados

```bash
curl -X GET "http://localhost:8000/api/reminders" \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

### Verás recordatorios como:
```json
{
    "status": "success",
    "data": {
        "recordatorios": [
            {
                "id": 101,
                "titulo": "Examen Final - Matemáticas Discretas",
                "descripcion": "Examen final del curso de matemáticas discretas. Temas: grafos, lógica proposicional, combinatoria.",
                "fecha_hora": "2025-11-15 08:00:00",
                "tipo": "examen",
                "completado": false,
                "es_sincronizado": true,
                "created_at": "2025-11-08T03:50:15.000000Z"
            },
            {
                "id": 102,
                "titulo": "Entrega Proyecto - Ingeniería de Software",
                "descripcion": "Entrega final del proyecto de la materia de ingeniería de software.",
                "fecha_hora": "2025-11-20 23:59:00",
                "tipo": "tarea",
                "completado": false,
                "es_sincronizado": true,
                "created_at": "2025-11-08T03:50:15.000000Z"
            }
        ]
    }
}
```

## Paso 5: Configurar sincronización automática

### En el servidor (cron job):
```bash
# Editar crontab
crontab -e

# Agregar línea para sincronizar cada 2 horas
0 */2 * * * cd /path/to/tu/proyecto && php artisan calendar:sync
```

### Ejecutar manualmente:
```bash
cd /path/to/tu/proyecto
php artisan calendar:sync
```

## Tipos de eventos que se importarán automáticamente:

### Como "examen":
- "Examen Final - Cálculo"
- "Prueba Parcial - Física"
- "Test de Inglés"

### Como "tarea": 
- "Entrega Proyecto - Programación"
- "Homework - English Course"
- "Entrega de Ensayo"

### Como "estudio":
- "Clase Magistral - Historia"
- "Laboratorio de Química"
- "Seminario de Investigación"

## Monitoreo y mantenimiento

### Verificar estado de sincronizaciones:
```bash
curl -X GET "http://localhost:8000/api/calendar-sync" \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

### Si hay errores:
```json
{
    "id": 1,
    "nombre": "Calendario Ucundinamarca",
    "estado": "error",
    "ultimo_error": "Error al descargar calendario: HTTP 404",
    "ultima_sincronizacion": "2025-11-07T15:30:00.000000Z"
}
```

### Soluciones comunes:
1. **Error 404**: URL del calendario cambió, verifica en el campus virtual
2. **Token expirado**: Regenera el token en el campus virtual
3. **Error de red**: Problema temporal, se resolverá en la próxima sincronización

## Integración en aplicación móvil

### Al abrir la app:
```javascript
// Verificar si hay sincronizaciones pendientes
fetch('/api/calendar-sync', {
    headers: { 'Authorization': `Bearer ${token}` }
})
.then(response => response.json())
.then(data => {
    // Mostrar estado de sincronizaciones
    data.sincronizaciones.forEach(sync => {
        if (sync.estado === 'error') {
            showNotification(`Error en ${sync.nombre}: ${sync.ultimo_error}`);
        }
    });
});
```

### Botón de sincronización manual:
```javascript
async function syncCalendar(syncId) {
    const response = await fetch(`/api/calendar-sync/${syncId}/sync`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
    });
    
    const result = await response.json();
    
    if (result.status === 'success') {
        showNotification(`${result.data.eventos_importados} eventos importados`);
        refreshReminders();
    } else {
        showError(`Error: ${result.error}`);
    }
}
```

¡Con esto ya tendrás tu calendario universitario sincronizado automáticamente! 🎓📅