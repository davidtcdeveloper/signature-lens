package com.signaturelens.core.storage

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException

class ExifManager {
    fun writeMetadata(
        filePath: String,
        orientation: Int,
        width: Int,
        height: Int
    ) {
        try {
            val exif = ExifInterface(filePath)
            
            // Map Camera2 orientation to EXIF orientation
            val exifOrientation = when (orientation) {
                90 -> ExifInterface.ORIENTATION_ROTATE_90
                180 -> ExifInterface.ORIENTATION_ROTATE_180
                270 -> ExifInterface.ORIENTATION_ROTATE_270
                else -> ExifInterface.ORIENTATION_NORMAL
            }
            
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation.toString())
            exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
            exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, System.currentTimeMillis().toString()) // Basic timestamp
            exif.setAttribute(ExifInterface.TAG_MAKE, android.os.Build.MANUFACTURER)
            exif.setAttribute(ExifInterface.TAG_MODEL, android.os.Build.MODEL)
            
            exif.saveAttributes()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
