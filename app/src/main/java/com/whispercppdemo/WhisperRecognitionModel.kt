package com.whispercppdemo

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import android.content.ComponentName
import android.content.pm.PackageManager

import com.whispercpp.whisper.WhisperContext;


class WhisperRecognitionModel(
    private val context: Context,
    private val serviceClass: Class<*>
) : ViewModel() {
    var canTranscribe = false
    var dataLog = ""

    private val service = ComponentName(context, serviceClass);
    private val modelsPath = File(context.filesDir, "models")
    private var whisperContext: WhisperContext? = null

    init {
        viewModelScope.launch {
            loadData()
        }
    }

    private suspend fun loadData() {
        printMessage("Loading data...\n")
        try {
            copyAssets()
            loadBaseModel()
            canTranscribe = true
        } catch (e: Exception) {
            Log.w(LOG_TAG, e)
            printMessage("${e.localizedMessage}\n")
        }
    }

    private suspend fun printMessage(msg: String) = withContext(Dispatchers.Main) {
        dataLog += msg
        println(msg)
    }

    private suspend fun copyAssets() = withContext(Dispatchers.IO) {
        modelsPath.mkdirs()
        printMessage("All data copied to working directory.\n")
    }

    private suspend fun loadBaseModel() = withContext(Dispatchers.IO) {
        printMessage("Loading model...\n")
        //val models = context.assets.list("models/")
        //if (models != null) {
        context.packageManager.getServiceInfo(service, PackageManager.GET_META_DATA).apply {
            val model = metaData.getString("whisper.model") ?: "ggml-tiny.bin"
            whisperContext =
//                WhisperContext.createContextFromAsset(context.assets, "models/" + model)
//            printMessage("Loaded model ${model}.\n")
                WhisperContext.createContextFromAsset(context.assets, "models/" + model)
            printMessage("Loaded model ${model}.\n")
        }
    }

    fun transcribe(bytes: ByteArray, onResultListener: (String) -> Unit) = viewModelScope.launch {
        if (!canTranscribe) {
            onResultListener("[busy!]")
        }
        printMessage("Reading wave bytes...\n")
        val data = decodeBytes(bytes)
        printMessage("Transcribing data...\n")

//        val text = whisperContext?.transcribeData(data, false)
//        printMessage("Done transcribing data.\n")
//        text?.let { onResultListener(it) } ?: onResultListener("[no test!]")

        context.packageManager.getServiceInfo(service, PackageManager.GET_META_DATA).apply {
            val language = metaData.getString("whisper.language") ?: "auto"
            val text = whisperContext?.transcribeData(language, data)
            printMessage("Done: $language $text\n")
            text?.let { onResultListener(it) } ?: onResultListener("[no test!]")
        }
    }

    override fun onCleared() {
        runBlocking {
            whisperContext?.release()
            whisperContext = null
        }
    }

    companion object {
        private const val LOG_TAG = "WhisperRecognitionModel"
    }
}

