package com.example.core.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.core.ai.GroqApiClient
import com.example.core.model.AgentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val groqApiClient: GroqApiClient
) {
    private var textToSpeech: TextToSpeech? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null

    private val _voiceState = MutableStateFlow(AgentStatus.IDLE)
    val voiceState: StateFlow<AgentStatus> = _voiceState.asStateFlow()

    private val _transcript = MutableStateFlow<String?>(null)
    val transcript: StateFlow<String?> = _transcript.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    init {
        initializeTts()
    }

    private fun initializeTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = AgentStatus.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        _voiceState.value = AgentStatus.IDLE
                    }

                    override fun onError(utteranceId: String?) {
                        _voiceState.value = AgentStatus.IDLE
                    }
                })
                _isTtsReady.value = true
            }
        }
    }

    fun startListening(): Boolean {
        try {
            stopSpeaking()
            val outputFile = File(context.cacheDir, "aeris_voice_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            _voiceState.value = AgentStatus.LISTENING
            return true
        } catch (_: Exception) {
            _voiceState.value = AgentStatus.IDLE
            return false
        }
    }

    suspend fun stopListeningAndTranscribe(): String? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _voiceState.value = AgentStatus.THINKING

            val file = currentRecordingFile
            if (file != null && file.exists() && file.length() > 0) {
                val text = groqApiClient.transcribeAudio(file)
                _transcript.value = text
                _voiceState.value = AgentStatus.IDLE
                file.delete()
                return text
            }
        } catch (_: Exception) {
            mediaRecorder = null
            _voiceState.value = AgentStatus.IDLE
        }
        _voiceState.value = AgentStatus.IDLE
        return null
    }

    fun cancelListening() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        currentRecordingFile?.delete()
        _voiceState.value = AgentStatus.IDLE
    }

    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (textToSpeech != null && _isTtsReady.value) {
            _voiceState.value = AgentStatus.SPEAKING
            textToSpeech?.speak(
                text.take(500), // Clean verbal response
                TextToSpeech.QUEUE_FLUSH,
                null,
                "utterance_${System.currentTimeMillis()}"
            )
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        if (_voiceState.value == AgentStatus.SPEAKING) {
            _voiceState.value = AgentStatus.IDLE
        }
    }

    fun destroy() {
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null
        cancelListening()
    }
}
