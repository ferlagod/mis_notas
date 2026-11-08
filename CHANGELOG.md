# Cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); versiones según [SemVer](https://semver.org/lang/es/).

## [1.1.0] - 2026-11-08

### Añadido
- Editor: deshacer y rehacer, continuación automática de listas al pulsar Intro (y salir de la lista con un elemento vacío), formato que se aplica a la línea o selección (y se quita al repetirlo), botones de lista numerada y enlace, varias imágenes a la vez y tiempo de lectura.
- Menú de la nota: compartir con otras apps, copiar el texto, duplicar, cambiar color (ahora los 9 colores) y mover de carpeta.
- Selección múltiple con pulsación larga para fijar, colorear, mover, borrar o restaurar varias notas.
- "Deshacer" tras mover notas a la papelera.
- Ordenar notas por fecha de modificación o por título; la vista de cuadrícula/lista y el orden se recuerdan.
- Búsqueda sin distinguir acentos, con varias palabras y resaltado de coincidencias; buscador global en la pantalla principal.
- Botón "Nueva nota" en la pantalla principal y acceso directo en el icono de la app.
- Crear notas desde "Compartir" en otras apps (texto e imágenes).
- Exportar todas las notas a un ZIP de Markdown (con carpetas, imágenes y metadatos) e importar `.zip`, `.md` y `.txt`.
- Bloqueo de la app con huella, cara o PIN, que también oculta las notas en las apps recientes (Android 13+).
- Tema claro, oscuro o del sistema.
- Vaciado automático de la papelera a los 30 días (se puede desactivar).
- Tablas Markdown, enlaces en direcciones web sueltas y visor de imágenes con zoom.
- Progreso de las listas de tareas en las tarjetas, fechas relativas ("hace 5 min") y estados vacíos ilustrados.
- Pantalla de Ajustes completa (Apariencia, Privacidad, Notas, Copia de seguridad, Sincronización, Idioma).

### Cambiado
- El icono de la app es ahora el logo de Mis Notas (antes se veía el icono genérico de Android), con versión monocroma para los iconos temáticos.
- Las notas que se quedan vacías al salir del editor ya no se guardan; si tenían contenido van a la papelera (con "Deshacer").
- Las carpetas sin color usan el color de acento de la app.

### Corregido
- Una carpeta podía moverse dentro de una de sus subcarpetas, creando un ciclo que la hacía desaparecer de la lista.
- La paleta de colores se salía del diálogo de carpetas en pantallas estrechas.
- Los botones de título, lista, tarea y cita insertaban el marcador en mitad de la línea.
- `nombres_con_guiones_bajos` se mostraban en cursiva.
- La búsqueda seguía activa al volver atrás con el gesto del sistema.
- El título de una carpeta abierta no se actualizaba al renombrarla.
- La barra superior de la lista de notas quedaba bajo la barra de estado.
- Destello blanco al abrir la app en modo oscuro.
- Los avisos se quedaban en cola en lugar de sustituirse.

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
- Interfaz traducida a español, inglés, francés, alemán e italiano, con selector de idioma en Ajustes ("Idioma del sistema" por defecto). En Android 13+ usa el idioma por aplicación del sistema.
- Licencia GPL v3, README, guía de contribución, política de seguridad y CI.

### Cambiado
- Todo el contenido de cada nota (también carpeta, color y fechas) va cifrado en un único bloque vinculado a su id.
- PBKDF2 sube a 600.000 iteraciones en cuentas nuevas.
- Imágenes de hasta 1600 px (antes 800 px).
- Las notas ya no se incluyen en la copia de seguridad en la nube de Google.
- Las fechas se muestran con el formato del idioma activo.

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
