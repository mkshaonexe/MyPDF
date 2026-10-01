package com.danjdt.pdfviewer.view.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.widget.ImageView
import com.google.android.material.card.MaterialCardView
import com.ssaimy.pdf.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class DefaultPdfPageViewHolder(
    view: View,
    private val scope: CoroutineScope,
    private val renderBlock: suspend (position: Int) -> Result<Bitmap>,
) : PdfPageViewHolder(view) {
    private val imageView: ImageView = itemView.findViewById(R.id.image)
    private val cardPage: MaterialCardView? = itemView.findViewById(R.id.cardPage)

    private var renderJob: Job? = null

    companion object {
        private val invertColorFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
                     0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
                     0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
                     0.0f,  0.0f,  0.0f, 1.0f,   0.0f
                )
            )
        )
    }

    override fun bind(position: Int) {
        bind(position, false)
    }

    fun bind(position: Int, isNightReading: Boolean) {
        applyNightReadingStyle(isNightReading)

        renderJob?.cancel()
        renderJob = scope.launch {
            val renderResult = renderBlock(position)
            ensureActive()
            renderResult.onSuccess { page ->
                val targetWidth = if (imageView.width > 0) {
                    imageView.width
                } else {
                    itemView.resources.displayMetrics.widthPixels - 
                        itemView.resources.getDimensionPixelSize(R.dimen.page_margin_horizontal) * 2
                }
                if (targetWidth > 0 && page.width > 0) {
                    imageView.layoutParams.height = (page.height.toDouble() * targetWidth / page.width).toInt()
                }
                imageView.setImageBitmap(page)
            }
        }
    }

    private fun applyNightReadingStyle(isNightReading: Boolean) {
        if (isNightReading) {
            imageView.colorFilter = invertColorFilter
            cardPage?.setCardBackgroundColor(Color.parseColor("#1C1D21"))
            cardPage?.strokeColor = Color.parseColor("#33363F")
        } else {
            imageView.colorFilter = null
            cardPage?.setCardBackgroundColor(Color.WHITE)
            cardPage?.strokeColor = Color.parseColor("#DDE1E8")
        }
    }
}