package com.cloudy.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloudy.ai.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatRepository: ChatRepository
    private lateinit var aiService: AIService
    private lateinit var tts: TextToSpeech
    private var isTTSReady = false

    private val voiceResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                binding.promptInput.setText(spoken)
                binding.statusText.text = getString(R.string.status_listening_done)
                sendMessage(spoken)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            binding.statusText.text = getString(R.string.status_ready)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatRepository = ChatRepository(this)
        aiService = AIService(this)
        chatAdapter = ChatAdapter()

        setupUI()
        setupTTS()
        loadChatHistory()
        checkPermissions()
    }

    private fun setupUI() {
        // RecyclerView setup
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        // Button listeners
        binding.startCloudButton.setOnClickListener {
            startCloud()
        }

        binding.voiceButton.setOnClickListener {
            startVoiceRecognition()
        }

        binding.sendButton.setOnClickListener {
            val input = binding.promptInput.text.toString().trim()
            if (input.isNotEmpty()) {
                sendMessage(input)
                binding.promptInput.setText("")
            } else {
                Toast.makeText(this, "الرجاء كتابة رسالة", Toast.LENGTH_SHORT).show()
            }
        }

        binding.speakButton.setOnClickListener {
            val input = binding.promptInput.text.toString().ifBlank { "مرحبا كلودي" }
            if (isTTSReady) {
                speakResponse(input)
            }
        }

        binding.clearChatButton.setOnClickListener {
            chatRepository.clearMessages()
            chatAdapter.submitList(emptyList())
            binding.statusText.text = "تم مسح المحادثة"
        }
    }

    private fun setupTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTTSReady = true
                tts.language = java.util.Locale("ar", "SA")
                binding.statusText.text = getString(R.string.status_ready)
            }
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        )
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions)
        }
    }

    private fun sendMessage(userInput: String) {
        val userMessage = Message(text = userInput, isFromUser = true)
        chatRepository.addMessage(userMessage)
        chatAdapter.addMessage(userMessage)
        binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        binding.statusText.text = "جاري المعالجة..."

        // Get AI response
        aiService.generateResponse(userInput) { response ->
            val aiMessage = Message(text = response, isFromUser = false)
            chatRepository.addMessage(aiMessage)
            chatAdapter.addMessage(aiMessage)
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            binding.statusText.text = "جاهز"
            chatRepository.saveMessages()

            // Auto-speak the response
            if (isTTSReady) {
                speakResponse(response)
            }
        }
    }

    private fun startCloud() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = android.net.Uri.parse("package:${packageName}")
            startActivity(intent)
            binding.statusText.text = getString(R.string.status_overlay_needed)
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            binding.statusText.text = getString(R.string.status_microphone_needed)
            return
        }

        ContextCompat.startForegroundService(Intent(this, FloatingCloudService::class.java))
        binding.statusText.text = getString(R.string.status_cloud_active)
        Toast.makeText(this, "السحابة العائمة مفعلة", Toast.LENGTH_SHORT).show()
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث بالعربية...")
        }
        voiceResultLauncher.launch(intent)
    }

    private fun speakResponse(text: String) {
        binding.statusText.text = getString(R.string.status_speaking)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cloudy_response")
    }

    private fun loadChatHistory() {
        chatRepository.loadMessages()
        chatAdapter.submitList(chatRepository.getMessages())
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        aiService.destroy()
        super.onDestroy()
    }
}
