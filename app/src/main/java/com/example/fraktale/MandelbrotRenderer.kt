package com.example.fraktale

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MandelbrotRenderer : GLSurfaceView.Renderer {
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var resolutionHandle: Int = 0
    private var zoomHandle: Int = 0
    private var offsetHandle: Int = 0

    private var zoom = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var width: Int = 0
    private var height: Int = 0

    private val vertexShaderCode =
        """
        attribute vec4 vPosition;
        void main() {
            gl_Position = vPosition;
        }
        """

    private val fragmentShaderCode =
        """
        precision highp float;
        uniform vec2 resolution;
        uniform float zoom;
        uniform vec2 offset;
        
        vec2 cmul(vec2 a, vec2 b) {
            return vec2(a.x*b.x - a.y*b.y, a.x*b.y + a.y*b.x);
        }
        
        vec3 fireColor(float t) {
            vec3 color1 = vec3(0.0, 0.0, 0.0);      // Black
            vec3 color2 = vec3(0.9, 0.1, 0.0);      // Dark Red
            vec3 color3 = vec3(1.0, 0.6, 0.0);      // Orange
            vec3 color4 = vec3(1.0, 0.9, 0.3);      // Yellow
            
            if (t < 0.33) {
                return mix(color1, color2, t * 3.0);
            } else if (t < 0.66) {
                return mix(color2, color3, (t - 0.33) * 3.0);
            } else {
                return mix(color3, color4, (t - 0.66) * 3.0);
            }
        }
        
        void main() {
            vec2 z, c;
            c.x = (gl_FragCoord.x - resolution.x/2.0) / (0.5 * zoom * resolution.y) + offset.x;
            c.y = (gl_FragCoord.y - resolution.y/2.0) / (0.5 * zoom * resolution.y) + offset.y;
            z = c;
            float iter = 0.0;
            const float max_iter = 300.0;
            for(iter = 0.0; iter < max_iter; iter++) {
                if(length(z) > 2.0) break;
                z = cmul(z, z) + c;
            }
            if(iter == max_iter) {
                gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
            } else {
                float t = iter / max_iter;
                t = pow(t, 0.5); // Zwiększa intensywność łuny
                vec3 color = fireColor(t);
                gl_FragColor = vec4(color, 1.0);
            }
        }
        """

    private val vertices = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        program = createProgram(vertexShaderCode, fragmentShaderCode)
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        resolutionHandle = GLES20.glGetUniformLocation(program, "resolution")
        zoomHandle = GLES20.glGetUniformLocation(program, "zoom")
        offsetHandle = GLES20.glGetUniformLocation(program, "offset")
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle, 2,
            GLES20.GL_FLOAT, false,
            8, vertexBuffer
        )
        GLES20.glUniform2f(resolutionHandle, width.toFloat(), height.toFloat())
        GLES20.glUniform1f(zoomHandle, zoom)
        GLES20.glUniform2f(offsetHandle, offsetX, offsetY)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        this.width = width
        this.height = height
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    fun setZoom(newZoom: Float) {
        zoom = newZoom
    }

    fun setOffset(newOffsetX: Float, newOffsetY: Float) {
        offsetX = newOffsetX
        offsetY = newOffsetY
    }
}