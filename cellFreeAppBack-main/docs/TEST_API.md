# Pruebas de la API de Autenticación

## Verificación Rápida

### 1. Probar endpoint de registro:
```bash
curl -X POST http://localhost:8000/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "name": "Usuario Prueba",
    "email": "prueba@example.com",
    "password": "password123",
    "password_confirmation": "password123"
  }'
```

### 2. Probar endpoint de login:
```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "email": "prueba@example.com",
    "password": "password123"
  }'
```

### 3. Probar endpoint protegido (usar el token del login):
```bash
curl -X GET http://localhost:8000/api/auth/user \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

## Resultados Esperados

### Registro Exitoso:
```json
{
    "status": "success",
    "message": "Usuario registrado exitosamente",
    "data": {
        "user": {
            "id": 1,
            "name": "Usuario Prueba",
            "email": "prueba@example.com",
            "email_verified_at": null,
            "created_at": "2025-11-05T...",
            "updated_at": "2025-11-05T..."
        },
        "access_token": "1|...",
        "token_type": "Bearer"
    }
}
```

### Login Exitoso:
```json
{
    "status": "success",
    "message": "Inicio de sesión exitoso",
    "data": {
        "user": {...},
        "access_token": "2|...",
        "token_type": "Bearer"
    }
}
```

## Archivos Importantes Creados:

1. **AuthController** - `app/Http/Controllers/Api/AuthController.php`
2. **Rutas API** - `routes/api.php` (actualizado)
3. **Validaciones** - `app/Http/Requests/Api/`
4. **Traits y Middleware** - Para respuestas consistentes
5. **Documentación** - `API_DOCUMENTATION.md`

## Status del Proyecto:
✅ Controlador de autenticación creado
✅ Rutas API configuradas  
✅ Validaciones implementadas
✅ Modelo User actualizado
✅ Middleware creado
✅ Documentación completa
✅ Laravel Sanctum configurado y funcionando

## Próximos Pasos:
1. Probar los endpoints con las peticiones cURL de arriba
2. Integrar con tu aplicación frontend/móvil
3. Configurar rate limiting si es necesario
4. Personalizar validaciones según tus necesidades