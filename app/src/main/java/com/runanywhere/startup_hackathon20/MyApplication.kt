package com.runanywhere.startup_hackathon20

import android.app.Application
import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.data.models.SDKEnvironment
import com.runanywhere.sdk.public.extensions.addModelFromURL
import com.runanywhere.sdk.llm.llamacpp.LlamaCppServiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SDK asynchronously
        GlobalScope.launch(Dispatchers.IO) {
            initializeSDK()
        }
    }

    private suspend fun initializeSDK() {
        try {
            // BACK TO DEVELOPMENT MODE (No crash)
            RunAnywhere.initialize(
                context = this@MyApplication,
                apiKey = "dev-key",
                environment = SDKEnvironment.DEVELOPMENT
            )

            LlamaCppServiceProvider.register()

            // WE WILL REGISTER MODELS MANUALLY BELOW
            registerModels()

            RunAnywhere.scanForDownloadedModels()
            Log.i("MyApp", "SDK initialized successfully (Dev Mode)")

        } catch (e: Exception) {
            Log.e("MyApp", "SDK initialization failed: ${e.message}")
        }
    }

    private suspend fun registerModels() {
        // Medium-sized model
        addModelFromURL(
            url = "https://huggingface.co/Triangle104/Qwen2.5-0.5B-Instruct-Q6_K-GGUF/resolve/main/qwen2.5-0.5b-instruct-q6_k.gguf",
            name = "Qwen 2.5 0.5B Instruct Q6_K",
            type = "LLM"
        )

        // Phi-3-Mini
        addModelFromURL(
            url = "https://huggingface.co/bartowski/Phi-3-mini-128k-instruct-GGUF/resolve/main/Phi-3-mini-128k-instruct-Q4_K_M.gguf",
            name = "Phi-3-Mini-128k-Instruct-Q4_K_M",
            type = "LLM"
        )
    }
}