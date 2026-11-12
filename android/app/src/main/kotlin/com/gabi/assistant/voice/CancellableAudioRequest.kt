package com.gabi.assistant.voice

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Cancellation closes an in-flight HTTP request, including response-body reads. */
internal suspend fun Call.awaitAudioBytes(): ByteArray = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (cont.isActive) cont.resumeWithException(e)
        }
        override fun onResponse(call: Call, response: Response) {
            try {
                val bytes = response.use {
                    if (!it.isSuccessful) throw IOException("ElevenLabs HTTP ${it.code}")
                    it.body?.bytes()?.takeIf { data -> data.isNotEmpty() }
                        ?: throw IOException("ElevenLabs devolveu áudio vazio")
                }
                if (cont.isActive) cont.resume(bytes)
            } catch (e: Exception) {
                if (cont.isActive) cont.resumeWithException(e)
            }
        }
    })
}
