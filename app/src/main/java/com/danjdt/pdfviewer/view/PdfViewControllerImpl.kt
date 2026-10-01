package com.danjdt.pdfviewer.view

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.danjdt.pdfviewer.view.adapter.DefaultPdfPageAdapter
import com.danjdt.pdfviewer.interfaces.OnPageChangedListener
import com.danjdt.pdfviewer.interfaces.PdfViewController
import com.danjdt.pdfviewer.utils.PdfPageQuality
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

class PdfViewControllerImpl(
    context: Context,
    private val scope: CoroutineScope,
) : PdfViewController {

    private var view: ZoomableRecyclerView = ZoomableRecyclerView(context)
    private var pageQuality: PdfPageQuality = PdfPageQuality.QUALITY_1080
    private var onPageChangedListener: OnPageChangedListener? = null
    private var dispatcher: CoroutineDispatcher = Dispatchers.IO
    private var lastVisiblePosition = -1
    private var isNightReading: Boolean = false

    override fun setup(file: File) {
        closeCurrentAdapter()
        file.deleteOnExit()
        val adapter = DefaultPdfPageAdapter(file, pageQuality, dispatcher, scope)
        adapter.isNightReading = isNightReading
        view.adapter = adapter
        lastVisiblePosition = -1

        view.post {
            val total = adapter.itemCount
            if (total > 0) {
                lastVisiblePosition = 0
                onPageChangedListener?.onPageChanged(page = 1, total = total)
            }
        }
    }

    override fun setZoomEnabled(isZoomEnabled: Boolean) {
        view.isZoomEnabled = isZoomEnabled
        view.clearOnScrollListeners()
        view.addOnScrollListener(onScrollListener)
    }

    override fun setMaxZoom(maxZoom: Float) {
        view.maxZoom = maxZoom
    }

    override fun setQuality(quality: PdfPageQuality) {
        this.pageQuality = quality
    }

    override fun setOnPageChangedListener(onPageChangedListener: OnPageChangedListener?) {
        this.onPageChangedListener = onPageChangedListener
    }

    override fun setDispatcher(dispatcher: CoroutineDispatcher) {
        this.dispatcher = dispatcher
    }

    override fun setNightMode(isNightMode: Boolean) {
        this.isNightReading = isNightMode
        (view.adapter as? DefaultPdfPageAdapter)?.isNightReading = isNightMode
    }

    override fun goToPosition(position: Int) {
        view.adapter?.run {
            if (position in 0 until itemCount) {
                view.smoothScrollToPosition(position)
            }
        }
    }

    override fun getView(): View = view

    private fun closeCurrentAdapter() {
        (view.adapter as? DefaultPdfPageAdapter)?.close()
    }

    override fun close() {
        closeCurrentAdapter()
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            val position = layoutManager.findFirstVisibleItemPosition()
            if (position != -1 && position != lastVisiblePosition) {
                lastVisiblePosition = position
                onPageChangedListener?.onPageChanged(
                    page = lastVisiblePosition + 1,
                    total = recyclerView.adapter?.itemCount ?: 0
                )
            }
        }
    }
}