// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

/**
 * Utilidad de compresión y redimensionado de imágenes para MisNotas.
 *
 * Transforma imágenes desde un [Uri] en bytes WebP con pérdida (lado máximo 1600 px, calidad 75),
 * respetando la orientación EXIF. El resultado se guarda como adjunto de la nota.
 */
object ImageCompressor {

    const val MAX_DIMENSION_PX = 1600
    const val COMPRESSION_QUALITY = 75
    const val MIME_TYPE = "image/webp"

    /**
     * Comprime la imagen referenciada por [uri] en Dispatchers.IO y devuelve los bytes WebP.
     */
    suspend fun compressImage(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = MAX_DIMENSION_PX,
        quality: Int = COMPRESSION_QUALITY
    ): ByteArray = withContext(Dispatchers.IO) {
        compressImageInternal(context, uri, maxDimensionPx, quality)
    }

    private fun compressImageInternal(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int,
        quality: Int
    ): ByteArray {
        val contentResolver = context.contentResolver

        // 1. Decodificar solo límites para calcular sample size eficiente.
        // Con inJustDecodeBounds, decodeStream devuelve siempre null: el resultado útil está en
        // boundsOptions, así que solo se comprueba que el stream se haya podido abrir.
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val stream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("No se pudo abrir el stream para la URI: $uri")
        stream.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

        val originalWidth = boundsOptions.outWidth
        val originalHeight = boundsOptions.outHeight
        if (originalWidth <= 0 || originalHeight <= 0) {
            throw IllegalArgumentException("No se pudieron determinar las dimensiones de la imagen en: $uri")
        }

        // 2. Calcular inSampleSize para evitar OutOfMemory en fotos de alta resolución
        var sampleSize = 1
        val maxOriginal = max(originalWidth, originalHeight)
        while (maxOriginal / (sampleSize * 2) >= maxDimensionPx) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val sampledBitmap = contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalStateException("Error al decodificar bitmap para: $uri")

        // 3. Obtener orientación EXIF si está disponible
        val orientation = contentResolver.openInputStream(uri)?.use { stream ->
            getExifOrientation(stream)
        } ?: ExifInterface.ORIENTATION_NORMAL

        // 4. Redimensionar manteniendo proporción al lado máximo y corregir rotación
        val correctlyOrientedAndScaledBitmap = scaleAndRotateBitmap(
            source = sampledBitmap,
            maxDimension = maxDimensionPx,
            orientation = orientation
        )

        // 5. Comprimir a WEBP con pérdida
        val outputStream = ByteArrayOutputStream()
        val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        correctlyOrientedAndScaledBitmap.compress(compressFormat, quality, outputStream)

        // Liberar bitmaps si no son reciclados automáticamente
        if (sampledBitmap != correctlyOrientedAndScaledBitmap && !sampledBitmap.isRecycled) {
            sampledBitmap.recycle()
        }
        if (!correctlyOrientedAndScaledBitmap.isRecycled) {
            correctlyOrientedAndScaledBitmap.recycle()
        }

        return outputStream.toByteArray()
    }

    private fun getExifOrientation(inputStream: InputStream): Int {
        return try {
            val exif = ExifInterface(inputStream)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun scaleAndRotateBitmap(
        source: Bitmap,
        maxDimension: Int,
        orientation: Int
    ): Bitmap {
        val width = source.width
        val height = source.height
        val maxSide = max(width, height)

        val matrix = Matrix()

        // Ajustar rotación / espejado según EXIF
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            // Giro + espejo (algunas cámaras frontales y fotos editadas).
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
        }

        // Calcular escalado proporcional
        if (maxSide > maxDimension) {
            val scale = maxDimension.toFloat() / maxSide.toFloat()
            matrix.postScale(scale, scale)
        }

        return Bitmap.createBitmap(
            source,
            0,
            0,
            width,
            height,
            matrix,
            true
        )
    }
}
