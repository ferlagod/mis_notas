# Cómo contribuir

¡Gracias por tu interés en Mis Notas!

## Incidencias

- **Fallos**: indica la versión de la app y de Android, los pasos para reproducirlo y qué esperabas que pasara. Si es un error de sincronización, incluye el mensaje que aparece en Ajustes (sin contraseñas ni la URL de tu servidor si no quieres compartirla).
- **Ideas**: describe el problema que resolverían antes de proponer una solución concreta.
- **Vulnerabilidades**: no las publiques; sigue [SECURITY.md](SECURITY.md).

## Pull requests

1. Haz un *fork* y crea una rama desde `main`.
2. Mantén los cambios pequeños y centrados en un tema.
3. Antes de enviar, comprueba que todo pasa:
   ```bash
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```
4. Si cambias la base de datos, sube `version` en `AppDatabase`, añade una `Migration` y versiona el JSON que se genera en `app/schemas/`. Nunca uses migraciones destructivas: se perderían notas.
5. Si cambias el formato remoto, actualiza [docs/SYNC_FORMAT.md](docs/SYNC_FORMAT.md) y mantén la lectura de los formatos antiguos.
6. Los cambios en `data/sync` o `security` deben venir con tests.

Al contribuir aceptas que tu código se publique bajo la licencia **GPL v3 o posterior**.

## Estilo

- Kotlin con el estilo oficial (`kotlin.code.style=official`).
- Comentarios del código en español.
- Ningún texto visible escrito en el código: todo va en `strings.xml`. Si añades un texto, añádelo en **los cinco** idiomas (`values/` es inglés, más `values-es`, `values-fr`, `values-de` y `values-it`). Lint falla si falta alguna traducción.
- Nada de dependencias propietarias ni servicios de Google Play: la app debe poder publicarse en F-Droid.

## Traducciones

- Para corregir una traducción, edita el `strings.xml` de ese idioma.
- Para añadir un idioma nuevo, crea `app/src/main/res/values-<código>/strings.xml` con todas las claves y añade la entrada en `AppLanguage` (`util/LanguageManager.kt`). La lista de idiomas para el sistema se genera sola (`generateLocaleConfig`).
- Usa `plurals` para textos con números y respeta los marcadores (`%1$s`, `%d`).

## Publicar una versión

1. Actualiza `versionCode` y `versionName` en `app/build.gradle.kts` y añade la entrada en [CHANGELOG.md](CHANGELOG.md).
2. Crea y sube una etiqueta: `git tag v1.0.0 && git push origin v1.0.0`.
3. El flujo `release.yml` compila el APK, lo firma y crea un *release* en GitHub con el APK adjunto.

Para que el flujo firme el APK, añade estos *secrets* en **Settings → Secrets and variables → Actions**:

| Secret | Valor |
|--------|-------|
| `KEYSTORE_BASE64` | El `.jks` en Base64 (`base64 -i misnotas-release.jks \| pbcopy`) |
| `KEYSTORE_PASSWORD` | Contraseña del almacén |
| `KEY_ALIAS` | Alias de la clave (p. ej. `misnotas`) |
| `KEY_PASSWORD` | Contraseña de la clave |

Sin esos *secrets*, el flujo publica un APK **sin firmar**, que Android no deja instalar.

## Publicar en F-Droid

El repositorio ya incluye lo que F-Droid necesita:

- **Licencia**: GPL-3.0-or-later. El texto completo está en [LICENSE](LICENSE) y cada fichero de código y recursos lleva su cabecera `SPDX-License-Identifier`. La app muestra el aviso legal en **Ajustes → Acerca de**.
- **Solo dependencias libres**: nada de Google Play Services, Firebase ni rastreadores. El bloque de dependencias cifrado de Google está desactivado (`dependenciesInfo`).
- **Ficha de la tienda** en `fastlane/metadata/android/<idioma>/`: título, descripciones, icono, gráfico destacado y novedades por `versionCode` (`changelogs/<versionCode>.txt`). Las capturas van en `images/phoneScreenshots/` (1.png, 2.png…).
- **Borrador de la receta** en [`fdroid/metadata/com.ferlagod.misnotas.yml`](fdroid/metadata/com.ferlagod.misnotas.yml).

Pasos:

1. Asegúrate de que la etiqueta `vX.Y.Z` apunta al commit que quieres publicar y súbela (`git push origin vX.Y.Z`).
2. Añade `fastlane/metadata/android/*/changelogs/<versionCode>.txt` para cada versión nueva (máx. 500 caracteres).
3. Haz un *fork* de [fdroiddata](https://gitlab.com/fdroid/fdroiddata), copia la receta en `metadata/com.ferlagod.misnotas.yml` y compruébala con `fdroid lint com.ferlagod.misnotas` y `fdroid build -v -l com.ferlagod.misnotas`.
4. Abre un *merge request* en fdroiddata. Con `UpdateCheckMode: Tags`, las versiones siguientes se detectan solas al subir nuevas etiquetas.
