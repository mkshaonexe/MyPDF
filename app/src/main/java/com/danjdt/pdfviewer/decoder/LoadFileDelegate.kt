package com.danjdt.pdfviewer.decoder

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class LoadFileDelegate(private val input: InputStream, private val file: File) {

    fun doLoadFile(): File {
        input.use { inStream ->
            FileOutputStream(file).use { outStream ->
                inStream.copyTo(outStream)
                outStream.flush()
            }
        }
        return file
    }
}