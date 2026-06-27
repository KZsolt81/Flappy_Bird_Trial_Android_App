package com.example.flappybird.ui.main

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// Pipe data model
data class Pipe(
    var x: Float, // Normalized 0.0 to 1.0 (left to right)
    val topHeight: Float, // Normalized (height of top pipe)
    val bottomHeight: Float, // Normalized (height of bottom pipe)
    var passed: Boolean = false
)

@Composable
fun FlappyBirdGame(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("flappy_bird_prefs", Context.MODE_PRIVATE) }

    // Game state
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var highScore by remember { mutableStateOf(sharedPreferences.getInt("high_score", 0)) }

    // Physics state
    var birdY by remember { mutableStateOf(0.4f) } // Normalized 0.0 to 1.0
    var birdVelocity by remember { mutableStateOf(0f) }
    val gravity = 0.0004f
    val jumpImpulse = -0.010f

    // Pipes list
    val pipes = remember { mutableStateListOf<Pipe>() }

    // Constants
    val birdRadius = 0.025f // 2.5% of height
    val pipeWidth = 0.16f   // 16% of width
    val pipeGap = 0.31f     // Gap size between top and bottom pipe (31% of height)
    val minPipeHeight = 0.15f
    val maxPipeHeight = 0.48f
    val pipeSpeed = 0.0042f

    // Function to restart the game
    fun restartGame() {
        birdY = 0.4f
        birdVelocity = 0f
        score = 0
        pipes.clear()
        // Spawn first pipe dynamically using pipeGap
        val firstTop = 0.3f
        val firstBottom = 0.9f - firstTop - pipeGap
        pipes.add(Pipe(1.0f, firstTop, firstBottom))
        isGameOver = false
        isPlaying = true
    }

    // Main Game loop
    LaunchedEffect(isPlaying, isGameOver) {
        if (isPlaying && !isGameOver) {
            while (true) {
                // Update physics
                birdVelocity += gravity
                birdY = (birdY + birdVelocity).coerceIn(0f, 1f)

                // Ground collision
                if (birdY >= 0.88f) { // Ground is at 0.9
                    isGameOver = true
                    if (score > highScore) {
                        highScore = score
                        sharedPreferences.edit().putInt("high_score", highScore).apply()
                    }
                    break
                }

                // Move pipes
                var i = 0
                while (i < pipes.size) {
                    val pipe = pipes[i]
                    pipe.x -= pipeSpeed
                    
                    // Spawn new pipe if needed (when the last pipe has moved past 0.55)
                    if (i == pipes.size - 1 && pipe.x <= 0.55f) {
                        val top = Random.nextFloat() * (maxPipeHeight - minPipeHeight) + minPipeHeight
                        val bottom = 1.0f - top - pipeGap - 0.1f // 0.1f is ground spacing
                        pipes.add(Pipe(1.0f, top, bottom))
                    }

                    // Check pass / score
                    if (!pipe.passed && pipe.x + pipeWidth < 0.25f) {
                        pipe.passed = true
                        score++
                    }

                    // Check collision
                    // Bird X is constant at 0.25f
                    val birdX = 0.25f
                    if (birdX + birdRadius > pipe.x && birdX - birdRadius < pipe.x + pipeWidth) {
                        // Horizontal collision overlap. Check vertical bounds.
                        if (birdY - birdRadius < pipe.topHeight || birdY + birdRadius > 0.9f - pipe.bottomHeight) {
                            isGameOver = true
                            if (score > highScore) {
                                highScore = score
                                sharedPreferences.edit().putInt("high_score", highScore).apply()
                            }
                        }
                    }

                    // Remove off-screen pipes
                    if (pipe.x + pipeWidth < 0f) {
                        pipes.removeAt(i)
                    } else {
                        i++
                    }
                }

                if (isGameOver) break

                delay(16) // ~60 FPS
            }
        }
    }

    // Tap to jump
    val onJump = {
        if (!isGameOver) {
            if (!isPlaying) {
                restartGame()
            } else {
                birdVelocity = jumpImpulse
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onJump() })
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val groundY = canvasH * 0.9f

            // 1. Draw sky background gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3FBEFF), // Sky blue
                        Color(0xFF86E3FF)  // Light cyan
                    ),
                    startY = 0f,
                    endY = groundY
                ),
                size = Size(canvasW, groundY)
            )

            // Draw sun
            drawCircle(
                color = Color(0xFFFFF07A),
                radius = canvasW * 0.12f,
                center = Offset(canvasW * 0.8f, canvasH * 0.15f)
            )

            // 2. Draw pipes
            for (pipe in pipes) {
                val pipeLeft = pipe.x * canvasW
                val pipeRight = (pipe.x + pipeWidth) * canvasW
                val w = pipeRight - pipeLeft

                // Top pipe
                val topH = pipe.topHeight * canvasH
                drawRect(
                    color = Color(0xFF73C344), // Main pipe green
                    topLeft = Offset(pipeLeft, 0f),
                    size = Size(w, topH)
                )
                // Top pipe border/details
                drawRect(
                    color = Color(0xFF4A8B22),
                    topLeft = Offset(pipeLeft, 0f),
                    size = Size(w, topH),
                    style = Stroke(width = 4.dp.toPx())
                )
                // Top pipe cap
                val capH = 30.dp.toPx()
                val capW = w + 8.dp.toPx()
                drawRoundRect(
                    color = Color(0xFF87E64C),
                    topLeft = Offset(pipeLeft - 4.dp.toPx(), topH - capH),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF4A8B22),
                    topLeft = Offset(pipeLeft - 4.dp.toPx(), topH - capH),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx())
                )

                // Bottom pipe
                val bottomH = pipe.bottomHeight * canvasH
                val bottomY = groundY - bottomH
                drawRect(
                    color = Color(0xFF73C344),
                    topLeft = Offset(pipeLeft, bottomY),
                    size = Size(w, bottomH)
                )
                drawRect(
                    color = Color(0xFF4A8B22),
                    topLeft = Offset(pipeLeft, bottomY),
                    size = Size(w, bottomH),
                    style = Stroke(width = 4.dp.toPx())
                )
                // Bottom pipe cap
                drawRoundRect(
                    color = Color(0xFF87E64C),
                    topLeft = Offset(pipeLeft - 4.dp.toPx(), bottomY),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF4A8B22),
                    topLeft = Offset(pipeLeft - 4.dp.toPx(), bottomY),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            // 3. Draw ground
            drawRect(
                color = Color(0xFF553A26), // Soil brown
                topLeft = Offset(0f, groundY),
                size = Size(canvasW, canvasH - groundY)
            )
            drawRect(
                color = Color(0xFF63BD32), // Grass green
                topLeft = Offset(0f, groundY),
                size = Size(canvasW, 16.dp.toPx())
            )
            // Grass separator line
            drawLine(
                color = Color(0xFF3B721E),
                start = Offset(0f, groundY),
                end = Offset(canvasW, groundY),
                strokeWidth = 4.dp.toPx()
            )

            // 4. Draw bird
            val birdRadiusPx = birdRadius * canvasH
            val birdCenter = Offset(0.25f * canvasW, birdY * canvasH)

            // Calculate rotation angle based on velocity (tilt up when jumping, tilt down when falling)
            val rotationAngle = (birdVelocity * 2200f).coerceIn(-30f, 60f)

            withTransform({
                rotate(degrees = rotationAngle, pivot = birdCenter)
            }) {
                // Bird body
                drawCircle(
                    color = Color(0xFFFFD54F), // Yellow body
                    radius = birdRadiusPx,
                    center = birdCenter
                )
                drawCircle(
                    color = Color(0xFFE5A800), // Darker yellow border
                    radius = birdRadiusPx,
                    center = birdCenter,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Bird eye
                val eyeOffset = Offset(birdCenter.x + birdRadiusPx * 0.35f, birdCenter.y - birdRadiusPx * 0.3f)
                drawCircle(
                    color = Color.White,
                    radius = birdRadiusPx * 0.3f,
                    center = eyeOffset
                )
                drawCircle(
                    color = Color.Black,
                    radius = birdRadiusPx * 0.12f,
                    center = eyeOffset
                )

                // Bird beak
                val beakPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(birdCenter.x + birdRadiusPx * 0.8f, birdCenter.y - birdRadiusPx * 0.1f)
                    lineTo(birdCenter.x + birdRadiusPx * 1.3f, birdCenter.y + birdRadiusPx * 0.1f)
                    lineTo(birdCenter.x + birdRadiusPx * 0.8f, birdCenter.y + birdRadiusPx * 0.3f)
                    close()
                }
                drawPath(
                    path = beakPath,
                    color = Color(0xFFFF7043) // Orange beak
                )
                drawPath(
                    path = beakPath,
                    color = Color(0xFFD84315), // Border
                    style = Stroke(width = 2.dp.toPx())
                )

                // Bird wing
                val wingPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(birdCenter.x - birdRadiusPx * 0.5f, birdCenter.y)
                    quadraticTo(
                        birdCenter.x - birdRadiusPx * 0.8f, birdCenter.y + birdRadiusPx * 0.5f,
                        birdCenter.x - birdRadiusPx * 0.2f, birdCenter.y + birdRadiusPx * 0.4f
                    )
                    quadraticTo(
                        birdCenter.x + birdRadiusPx * 0.2f, birdCenter.y + birdRadiusPx * 0.2f,
                        birdCenter.x - birdRadiusPx * 0.2f, birdCenter.y
                    )
                    close()
                }
                drawPath(
                    path = wingPath,
                    color = Color(0xFFFFF176) // Lighter wing
                )
                drawPath(
                    path = wingPath,
                    color = Color(0xFFE5A800),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Live score display
        if (isPlaying && !isGameOver) {
            Text(
                text = score.toString(),
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp),
                textAlign = TextAlign.Center
            )
        }

        // Start screen / game over overlay
        if (!isPlaying || isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGameOver) "GAME OVER" else "FLAPPY BIRD",
                            color = if (isGameOver) Color(0xFFD84315) else Color(0xFF2E7D32),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (isGameOver) {
                            Text(
                                text = "Score: $score",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF424242)
                            )
                        }

                        Text(
                            text = "High Score: $highScore",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF757575),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Button(
                            onClick = { restartGame() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB300)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isGameOver) "PLAY AGAIN" else "START GAME",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
