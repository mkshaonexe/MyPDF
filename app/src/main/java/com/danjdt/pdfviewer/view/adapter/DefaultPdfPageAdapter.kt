package com.danjdt.pdfviewer.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ssaimy.pdf.databinding.PdfPageBinding
import com.danjdt.pdfviewer.utils.PdfPageQuality
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import java.io.File

class DefaultPdfPageAdapter(
    file: File,
    quality: PdfPageQuality,
    dispatcher: CoroutineDispatcher,
    private val scope: CoroutineScope,
) : PdfPagesAdapter<DefaultPdfPageViewHolder>(file, quality, dispatcher) {

    var isNightReading: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultPdfPageViewHolder {
        val view = PdfPageBinding.inflate(LayoutInflater.from(parent.context), parent, false).root
        return DefaultPdfPageViewHolder(view, scope, ::renderPage)
    }

    override fun onBindViewHolder(holder: DefaultPdfPageViewHolder, position: Int) {
        holder.bind(position, isNightReading)
    }
}