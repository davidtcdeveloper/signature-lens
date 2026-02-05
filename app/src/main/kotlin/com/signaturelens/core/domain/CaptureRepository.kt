package com.signaturelens.core.domain

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.signaturelens.camera.CameraRepository
import com.signaturelens.core.encoding.ImageEncoder
import com.signaturelens.core.storage.MediaStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CaptureRepository(
    private val cameraRepository: CameraRepository,
    private val imageEncoder: ImageEncoder,
    private val mediaStoreManager: MediaStoreManager,
    private val context: Context? = null
) {
    suspend fun captureStyledImage(orientation: Int = 0): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // 1. Capture raw styled bitmap
            val bitmap = cameraRepository.captureStillBitmap()
            
            // 2. Encode & Save to MediaStore (encapsulated in MediaStoreManager)
            val uri = mediaStoreManager.saveImage(bitmap, imageEncoder, orientation)
            
            // Cleanup
            bitmap.recycle()
            
            Result.success(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteCapture(uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val contentResolver = context?.contentResolver
            if (contentResolver != null) {
                val rowsDeleted = contentResolver.delete(uri, null, null)
                Result.success(rowsDeleted > 0)
            } else {
                Result.failure(Exception("Context not available"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
