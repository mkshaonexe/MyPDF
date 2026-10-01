package com.danjdt.pdfviewer.renderer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.danjdt.pdfviewer.utils.PdfPageQuality
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class PdfPageRenderer(
    file: File,
    private val quality: PdfPageQuality,
    private val dispatcher: CoroutineDispatcher,
) {
    private val deferredMap = mutableMapOf<Int, Deferred<Result<Bitmap>>>()
    private val mutex = Mutex()

    private val fileDescriptor: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val pdfRenderer: PdfRenderer = PdfRenderer(fileDescriptor)

    val pageCount: Int
        get() = try {
            pdfRenderer.pageCount
        } catch (e: Exception) {
            0
        }

    @Suppress("DeferredResultUnused")
    suspend fun render(position: Int): Result<Bitmap> {
        return withContext(dispatcher) {
            getBitmapAsync(position).await().also {
                mutex.withLock {
                    deferredMap.remove(position)
                }
            }
        }
    }

    private suspend fun getBitmapAsync(position: Int): Deferred<Result<Bitmap>> = mutex.withLock {
        deferredMap.getOrPut(position) {
            coroutineScope {
                async {
                    runCatching {
                        renderPage(position)
                    }.onFailure { throwable ->
                        Log.e("PdfPageRenderer", "Page #$position render has failed", throwable)
                    }
                }
            }
        }
    }

    private fun renderPage(position: Int): Bitmap {
        synchronized(pdfRenderer) {
            return pdfRenderer.openPage(position).use { page ->
                val width = quality.value
                val height = (quality.value.toLong() * page.height / page.width).toInt()
                // Create a bitmap with pdf page dimensions
                val bitmap = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                // Fill with white background so transparent PDF pages render correctly
                bitmap.eraseColor(Color.WHITE)

                // Render the page onto the Bitmap.
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

    fun close() {
        try {
            pdfRenderer.close()
        } catch (e: Exception) {
            Log.e("PdfPageRenderer", "Error closing PdfRenderer", e)
        }
        try {
            fileDescriptor.close()
        } catch (e: Exception) {
            Log.e("PdfPageRenderer", "Error closing ParcelFileDescriptor", e)
        }
    }
}