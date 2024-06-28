package com.example.fraktale

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.opengl.GLSurfaceView
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MandelbrotScreen()
                }
            }
        }
    }
}

@Composable
fun MandelbrotScreen() {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val renderer = remember { MandelbrotRenderer() }
    var glView by remember { mutableStateOf<GLSurfaceView?>(null) }

    val coroutineScope = rememberCoroutineScope()

    var isMovingLeft by remember { mutableStateOf(false) }
    var isMovingRight by remember { mutableStateOf(false) }
    var isMovingUp by remember { mutableStateOf(false) }
    var isMovingDown by remember { mutableStateOf(false) }
    var isZoomingIn by remember { mutableStateOf(false) }
    var isZoomingOut by remember { mutableStateOf(false) }

    LaunchedEffect(isMovingLeft, isMovingRight, isMovingUp, isMovingDown, isZoomingIn, isZoomingOut) {
        while (isMovingLeft || isMovingRight || isMovingUp || isMovingDown || isZoomingIn || isZoomingOut) {
            if (isMovingLeft) offsetX -= 0.01f / zoom
            if (isMovingRight) offsetX += 0.01f / zoom
            if (isMovingUp) offsetY += 0.01f / zoom
            if (isMovingDown) offsetY -= 0.01f / zoom
            if (isZoomingIn) zoom *= 1.01f
            if (isZoomingOut) zoom /= 1.01f

            updateRenderer(renderer, offsetX, offsetY, zoom, glView)
            delay(16) // około 60 FPS
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(2)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                    glView = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        coroutineScope.launch {
                            offsetX -= pan.x / (size.width * zoom)
                            offsetY += pan.y / (size.height * zoom)
                            zoom *= zoomChange
                            renderer.setOffset(offsetX, offsetY)
                            renderer.setZoom(zoom)
                            glView?.requestRender()
                        }
                    }
                }
        )

        // Kontrolki na dole ekranu
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pusta przestrzeń po lewej stronie
                Spacer(modifier = Modifier.width(50.dp))

                // Przyciski kierunkowe na środku
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ControlButton(text = "^",
                        onPress = { isMovingUp = true },
                        onRelease = { isMovingUp = false }
                    )
                    Row {
                        ControlButton(text = "<",
                            onPress = { isMovingLeft = true },
                            onRelease = { isMovingLeft = false }
                        )
                        Spacer(modifier = Modifier.width(50.dp))
                        ControlButton(text = ">",
                            onPress = { isMovingRight = true },
                            onRelease = { isMovingRight = false }
                        )
                    }
                    ControlButton(text = "v",
                        onPress = { isMovingDown = true },
                        onRelease = { isMovingDown = false }
                    )
                }

                // Przyciski zoom po prawej stronie
                Column {
                    ControlButton(text = "+",
                        onPress = { isZoomingIn = true },
                        onRelease = { isZoomingIn = false }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ControlButton(text = "-",
                        onPress = { isZoomingOut = true },
                        onRelease = { isZoomingOut = false }
                    )
                }
            }
        }
    }
}

@Composable
fun ControlButton(text: String, onPress: () -> Unit, onRelease: () -> Unit) {
    Button(
        onClick = { },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xAA800000)
        ),
        modifier = Modifier
            .size(50.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when {
                            event.type == PointerEventType.Press -> onPress()
                            event.type == PointerEventType.Release -> onRelease()
                        }
                    }
                }
            }
    ) {
        Text(text, color = Color.White)
    }
}

fun updateRenderer(renderer: MandelbrotRenderer, offsetX: Float, offsetY: Float, zoom: Float, glView: GLSurfaceView?) {
    renderer.setOffset(offsetX, offsetY)
    renderer.setZoom(zoom)
    glView?.requestRender()
}