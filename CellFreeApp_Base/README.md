# Respaldo de base de datos CellFree

`CellFreeApp.sql` es un volcado SQL conservado como respaldo independiente del backend activo. No forma parte del flujo de instalación de una base nueva: para eso se crea una base vacía y se ejecutan las migraciones de `cellFreeAppBack-main`.

Si necesitas recuperar este respaldo, impórtalo en una base aislada y revisa su versión antes de intentar aplicar migraciones. No lo importes sobre la base de desarrollo o producción sin copia de seguridad y verificación previa.

Antes de compartir el volcado, revisa si contiene datos personales, usuarios, hashes de contraseñas u otra información privada.
