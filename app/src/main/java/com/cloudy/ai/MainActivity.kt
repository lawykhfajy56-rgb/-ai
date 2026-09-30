package com.cloudy.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var promptInput: EditText
    private lateinit var startCloudButton: Button
    private lateinit var voiceButton: ImageButton
    private lateinit var speakButton: Button
    private lateinit var tts: TextToSpeech

    private val voiceResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                promptInput.setText(spoken)
                statusText.text = getString(R.string.status_listening_done)
                speakResponse("سأساعدك في: $spoken")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        promptInput = findViewById(R.id.promptInput)
        startCloudButton = findViewById(R.id.startCloudButton)
        voiceButton = findViewById(R.id.voiceButton)
        speakButton = findViewById(R.id.speakButton)

        tts = TextToSpeech(this) {
            statusText.text = getString(R.string.status_ready)
        }

        startCloudButton.setOnClickListener {
            startCloud()
        }

        voiceButton.setOnClickListener {
            startVoiceRecognition()
        }

        speakButton.setOnClickListener {
            val input = promptInput.text.toString().ifBlank { "مرحبًا كلودي" }
            speakResponse(input)
        }
    }

    private fun startCloud() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = android.net.Uri.parse("package:${packageName}")
            startActivity(intent)
            statusText.text = getString(R.string.status_overlay_needed)
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
            statusText.text = getString(R.string.status_microphone_needed)
            return
        }

        ContextCompat.startForegroundService(Intent(this, FloatingCloudService::class.java))
        statusText.text = getString(R.string.status_cloud_active)
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث بالعربية")
        }
        voiceResultLauncher.launch(intent)
    }

    private fun speakResponse(text: String) {
        statusText.text = getString(R.string.status_speaking)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cloudy_response")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCloud()
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
