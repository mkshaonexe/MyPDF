package com.ssaimy.pdf

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.danjdt.pdfviewer.PdfViewer
import com.danjdt.pdfviewer.interfaces.OnErrorListener
import com.danjdt.pdfviewer.interfaces.OnPageChangedListener
import com.danjdt.pdfviewer.utils.PdfPageQuality
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ssaimy.pdf.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity(), OnPageChangedListener, OnErrorListener {

    private lateinit var binding: ActivityMainBinding
    private var currentUri: Uri? = null
    private var pdfViewer: PdfViewer? = null
    private lateinit var prefs: SharedPreferences
    private var isNightReadingMode = false

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            loadPdfUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("pdf_reader_prefs", Context.MODE_PRIVATE)
        val savedTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isNightReadingMode = prefs.getBoolean("night_reading_mode", false)

        setSupportActionBar(binding.toolbar)
        setupWindowInsets()

        handleIntent(intent)
    }

    private fun setupWindowInsets() {
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isDarkMode
        insetsController.isAppearanceLightNavigationBars = !isDarkMode

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val statusBarInsets = windowInsets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val navBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Prevent header and status bar collision by padding the AppBarLayout
            binding.appBarLayout.updatePadding(
                left = statusBarInsets.left,
                top = statusBarInsets.top,
                right = statusBarInsets.right
            )

            // Ensure the floating page counter and content clear the navigation bar
            val defaultMargin = resources.getDimensionPixelSize(R.dimen.default_margin)
            binding.counterContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBarInsets.bottom + defaultMargin
                rightMargin = navBarInsets.right + defaultMargin
            }

            binding.rootView.updatePadding(bottom = navBarInsets.bottom)

            windowInsets
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null) {
            loadPdfUri(uri)
        } else if (currentUri == null) {
            loadDefaultSample()
        }
    }

    private fun loadDefaultSample() {
        binding.toolbar.title = getString(R.string.app_name)
        binding.toolbar.subtitle = null

        pdfViewer?.close()
        binding.rootView.removeAllViews()

        pdfViewer = PdfViewer.Builder(binding.rootView, lifecycleScope)
            .setMaxZoom(4f)
            .setZoomEnabled(true)
            .setNightMode(isNightReadingMode)
            .quality(PdfPageQuality.QUALITY_1080)
            .setOnErrorListener(this)
            .setOnPageChangedListener(this)
            .setRenderDispatcher(Dispatchers.Default)
            .build()

        pdfViewer?.load(R.raw.sample)
    }

    private fun loadPdfUri(uri: Uri) {
        currentUri = uri
        val fileName = getFileName(uri) ?: "Document.pdf"
        binding.toolbar.title = fileName
        binding.toolbar.subtitle = null

        pdfViewer?.close()
        binding.rootView.removeAllViews()

        pdfViewer = PdfViewer.Builder(binding.rootView, lifecycleScope)
            .setMaxZoom(4f)
            .setZoomEnabled(true)
            .setNightMode(isNightReadingMode)
            .quality(PdfPageQuality.QUALITY_1080)
            .setOnErrorListener(this)
            .setOnPageChangedListener(this)
            .setRenderDispatcher(Dispatchers.Default)
            .build()

        pdfViewer?.load(uri)
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                // Ignore fallback to path
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        updateNightReadingIcon(menu?.findItem(R.id.action_night_reading))
        return super.onPrepareOptionsMenu(menu)
    }

    private fun updateNightReadingIcon(item: MenuItem?) {
        if (item == null) return
        if (isNightReadingMode) {
            item.setIcon(R.drawable.ic_day_mode)
            item.setTitle(R.string.night_reading_mode)
        } else {
            item.setIcon(R.drawable.ic_night_mode)
            item.setTitle(R.string.night_reading_mode)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open -> {
                openDocumentLauncher.launch(arrayOf("application/pdf"))
                true
            }
            R.id.action_night_reading -> {
                isNightReadingMode = !isNightReadingMode
                prefs.edit().putBoolean("night_reading_mode", isNightReadingMode).apply()
                pdfViewer?.setNightMode(isNightReadingMode)
                invalidateOptionsMenu()
                true
            }
            R.id.action_theme -> {
                showThemeDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        val currentPref = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val selectedIndex = when (currentPref) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.theme)
            .setSingleChoiceItems(themes, selectedIndex) { dialog, which ->
                val newMode = when (which) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                prefs.edit().putInt("theme_mode", newMode).apply()
                AppCompatDelegate.setDefaultNightMode(newMode)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onPageChanged(page: Int, total: Int) {
        val text = getString(R.string.pdf_page_counter, page, total)
        binding.tvCounter.text = text
        binding.toolbar.subtitle = text
    }

    override fun onFileLoadError(e: Exception) {
        Toast.makeText(this, R.string.error_loading_pdf, Toast.LENGTH_SHORT).show()
    }

    override fun onAttachViewError(e: Exception) {
        Toast.makeText(this, R.string.error_loading_pdf, Toast.LENGTH_SHORT).show()
    }

    override fun onPdfRendererError(e: Exception) {
        Toast.makeText(this, R.string.error_loading_pdf, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        pdfViewer?.close()
        super.onDestroy()
    }
}