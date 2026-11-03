# Cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); versiones según [SemVer](https://semver.org/lang/es/).

## [1.0.0] - 2026-11-03

Primera versión pública.

### Añadido
- Sincronización v2 con Nextcloud: detección de cambios por ETag, subidas con `If-Match` y nueva ronda automática si el servidor cambió mientras tanto.
- `keyinfo.json` en el servidor: la misma contraseña maestra funciona en todos los dispositivos y una contraseña incorrecta se rechaza antes de subir nada.
- Conflictos sin pérdida de datos: se conservan ambas versiones como "(conflicto)".
- Los borrados se propagan entre dispositivos (lápidas) y la papelera se sincroniza.
- Imágenes como adjuntos cifrados independientes.
- Sincronización periódica en segundo plano (WorkManager) y al volver a la app.
- Markdown: bloques de código, listas numeradas, listas anidadas y enlaces.
- Estado de la última sincronización y sección "Acerca de" en Ajustes.
- Licencia GPL v3, README, guía de contribución, política de seguridad y CI.

### Cambiado
- Todo el contenido de cada nota (también carpeta, color y fechas) va cifrado en un único bloque vinculado a su id.
- PBKDF2 sube a 600.000 iteraciones en cuentas nuevas.
- Imágenes de hasta 1600 px (antes 800 px).
- Las notas ya no se incluyen en la copia de seguridad en la nube de Google.

### Corregido
- Las ediciones se guardan también con el gesto "atrás" y al salir de la app.
- Una sincronización ya no sobrescribe lo que estás escribiendo.
- Las notas nuevas ya no se duplican al guardarse varias veces.
- La papelera ya no se vaciaba sola al sincronizar.
- Recibir una carpeta desde el servidor ya no sacaba sus notas de ella.
- Abrir y cerrar una nota ya no cambia su fecha de modificación.
- Derivar la clave ya no bloquea la interfaz.

### Migración desde 1.0
- La base de datos local se migra sin pérdida de datos.
- Los ficheros antiguos del servidor se leen y se reescriben en el formato nuevo. Actualiza primero el dispositivo donde configuraste la cuenta.
