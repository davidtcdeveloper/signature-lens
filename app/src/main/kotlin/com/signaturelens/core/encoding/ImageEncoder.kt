package com.signaturelens.core.encoding

import android.graphics.Bitmap
import android.os.Build
import java.io.OutputStream

enum class ImageFormat {
    HEIC,
    JPEG
}

interface ImageEncoder {
    fun encode(bitmap: Bitmap, outputStream: OutputStream): ImageFormat
}

class AndroidImageEncoder(private val heicSupportChecker: HeicSupportChecker) : ImageEncoder {

    override fun encode(bitmap: Bitmap, outputStream: OutputStream): ImageFormat {
        return if (heicSupportChecker.isHeicSupported()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.valueOf("HEIC"), 90, outputStream)
                ImageFormat.HEIC
            } else {
                // Fallback for logic safety, though checker should prevent this
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                ImageFormat.JPEG
            }
        } else {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            ImageFormat.JPEG
        }
    }
}
