# CellFree App

CellFree está formado por una aplicación móvil Android y un backend Laravel que ofrece la API y el panel administrativo.

## Carpetas del proyecto

```text
PROYECTO CELLFREE APP/
├── README.md                         # Vista general y enlaces
├── docs/
│   └── GUIA_EJECUCION_WINDOWS.md     # Instalación local paso a paso
├── cellFreeAppBack-main/             # Backend Laravel, API y panel Blade
├── cellFreeAppAndroid-main/          # Aplicación Android (Kotlin, XML)
└── CellFreeApp_Base/
    └── CellFreeApp.sql               # Respaldo SQL independiente
```

Las carpetas `cellFreeAppBack-main` y `cellFreeAppAndroid-main` son proyectos independientes: cada una contiene su propio README, configuración y herramientas. No se deben combinar sus archivos raíz.

## Inicio rápido

1. Instala Laragon con PHP, Composer y MySQL, y Android Studio con Android SDK 34 y JDK 17.
2. Sigue [`docs/GUIA_EJECUCION_WINDOWS.md`](docs/GUIA_EJECUCION_WINDOWS.md).
3. La guía crea una base de datos vacía con Laravel y no importa el respaldo SQL. Usa ese respaldo solo si necesitas recuperar la base que contiene.

## Componentes

- Backend: Laravel 8, PHP, MySQL y Laravel Sanctum. Admin: `http://127.0.0.1:8000/admin/login`.
- Android: Kotlin/XML, Retrofit y Gradle. URL para el emulador: `http://10.0.2.2:8000/api/`.
- API interactiva: `http://127.0.0.1:8000/api/documentation` después de iniciar el backend.

## Documentación adicional

- Backend: [`cellFreeAppBack-main/README.md`](cellFreeAppBack-main/README.md)
- Android: [`cellFreeAppAndroid-main/README.md`](cellFreeAppAndroid-main/README.md)
- Sincronización de calendarios: [`cellFreeAppBack-main/docs/CALENDAR_SYNC_DOCUMENTATION.md`](cellFreeAppBack-main/docs/CALENDAR_SYNC_DOCUMENTATION.md)
- Panel administrativo: [`cellFreeAppBack-main/docs/ADMIN_PANEL.md`](cellFreeAppBack-main/docs/ADMIN_PANEL.md)

Los archivos `.env` y `local.properties` son locales y no se comparten. Cada persona crea los suyos a partir de los ejemplos, con sus propias credenciales y rutas. Revisa el contenido del respaldo SQL antes de compartirlo para confirmar que no incluya información personal o credenciales.

## Preparar para el repositorio

Esta carpeta aún no contiene metadatos `.git` ni un remoto configurado. El `.gitignore` de esta raíz está preparado para que el proyecto completo se agregue como un solo repositorio, sin incluir secretos, dependencias, compilaciones ni el respaldo SQL con registros. Si ya tienes un repositorio Git, integra esta carpeta en él y revisa `git status` antes de crear el commit.
