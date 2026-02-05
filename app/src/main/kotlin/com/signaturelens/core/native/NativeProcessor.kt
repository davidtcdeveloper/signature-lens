package com.signaturelens.core.native

import java.nio.ByteBuffer

object NativeProcessor {
    init {
        System.loadLibrary("signaturelens-native")
    }

    /**
     * Converts YUV planes to ARGB buffer.
     * All buffers must be direct buffers.
     */
    external fun convertYuvToRgb(
        yBuffer: ByteBuffer,
        uBuffer: ByteBuffer,
        vBuffer: ByteBuffer,
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
        outBuffer: ByteBuffer
    )
}
