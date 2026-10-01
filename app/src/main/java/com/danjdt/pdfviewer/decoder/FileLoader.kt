package com.danjdt.pdfviewer.decoder

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URL

class FileLoader {

    companion object {

        private fun getTempFile(context: Context): File {
            val fileName = "pdf_${System.currentTimeMillis()}_${(1000..9999).random()}.pdf"
            val file = File(context.cacheDir, fileName)
            file.deleteOnExit()
            return file
        }

        suspend fun loadFile(context: Context, @RawRes resId: Int): File {
            return withContext(Dispatchers.IO) {
                val input = context.resources.openRawResource(resId)
                LoadFileDelegate(input = input, file = getTempFile(context)).doLoadFile()
            }
        }

        suspend fun loadFile(context: Context, url: String): File {
            return withContext(Dispatchers.IO) {
                val imageUrl = URL(url)
                val urlConnection = imageUrl.openConnection()
                val input = urlConnection.getInputStream()
                LoadFileDelegate(input = input, file = getTempFile(context)).doLoadFile()
            }
        }

        suspend fun loadFile(context: Context, input: InputStream): File {
            return withContext(Dispatchers.IO) {
                LoadFileDelegate(input = input, file = getTempFile(context)).doLoadFile()
            }
        }

        suspend fun loadFile(context: Context, uri: Uri): File {
            return withContext(Dispatchers.IO) {
                val input = context.contentResolver.openInputStream(uri)
                input?.let {
                    LoadFileDelegate(input = it, file = getTempFile(context)).doLoadFile()
                } ?: throw FileNotFoundException()
            }
        }
    }
}