package com.signaturelens.core.intelligence

import android.media.Image
import android.graphics.Bitmap

/**
 * Utility for converting YUV image data to RGB format.
 * Handles YUV 4:2:0 planar (I420) and NV21 formats.
 */
object YuvConverter {
    /**
     * Convert YUV_420_888 image format to NV21 byte array.
     * Handles both planar and semi-planar UV layouts.
     */
    fun imageToNv21(image: Image): ByteArray {
        val planes = image.planes
        val ySize = planes[0].buffer.remaining()
        val uSize = planes[1].buffer.remaining()
        val vSize = planes[2].buffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y plane
        planes[0].buffer.get(nv21, 0, ySize)
        val pixelStride = planes[1].pixelStride

        if (pixelStride == 1) {
            // Planar UV
            planes[1].buffer.get(nv21, ySize, uSize)
            planes[2].buffer.get(nv21, ySize + uSize, vSize)
        } else {
            // Semi-planar or interleaved UV - need to deinterleave
            deinterleaveUv(planes, nv21, ySize, uSize, vSize, pixelStride)
        }

        return nv21
    }

    /**
     * Convert NV21 byte array to RGB pixel array.
     * Standard YUV to RGB conversion with BT.601 coefficients.
     */
    fun nv21ToRgb(data: ByteArray, width: Int, height: Int): IntArray {
        val pixels = IntArray(width * height)
        val uvOffset = width * height

        for (y in 0 until height) {
            for (x in 0 until width) {
                val yval = data[y * width + x].toInt() and 0xff
                val uvPixelOffset = (y / 2) * width + (x / 2) * 2

                val u = (data[uvOffset + uvPixelOffset].toInt() and 0xff) - 128
                val v = (data[uvOffset + uvPixelOffset + 1].toInt() and 0xff) - 128

                pixels[y * width + x] = calculateRgbPixel(yval, u, v)
            }
        }

        return pixels
    }

    /**
     * Downscale YUV image to a Bitmap with max width [maxWidth].
     * Reduces computation overhead for ML tasks.
     */
    fun downscaleImageToBitmap(image: Image, maxWidth: Int): Bitmap {
        val width = image.width
        val height = image.height

        // Calculate scale factor
        val scale = if (width > maxWidth) width / maxWidth else 1
        val scaledWidth = width / scale
        val scaledHeight = height / scale

        // Convert YUV to RGB
        val nv21Data = imageToNv21(image)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val rgbData = nv21ToRgb(nv21Data, width, height)
        bitmap.setPixels(rgbData, 0, width, 0, 0, width, height)

        // Downscale
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        bitmap.recycle()
        return scaledBitmap
    }

    private fun deinterleaveUv(
        planes: Array<Image.Plane>,
        nv21: ByteArray,
        ySize: Int,
        uSize: Int,
        vSize: Int,
        pixelStride: Int
    ) {
        val uvBuffer = ByteArray(uSize + vSize)
        planes[1].buffer.get(uvBuffer, 0, uSize)
        planes[2].buffer.get(uvBuffer, uSize, vSize)

        // Deinterleave U and V components
        for (i in 0 until uSize step pixelStride) {
            nv21[ySize + i / 2] = uvBuffer[i]
        }
        for (i in 0 until vSize step pixelStride) {
            nv21[ySize + uSize / 2 + i / 2] = uvBuffer[uSize + i]
        }
    }

    private fun calculateRgbPixel(yval: Int, u: Int, v: Int): Int {
        val r = (yval + (1.402f * v)).toInt().coerceIn(0, 255)
        val g = (yval - 0.344f * u - 0.714f * v).toInt().coerceIn(0, 255)
        val b = (yval + (1.772f * u)).toInt().coerceIn(0, 255)

        return -0x1000000 or (r shl 16) or (g shl 8) or b
    }
}
