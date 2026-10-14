package com.adam.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private val serverUrl = "https://pythonkareem.pythonanywhere.com/ask_adam"
    private lateinit var speechRecognizer: SpeechRecognizer
    var recognizedTextState = mutableStateOf("اضغط على الكرة لنطق 'يا آدم' أو بدء التحدث...")
    var adamReplyState = mutableStateOf("أنا آدم، المساعد الذكي لمكتبة سنتر آدم. تحت أمرك يا بشمهندس كريم!")
    var isListeningState = mutableStateOf(false)
    var isThinkingState = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS))
        setupSpeechRecognizer()

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFFFFF)) {
                MainScreen(
                    onOrbClick = { startListening() },
                    isListening = isListeningState.value,
                    isThinking = isThinkingState.value,
                    recognizedText = recognizedTextState.value,
                    adamReply = adamReplyState.value
                )
            }
        }
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListeningState.value = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListeningState.value = false }
                override fun onError(error: Int) { isListeningState.value = false }
                override fun onResults(results: Bundle?) {
                    isListeningState.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) sendToServer(matches[0])
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
            }
            speechRecognizer.startListening(intent)
        }
    }

    private fun sendToServer(text: String) {
        isThinkingState.value = true
        adamReplyState.value = "آدم بيفكر..."
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val json = JSONObject().apply { put("text", text); put("role", "kareem") }
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(serverUrl).post(body).build()
                val response = client.newCall(request).execute()
                val reply = JSONObject(response.body?.string() ?: "").optString("reply", "تم التنفيذ.")
                withContext(Dispatchers.Main) {
                    isThinkingState.value = false
                    adamReplyState.value = reply
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isThinkingState.value = false
                    adamReplyState.value = "خطأ في الاتصال بالسيرفر."
                }
            }
        }
    }
}

@Composable
fun MainScreen(onOrbClick: () -> Unit, isListening: Boolean, isThinking: Boolean, recognizedText: String, adamReply: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isListening || isThinking) 1.2f else 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse), label = "scale"
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFFFFF)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(30.dp))
            Text(text = "الوكيل آدم | سنتر آدم", color = Color(0xFF000000), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = "مساعدك الذكي بتصميم سيستم الإدارة", color = Color(0xFFDC3545), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(160.dp).scale(scale).background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = if (isListening) listOf(Color(0xFFDC3545), Color(0xFF000000)) else listOf(Color(0xFF212529), Color(0xFF000000))
                    ),
                    shape = CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = onOrbClick, modifier = Modifier.size(140.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                    Text(text = if (isListening) "🎙️" else if (isThinking) "⚡" else "🤖", fontSize = 45.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = recognizedText, color = Color(0xFF495057), fontSize = 15.sp, textAlign = TextAlign.Center)
        }

        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F5)),
            border = BorderStroke(1.dp, Color(0xFFDC3545).copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(text = adamReply, color = Color(0xFF212529), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}