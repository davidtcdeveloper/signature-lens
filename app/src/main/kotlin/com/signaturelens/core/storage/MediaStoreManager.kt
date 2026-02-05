package com.signaturelens.core.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.signaturelens.core.encoding.ImageEncoder
import com.signaturelens.core.encoding.ImageFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreManager(
    private val context: Context,
) {
    suspend fun saveImage(
        bitmap: Bitmap,
        encoder: ImageEncoder,
        orientation: Int
    ): Uri = withContext(Dispatchers.IO) {
        
        val timestamp = System.currentTimeMillis()
        val filename = "SignatureLens_${timestamp}"
        
        // We'll create a temporary entry first to write data
        // Determine format based on encoder logic (requires a dry run or logic check? 
        // Better: Let encoder write to stream. But we need MIME type for MediaStore insert.)
        // Refinement: We need to know MIME type BEFORE insert for optimal behavior, 
        // though we can update it later.
        
        // Strategy: Write to a temporary file first (allows EXIF writing + knowing format), then copy to MediaStore.
        // This is robust for EXIF handling since ExifInterface works best with Files/FileDescriptors.
        
        val tempFile = File(context.cacheDir, "$filename.tmp")
        val format = try {
            tempFile.outputStream().use { out ->
                encoder.encode(bitmap, out)
            }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }

        // Add EXIF to temp file
        ExifManager().writeMetadata(tempFile.absolutePath, orientation, bitmap.width, bitmap.height)

        // Insert into MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.${if (format == ImageFormat.HEIC) "heic" else "jpg"}")
            put(MediaStore.Images.Media.MIME_TYPE, if (format == ImageFormat.HEIC) "image/heic" else "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/SignatureLens")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        try {
            resolver.openOutputStream(uri)?.use { out ->
                tempFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: throw IllegalStateException("Failed to open MediaStore output stream")

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        } finally {
            tempFile.delete()
        }
    }
}
