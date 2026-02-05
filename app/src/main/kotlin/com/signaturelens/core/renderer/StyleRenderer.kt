package com.signaturelens.core.renderer

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Handles OpenGL ES rendering for the style pipeline.
 */
class StyleRenderer(private val context: Context) {

    private var programId: Int = 0
    private var inputTextureId: Int = 0
    private var vignetteStrengthLoc: Int = -1
    private var hasFacesLoc: Int = -1

    // Quad geometry covering the full screen
    private val vertices = floatArrayOf(
        -1f, -1f, 0f, 0f, 1f,  // Bottom Left
        1f, -1f, 0f, 1f, 1f,   // Bottom Right
        -1f, 1f, 0f, 0f, 0f,   // Top Left
        1f, 1f, 0f, 1f, 0f     // Top Right
    )
    
    private var vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    fun init() {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, "shaders/vertex_shader.glsl")
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, "shaders/fragment_shader.glsl")

        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)

        // Check link status
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES30.glGetProgramInfoLog(programId)
            GLES30.glDeleteProgram(programId)
            throw RuntimeException("Error linking program: $error")
        }

        vignetteStrengthLoc = GLES30.glGetUniformLocation(programId, "uVignetteStrength")
        hasFacesLoc = GLES30.glGetUniformLocation(programId, "uHasFaces")
        
        // Generate input texture
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        inputTextureId = textures[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
    }

    fun render(rgbBuffer: ByteBuffer, width: Int, height: Int, hasFaces: Boolean = false) {
        GLES30.glUseProgram(programId)

        // Upload texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
        // RGB buffer (Native converted)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, rgbBuffer
        )

        // Set uniforms
        GLES30.glUniform1i(GLES30.glGetUniformLocation(programId, "uInputTexture"), 0)
        GLES30.glUniform1f(vignetteStrengthLoc, 1.2f) // Tuning
        GLES30.glUniform1i(hasFacesLoc, if (hasFaces) 1 else 0)

        // Geometry
        vertexBuffer.position(0)
        val posLoc = 0
        val texLoc = 1
        GLES30.glEnableVertexAttribArray(posLoc)
        GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, 5 * 4, vertexBuffer)
        
        vertexBuffer.position(3)
        GLES30.glEnableVertexAttribArray(texLoc)
        GLES30.glVertexAttribPointer(texLoc, 2, GLES30.GL_FLOAT, false, 5 * 4, vertexBuffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(posLoc)
        GLES30.glDisableVertexAttribArray(texLoc)
    }
    
    fun release() {
        GLES30.glDeleteProgram(programId)
        GLES30.glDeleteTextures(1, intArrayOf(inputTextureId), 0)
    }

    private fun loadShader(type: Int, path: String): Int {
        val shaderCode = context.assets.open(path).bufferedReader().use { it.readText() }
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader $path: $error")
        }
        return shader
    }
}
