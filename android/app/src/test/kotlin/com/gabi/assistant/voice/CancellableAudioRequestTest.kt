package com.gabi.assistant.voice

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CancellableAudioRequestTest {
    private fun response(chain: Interceptor.Chain, code: Int, bytes: ByteArray) = Response.Builder()
        .request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("test")
        .body(bytes.toResponseBody("audio/mpeg".toMediaType())).build()
    private fun request() = Request.Builder().url("https://example.invalid/tts").build()

    @Test fun interruptedDownloadCancelsCallAndCannotReturnAudio() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val returned = AtomicBoolean(false)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            response(chain, 200, byteArrayOf(1, 2))
        }.build()
        val call = client.newCall(request())
        try {
            val job = launch(Dispatchers.Default) {
                call.awaitAudioBytes()
                returned.set(true)
            }
            assertTrue(withContext(Dispatchers.IO) { entered.await(3, TimeUnit.SECONDS) })
            job.cancelAndJoin()
            assertTrue(call.isCanceled())
            assertFalse(returned.get())
        } finally {
            release.countDown()
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }
    @Test fun successfulAudioReturnsExactBytes() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { response(it, 200, byteArrayOf(5, 6, 7)) }.build()
        try { assertArrayEquals(byteArrayOf(5, 6, 7), client.newCall(request()).awaitAudioBytes()) }
        finally { client.dispatcher.executorService.shutdown() }
    }
    @Test fun httpFailureDoesNotBecomeSuccessfulPlayback() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { response(it, 401, byteArrayOf(1)) }.build()
        try {
            try { client.newCall(request()).awaitAudioBytes(); fail("HTTP failure was ignored") }
            catch (e: IOException) { assertEquals("ElevenLabs HTTP 401", e.message) }
        } finally { client.dispatcher.executorService.shutdown() }
    }
    @Test fun emptyAudioIsRejected() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { response(it, 200, byteArrayOf()) }.build()
        try {
            try { client.newCall(request()).awaitAudioBytes(); fail("Empty audio was accepted") }
            catch (e: IOException) { assertTrue(e.message!!.contains("vazio")) }
        } finally { client.dispatcher.executorService.shutdown() }
    }
}
