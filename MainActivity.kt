package com.example.iris

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var tts: TextToSpeech
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        startButton = findViewById(R.id.startButton)

        checkPermissions()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                statusTextView.text = getString(R.string.status_listening)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                statusTextView.text = "Error: $error"
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val voiceInput = matches[0]
                    statusTextView.text = "Input: $voiceInput"
                    sendToOpenRouter(voiceInput)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        startButton.setOnClickListener {
            startListening()
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        startForegroundService()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS
        )
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer.startListening(intent)
    }

    private fun startForegroundService() {
        val serviceIntent = Intent(this, IrisForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun sendToOpenRouter(voiceInput: String) {
        statusTextView.text = getString(R.string.status_processing)
        
        val apiKey = "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"
        val url = "https://openrouter.ai/api/v1/chat/completions"
        
        val requestBody = mapOf(
            "model" to "nvidia/nemotron-3-nano-30b-a3b:free",
            "messages" to listOf(
                mapOf("role" to "system", "content" to "You are IRIS. Always respond ONLY in strict JSON format with fields: action, target, value, response."),
                mapOf("role" to "user", "content" to voiceInput)
            )
        )

        val json = gson.toJson(requestBody)
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { statusTextView.text = "API Error: ${e.message}" }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val apiResponse = gson.fromJson(responseBody, OpenRouterResponse::class.java)
                        val content = apiResponse.choices[0].message.content
                        val irisAction = gson.fromJson(content, IrisAction::class.java)
                        
                        runOnUiThread {
                            statusTextView.text = "Response: ${irisAction.response}"
                            tts.speak(irisAction.response, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    } catch (e: Exception) {
                        runOnUiThread { statusTextView.text = "Parse Error: ${e.message}" }
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tts.stop()
        tts.shutdown()
    }

    data class OpenRouterResponse(val choices: List<Choice>)
    data class Choice(val message: Message)
    data class Message(val content: String)
    data class IrisAction(val action: String, val target: String, val value: String?, val response: String)
}
