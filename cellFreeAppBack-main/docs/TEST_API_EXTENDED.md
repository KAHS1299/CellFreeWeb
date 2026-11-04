# Pruebas Rápidas de API - Aplicaciones Bloqueadas y Recordatorios

Este archivo contiene comandos cURL para probar rápidamente todos los endpoints de aplicaciones bloqueadas y recordatorios.

## Variables
Primero, ejecuta un login para obtener el token:

```bash
# Login para obtener token
TOKEN=$(curl -s -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  | jq -r '.data.access_token')

echo "Token: $TOKEN"
```

---

## Pruebas de Aplicaciones Bloqueadas

### 1. Crear aplicación bloqueada
```bash
curl -X POST http://localhost:8000/api/app-blocks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nombre_app": "Instagram",
    "categoria": "social",
    "tiempo_bloqueo": 60,
    "estado": true
  }'
```

### 2. Crear otra aplicación
```bash
curl -X POST http://localhost:8000/api/app-blocks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nombre_app": "TikTok",
    "categoria": "social",
    "tiempo_bloqueo": 30,
    "estado": true
  }'
```

### 3. Listar todas las aplicaciones bloqueadas
```bash
curl -X GET http://localhost:8000/api/app-blocks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Filtrar por categoría
```bash
curl -X GET "http://localhost:8000/api/app-blocks?categoria=social" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Obtener aplicaciones activas
```bash
curl -X GET http://localhost:8000/api/app-blocks/active/list \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 6. Actualizar aplicación (cambiar ID por uno real)
```bash
curl -X PUT http://localhost:8000/api/app-blocks/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nombre_app": "Instagram",
    "categoria": "social",
    "tiempo_bloqueo": 90
  }'
```

### 7. Cambiar estado de aplicación
```bash
curl -X PATCH http://localhost:8000/api/app-blocks/1/toggle-status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 8. Obtener estadísticas
```bash
curl -X GET http://localhost:8000/api/app-blocks/stats/summary \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 9. Eliminar aplicación
```bash
curl -X DELETE http://localhost:8000/api/app-blocks/2 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Pruebas de Recordatorios/Calendario

### 1. Crear recordatorio de examen
```bash
curl -X POST http://localhost:8000/api/reminders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "titulo": "Examen de Matemáticas",
    "descripcion": "Repasar capítulos 1-5 del libro",
    "fecha_hora": "2025-11-15 09:00:00",
    "tipo": "examen"
  }'
```

### 2. Crear recordatorio de tarea
```bash
curl -X POST http://localhost:8000/api/reminders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "titulo": "Entrega de proyecto",
    "descripcion": "Proyecto final de programación web",
    "fecha_hora": "2025-11-20 23:59:00",
    "tipo": "tarea"
  }'
```

### 3. Crear recordatorio de estudio para hoy
```bash
curl -X POST http://localhost:8000/api/reminders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "titulo": "Sesión de estudio",
    "descripcion": "Estudiar para el examen de mañana",
    "fecha_hora": "2025-11-05 20:00:00",
    "tipo": "estudio"
  }'
```

### 4. Listar todos los recordatorios
```bash
curl -X GET http://localhost:8000/api/reminders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Filtrar recordatorios por tipo
```bash
curl -X GET "http://localhost:8000/api/reminders?tipo=examen" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 6. Obtener recordatorios de hoy
```bash
curl -X GET http://localhost:8000/api/reminders/today/list \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 7. Obtener próximos recordatorios (7 días)
```bash
curl -X GET http://localhost:8000/api/reminders/upcoming/list \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 8. Obtener próximos recordatorios (14 días)
```bash
curl -X GET "http://localhost:8000/api/reminders/upcoming/list?days=14" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 9. Obtener recordatorios vencidos
```bash
curl -X GET http://localhost:8000/api/reminders/overdue/list \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 10. Vista de calendario (noviembre 2025)
```bash
curl -X GET "http://localhost:8000/api/reminders/calendar/month?year=2025&month=11" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 11. Actualizar recordatorio (cambiar ID por uno real)
```bash
curl -X PUT http://localhost:8000/api/reminders/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "titulo": "Examen de Matemáticas Avanzadas",
    "descripcion": "Repasar capítulos 1-8 del libro, ejercicios adicionales",
    "fecha_hora": "2025-11-15 10:00:00"
  }'
```

### 12. Marcar recordatorio como completado
```bash
curl -X PATCH http://localhost:8000/api/reminders/1/toggle-complete \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 13. Obtener estadísticas de recordatorios
```bash
curl -X GET http://localhost:8000/api/reminders/stats/summary \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

### 14. Eliminar recordatorio
```bash
curl -X DELETE http://localhost:8000/api/reminders/3 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Pruebas de Validación (Errores esperados)

### 1. Crear aplicación con nombre duplicado
```bash
curl -X POST http://localhost:8000/api/app-blocks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nombre_app": "Instagram",
    "categoria": "social"
  }'
```

### 2. Crear recordatorio con fecha pasada
```bash
curl -X POST http://localhost:8000/api/reminders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "titulo": "Recordatorio pasado",
    "fecha_hora": "2025-10-01 10:00:00",
    "tipo": "tarea"
  }'
```

### 3. Crear recordatorio con tipo inválido
```bash
curl -X POST http://localhost:8000/api/reminders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "titulo": "Recordatorio inválido",
    "fecha_hora": "2025-12-01 10:00:00",
    "tipo": "reunion"
  }'
```

---

## Verificar Datos

### Ver todas las aplicaciones bloqueadas
```bash
curl -X GET http://localhost:8000/api/app-blocks \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### Ver todos los recordatorios
```bash
curl -X GET http://localhost:8000/api/reminders \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### Ver estadísticas completas
```bash
echo "=== Estadísticas de Aplicaciones ==="
curl -X GET http://localhost:8000/api/app-blocks/stats/summary \
  -H "Authorization: Bearer $TOKEN" | jq '.data.estadisticas'

echo ""
echo "=== Estadísticas de Recordatorios ==="
curl -X GET http://localhost:8000/api/reminders/stats/summary \
  -H "Authorization: Bearer $TOKEN" | jq '.data.estadisticas'
```

---

## Notas

1. Reemplaza los IDs (1, 2, 3) con los IDs reales que obtengas al crear los recursos
2. Asegúrate de tener jq instalado para formatear las respuestas JSON
3. Modifica las fechas según sea necesario para las pruebas
4. El usuario debe estar previamente registrado para hacer login