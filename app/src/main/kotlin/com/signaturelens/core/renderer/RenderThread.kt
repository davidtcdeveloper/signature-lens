package com.signaturelens.core.renderer

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.Image
import android.media.ImageReader
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.signaturelens.core.native.NativeProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue

import android.graphics.Bitmap
import android.opengl.GLES30
import java.io.File
import java.io.FileOutputStream

sealed interface RenderRequest {
    data class Preview(val image: Image) : RenderRequest
    data class Capture(val image: Image, val onComplete: (Result<Bitmap>) -> Unit) : RenderRequest
}

class RenderThread(private val context: Context, private val surfaceTexture: SurfaceTexture) : Thread() {
    private var running = true
    private val queue = ConcurrentLinkedQueue<RenderRequest>()
    private var eglHelper: EglHelper? = null
    private var eglSurface: EGLSurface? = null
    private var styleRenderer: StyleRenderer? = null
    
    // Intermediate buffers
    private var previewRgbBuffer: ByteBuffer? = null
    private var captureRgbBuffer: ByteBuffer? = null
    
    // Face detection state
    @Volatile
    private var hasFaces = false

    override fun run() {
        eglHelper = EglHelper()
        eglSurface = eglHelper!!.createWindowSurface(surfaceTexture)
        eglHelper!!.makeCurrent(eglSurface!!)
        
        styleRenderer = StyleRenderer(context)
        styleRenderer!!.init()

        while (running) {
            val request = queue.poll()
            if (request != null) {
                when (request) {
                    is RenderRequest.Preview -> {
                        processPreview(request.image)
                        request.image.close()
                    }
                    is RenderRequest.Capture -> {
                        processCapture(request.image, request.onComplete)
                        request.image.close()
                    }
                }
            } else {
                try {
                    sleep(5)
                } catch (e: InterruptedException) {
                    running = false
                }
            }
        }

        styleRenderer!!.release()
        eglHelper!!.release()
    }

    private fun processPreview(image: Image) {
        val width = image.width
        val height = image.height
        
        val requiredSize = width * height * 4
        if (previewRgbBuffer == null || previewRgbBuffer!!.capacity() < requiredSize) {
            previewRgbBuffer = ByteBuffer.allocateDirect(requiredSize).order(ByteOrder.nativeOrder())
        }
        
        convertYuvToRgb(image, previewRgbBuffer!!)
        
        // Render to screen
        GLES30.glViewport(0, 0, width, height)
        styleRenderer!!.render(previewRgbBuffer!!, width, height, hasFaces)
        eglHelper!!.swapBuffers(eglSurface!!)
    }

    private fun processCapture(image: Image, onComplete: (Result<Bitmap>) -> Unit) {
        try {
            val width = image.width
            val height = image.height
            
            val requiredSize = width * height * 4
            if (captureRgbBuffer == null || captureRgbBuffer!!.capacity() < requiredSize) {
                captureRgbBuffer = ByteBuffer.allocateDirect(requiredSize).order(ByteOrder.nativeOrder())
            }
            
            convertYuvToRgb(image, captureRgbBuffer!!)

            // Create FBO for offscreen rendering
            val fboIds = IntArray(1)
            GLES30.glGenFramebuffers(1, fboIds, 0)
            val fboId = fboIds[0]
            
            val texIds = IntArray(1)
            GLES30.glGenTextures(1, texIds, 0)
            val texId = texIds[0]
            
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, texId, 0)
            
            val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
            if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                throw RuntimeException("Framebuffer not complete: $status")
            }
            
            // Render to FBO
            GLES30.glViewport(0, 0, width, height)
            styleRenderer!!.render(captureRgbBuffer!!, width, height, hasFaces)
            
            // Read pixels
            val pixelBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
            GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixelBuffer)
            
            // Cleanup GL objects
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glDeleteFramebuffers(1, fboIds, 0)
            GLES30.glDeleteTextures(1, texIds, 0)
            
            // Save to Bitmap
            pixelBuffer.rewind()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(pixelBuffer)
            
            onComplete(Result.success(bitmap))
            
        } catch (e: Exception) {
            onComplete(Result.failure(e))
        }
    }

    private fun convertYuvToRgb(image: Image, outBuffer: ByteBuffer) {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        
        NativeProcessor.convertYuvToRgb(
            yPlane.buffer,
            uPlane.buffer,
            vPlane.buffer,
            image.width,
            image.height,
            yPlane.rowStride,
            uPlane.rowStride,
            uPlane.pixelStride,
            outBuffer
        )
    }

    fun enqueueFrame(image: Image) {
        if (queue.size < 2) {
            queue.offer(RenderRequest.Preview(image))
        } else {
            image.close()
        }
    }

    fun setFaceDetectionResult(hasFaces: Boolean) {
        this.hasFaces = hasFaces
    }

    fun capture(image: Image, onComplete: (Result<Bitmap>) -> Unit) {
        queue.offer(RenderRequest.Capture(image, onComplete))
    }

    fun stopRendering() {
        running = false
        try {
            join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}
