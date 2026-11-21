package com.bypass.ide.ui.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.bypass.ide.ui.theme.CyanElectric
import com.bypass.ide.ui.theme.CyanSoft
import com.bypass.ide.ui.theme.EmeraldMatrix
import com.bypass.ide.ui.theme.ObsidianBg
import com.bypass.ide.ui.theme.ObsidianBorder
import com.bypass.ide.ui.theme.ObsidianSurface
import com.bypass.ide.ui.theme.ObsidianSurfaceElevated
import com.bypass.ide.ui.theme.RoseError
import com.bypass.ide.ui.theme.TextMuted
import com.bypass.ide.ui.theme.TextPrimary
import com.bypass.ide.ui.theme.TextSecondary
import com.bypass.ide.ui.theme.VioletPulse
import java.util.Locale
import kotlin.math.sin

@Composable
fun VoiceCallOverlay(
    transcript: String,
    responseText: String,
    isThinking: Boolean,
    errorMessage: String?,
    autoSpeak: Boolean,
    onAskMaria: (String, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf("Tap Mic to speak with Maria") }
    var typedFallback by remember { mutableStateOf("") }

    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
            }
        }
        engine
    }

    val speakReply: (String) -> Unit = { reply ->
        if (autoSpeak) {
            tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "maria_reply")
        }
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    val startListening = {
        if (speechRecognizer == null) {
            voiceStatus = "Speech recognition unavailable on this device. Type below."
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    voiceStatus = "Listening..."
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                    voiceStatus = "Processing voice..."
                }

                override fun onError(error: Int) {
                    isListening = false
                    voiceStatus = "No speech captured (code $error). Try again or type below."
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (spoken.isNotBlank()) {
                        voiceStatus = "Maria thinking..."
                        onAskMaria(spoken, speakReply)
                    } else {
                        voiceStatus = "Tap Mic to speak again"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            runCatching {
                speechRecognizer.startListening(intent)
            }.onFailure {
                voiceStatus = "Unable to start mic: ${it.localizedMessage}"
                isListening = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            voiceStatus = "Microphone permission denied. Use text input below."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            tts.stop()
            tts.shutdown()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = ObsidianBg.copy(alpha = 0.96f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "MARIA VOICE ASSISTANT",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanSoft
                )

                AdvancedNeuralOrb(
                    active = isListening || isThinking,
                    modifier = Modifier.size(136.dp)
                )

                PremiumEqualizer(
                    active = isListening || isThinking,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(28.dp)
                )

                Text(
                    text = voiceStatus,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isListening) EmeraldMatrix else TextSecondary,
                    textAlign = TextAlign.Center
                )

                if (transcript.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "You: $transcript",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanSoft,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: responseText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (errorMessage != null) RoseError else TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Typed voice fallback input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = typedFallback,
                        onValueChange = { typedFallback = it },
                        placeholder = { Text("Ask Maria via text...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("maria_text_input")
                    )
                    IconButton(
                        onClick = {
                            if (typedFallback.isNotBlank() && !isThinking) {
                                val q = typedFallback
                                typedFallback = ""
                                onAskMaria(q, speakReply)
                            }
                        },
                        enabled = typedFallback.isNotBlank() && !isThinking,
                        modifier = Modifier.testTag("maria_text_send")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Ask Maria",
                            tint = CyanElectric
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                isListening = false
                                voiceStatus = "Stopped listening"
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    startListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) EmeraldMatrix else CyanElectric,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("maria_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isListening) "Stop Mic" else "Speak")
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoseError.copy(alpha = 0.2f),
                            contentColor = RoseError
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("maria_end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedNeuralOrb(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "orb_pulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (active) 1600 else 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_phase"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.34f
        val pulse = if (active) (sin(phase) * 8f) else (sin(phase) * 3f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CyanElectric.copy(alpha = 0.42f),
                    VioletPulse.copy(alpha = 0.16f),
                    ObsidianBg.copy(alpha = 0f)
                ),
                center = center,
                radius = size.minDimension * 0.5f
            ),
            radius = size.minDimension * 0.48f,
            center = center
        )

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(CyanElectric, EmeraldMatrix, VioletPulse)
            ),
            radius = baseRadius + pulse,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CyanSoft.copy(alpha = 0.85f), CyanElectric.copy(alpha = 0.2f)),
                center = center,
                radius = baseRadius * 0.72f
            ),
            radius = (baseRadius * 0.65f) + (pulse * 0.5f),
            center = center
        )
    }
}

@Composable
fun PremiumEqualizer(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eq_anim")
    val tick by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_tick"
    )

    Canvas(modifier = modifier) {
        val barCount = 12
        val gap = 6.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val wave = if (active) {
                (0.3f + 0.7f * ((sin(tick + i * 0.65f) + 1f) / 2f))
            } else {
                0.2f
            }
            val barH = size.height * wave
            val left = i * (barWidth + gap)
            val top = (size.height - barH) / 2f
            drawRoundRect(
                color = if (i % 2 == 0) CyanElectric else EmeraldMatrix,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}
