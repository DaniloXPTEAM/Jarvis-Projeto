package com.gabi.assistant.skills

import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private interface SpotifyAccountsApi {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun getToken(
        @Header("Authorization") basicAuth: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): Response<SpotifyTokenResponse>
}

private data class SpotifyTokenResponse(val access_token: String, val expires_in: Int)

private interface SpotifyWebApi {
    @GET("v1/search")
    suspend fun search(
        @Header("Authorization") bearer: String,
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 1
    ): Response<SpotifySearchResponse>
}

private data class SpotifySearchResponse(val tracks: SpotifyTracks?)
private data class SpotifyTracks(val items: List<SpotifyTrack>)
private data class SpotifyTrack(val uri: String, val name: String, val artists: List<SpotifyArtist>)
private data class SpotifyArtist(val name: String)

/**
 * Busca de verdade na Web API da Spotify (fluxo "Client Credentials" — só autentica
 * o APP, não a conta pessoal do usuário, não precisa de login nem senha). Resolve um
 * texto livre ("Evidências Chitãozinho e Xororo") pro URI de uma faixa específica
 * (`spotify:track:<id>`) — esse formato É documentado oficialmente pela Spotify e
 * abre/toca a faixa direto, diferente do link de busca (`spotify:search:` ou
 * `open.spotify.com/search/...`) que só abre a TELA de resultados, sem tocar
 * sozinho. Sem Client ID/Secret configurados em Ajustes, `resolveTrackUri` devolve
 * null e quem chama cai pro link de busca (comportamento de antes, sem quebrar nada).
 */
@Singleton
class SpotifyClient @Inject constructor() {

    var clientId: String = ""
    var clientSecret: String = ""

    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0L

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val accountsApi: SpotifyAccountsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://accounts.spotify.com/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyAccountsApi::class.java)
    }

    private val webApi: SpotifyWebApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.spotify.com/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyWebApi::class.java)
    }

    private suspend fun getAccessToken(): String? {
        val now = System.currentTimeMillis()
        cachedToken?.let { token -> if (now < tokenExpiresAt) return token }
        if (clientId.isBlank() || clientSecret.isBlank()) {
            Log.d(TAG, "getAccessToken: clientId/secret em branco, pulando")
            return null
        }
        return try {
            val basic = "Basic " + Base64.encodeToString("$clientId:$clientSecret".toByteArray(), Base64.NO_WRAP)
            val resp = accountsApi.getToken(basic)
            if (!resp.isSuccessful) {
                Log.w(TAG, "Token Spotify falhou: ${resp.code()} ${resp.errorBody()?.string()}")
                return null
            }
            val body = resp.body() ?: return null
            cachedToken = body.access_token
            // Tira 60s de folga do vencimento real, pra nunca usar um token expirado por pouco.
            tokenExpiresAt = now + (body.expires_in - 60).coerceAtLeast(0) * 1000L
            Log.d(TAG, "Token Spotify obtido com sucesso, expira em ${body.expires_in}s")
            body.access_token
        } catch (e: Exception) {
            Log.e(TAG, "Erro pegando token Spotify: ${e.message}", e)
            null
        }
    }

    /** Resolve [query] pro URI de uma faixa (`spotify:track:...`) via busca real; null se não achar ou sem chave configurada. */
    suspend fun resolveTrackUri(query: String): String? {
        val token = getAccessToken() ?: return null
        return try {
            val resp = webApi.search("Bearer $token", query)
            if (!resp.isSuccessful) {
                Log.w(TAG, "Busca Spotify falhou: ${resp.code()} ${resp.errorBody()?.string()}")
                return null
            }
            val uri = resp.body()?.tracks?.items?.firstOrNull()?.uri
            Log.d(TAG, "Busca Spotify por \"$query\" -> $uri")
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Erro buscando no Spotify: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "SpotifyClient"
    }
}
