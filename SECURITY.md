# Seguridad

## Informar de una vulnerabilidad

**No abras una incidencia pública** para fallos de seguridad. Usa el aviso privado de GitHub:
pestaña **Security → Report a vulnerability** del repositorio
([enlace directo](https://github.com/ferlagod/mis_notas/security/advisories/new)).

Indica la versión de la app, los pasos para reproducirlo y el impacto. Intentaré responder en un plazo de 7 días.

Este es un proyecto personal mantenido en el tiempo libre y **no ha pasado una auditoría de seguridad independiente**.

## Modelo de amenazas

Mis Notas protege el contenido de tus notas frente a **quien controla o accede al servidor** (el administrador de Nextcloud, un atacante que lo comprometa, o quien intercepte el tráfico).

| Protegido | No protegido |
|-----------|--------------|
| Título, texto, imágenes, carpetas, colores, fechas, si está fijada o en la papelera | Número de notas y carpetas, tamaño aproximado de cada fichero |
| Integridad: el servidor no puede modificar, mezclar ni intercambiar ficheros sin que la app lo detecte | Cuándo se crea, modifica o borra cada fichero (metadatos de WebDAV) |
| Credenciales y clave en el dispositivo (Android Keystore) | Un teléfono desbloqueado y comprometido (root, malware) |
| | Que el servidor borre ficheros o devuelva versiones antiguas (*rollback*) |

En el teléfono la base de datos de notas **no está cifrada por la app**: depende del cifrado de almacenamiento de Android y del aislamiento entre apps. Las notas se excluyen de la copia de seguridad en la nube de Google.

## Diseño criptográfico

### Derivación de clave

- `PBKDF2WithHmacSHA256`, 600.000 iteraciones (las cuentas creadas con la v1.0 conservan 65.536), salt aleatorio de 16 bytes, clave de 256 bits.
- El salt y el número de iteraciones se publican en `keyinfo.json` dentro de la carpeta de Nextcloud, para que cualquier dispositivo derive la misma clave.
- `keyinfo.json` contiene también un **verificador**: la cadena fija `misnotas-key-check-v1` cifrada con la clave (AAD `keyinfo`). Sirve para comprobar la contraseña sin descargar notas y evita que un dispositivo con una contraseña equivocada suba datos ilegibles.

### Cifrado

- `AES/GCM/NoPadding`, clave de 256 bits, IV aleatorio de 12 bytes por operación, etiqueta de 128 bits.
- Formato binario: `IV (12) ‖ texto cifrado ‖ etiqueta (16)`; en JSON va en Base64.
- **Datos asociados (AAD)**: `note:<id>`, `folder:<id>` o `attachment:<id>`. Un bloque cifrado solo se descifra en el fichero al que pertenece.

### Almacenamiento local

- Credenciales de Nextcloud, salt, verificador y clave derivada: `EncryptedSharedPreferences` (AES-256-GCM / AES-256-SIV con clave maestra en Android Keystore).
- "Bloquear" borra la clave derivada del dispositivo; hace falta la contraseña maestra para volver a abrir la app con la cuenta conectada.

### Transporte

- HTTPS con la validación de certificados por defecto de Android. La app no acepta tráfico HTTP sin cifrar ni certificados autofirmados que no estén instalados en el sistema.
- Autenticación con *contraseña de aplicación* de Nextcloud (HTTP Basic sobre TLS).

## Limitaciones conocidas

- No se puede cambiar la contraseña maestra ni rotar la clave.
- No hay protección contra *rollback*: un servidor malicioso podría servir una versión anterior, válida, de una nota.
- Las lápidas de borrado (`"deleted": true`) no van cifradas: revelan el identificador y la fecha de borrado de una nota, pero no su contenido.
