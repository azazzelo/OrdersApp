package com.example.ordersapp // Или com.example.ordersapp.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Копирует содержимое файла из исходного URI во внутреннее хранилище приложения.
 *
 * @param context Контекст приложения.
 * @param uri Исходный URI файла (например, content://...).
 * @param subdirectory Имя поддиректории во внутреннем хранилище (по умолчанию "images").
 * @return Uri скопированного файла (file://...) или null в случае ошибки.
 */
fun copyUriToInternalStorage(context: Context, uri: Uri, subdirectory: String = "images"): Uri? {
    var inputStream = try {
        // Пытаемся открыть поток из исходного URI
        context.contentResolver.openInputStream(uri)
    } catch (e: Exception) {
        Log.e("CopyUtils", "Failed to open input stream from URI: $uri", e)
        return null
    } ?: run {
        Log.e("CopyUtils", "ContentResolver returned null stream for URI: $uri")
        return null // Если openInputStream вернул null
    }


    // Убедимся, что директория для сохранения существует
    val internalDir = File(context.filesDir, subdirectory)
    if (!internalDir.exists()) {
        if (!internalDir.mkdirs()) {
            Log.e("CopyUtils", "Failed to create directory: ${internalDir.absolutePath}")
            try { inputStream.close() } catch (e: IOException) {} // Закрываем входной поток
            return null
        }
        Log.d("CopyUtils", "Created directory: ${internalDir.absolutePath}")
    }

    // Генерируем уникальное имя файла, сохраняя расширение, если оно есть
    // (хотя для Glide расширение не обязательно, но полезно для отладки)
    val originalFileName = getFileNameFromUri(context, uri) ?: UUID.randomUUID().toString()
    val extension = originalFileName.substringAfterLast('.', "")
    val uniqueFileName = "${UUID.randomUUID()}${if (extension.isNotEmpty()) ".$extension" else ""}"

    val outputFile = File(internalDir, uniqueFileName)
    var outputStream: FileOutputStream? = null
    var success = false

    Log.d("CopyUtils", "Attempting to copy to: ${outputFile.absolutePath}")

    try {
        outputStream = FileOutputStream(outputFile)
        // Копируем байты из входного потока в выходной
        val copiedBytes = inputStream.copyTo(outputStream)
        outputStream.flush() // Убедимся, что все записано
        success = true
        Log.i("CopyUtils", "Successfully copied $copiedBytes bytes to: ${outputFile.absolutePath}")
    } catch (e: IOException) {
        Log.e("CopyUtils", "IOException during copy to: ${outputFile.absolutePath}", e)
        // Удаляем файл, если копирование не удалось
        if (outputFile.exists()) {
            outputFile.delete()
        }
    } catch (e: Exception) {
        Log.e("CopyUtils", "Unexpected exception during copy to: ${outputFile.absolutePath}", e)
        if (outputFile.exists()) {
            outputFile.delete()
        }
    }
    finally {
        // Всегда закрываем потоки
        try {
            inputStream.close()
        } catch (e: IOException) {
            Log.e("CopyUtils", "Error closing input stream", e)
        }
        try {
            outputStream?.close()
        } catch (e: IOException) {
            Log.e("CopyUtils", "Error closing output stream", e)
        }
    }

    // Возвращаем URI файла во внутреннем хранилище или null
    return if (success && outputFile.exists()) {
        Uri.fromFile(outputFile)
    } else {
        null
    }
}

// Вспомогательная функция для получения имени файла из content URI (не всегда работает)
private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return it.getString(nameIndex)
                }
            }
        }
    }
    // Если не content URI или не удалось получить имя, возвращаем последнюю часть пути
    return uri.lastPathSegment
}