package com.javis.assistant.skills

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agente de Clima — Open-Meteo (grátis, sem chave de API).
 * Tudo em pt-BR. Local padrão: Fortaleza/CE.
 */
interface OpenMeteoApi {
    @GET("forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String =
            "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
        @Query("temperature_unit") unit: String = "celsius",
        @Query("timezone") tz: String = "America/Fortaleza"
    ): Response<WeatherResponse>
}

data class WeatherResponse(val current: CurrentWeather? = null)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double = 0.0,
    @SerializedName("apparent_temperature") val feelsLike: Double = 0.0,
    @SerializedName("relative_humidity_2m") val humidity: Int = 0,
    @SerializedName("weather_code") val code: Int = 0,
    @SerializedName("wind_speed_10m") val windSpeed: Double = 0.0
)

@Singleton
class WeatherAgent @Inject constructor() {

    // Padrão: Fortaleza/CE (local do usuário)
    private val defaultLat = -3.7319
    private val defaultLon = -38.5267
    private val defaultName = "Fortaleza"

    private val api: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/v1/")
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApi::class.java)
    }

    /** cidades brasileiras comuns — chave sem acento/minúscula */
    private val cities = mapOf(
        "fortaleza" to Triple(-3.7319, -38.5267, "Fortaleza"),
        "sao paulo" to Triple(-23.5505, -46.6333, "São Paulo"),
        "rio de janeiro" to Triple(-22.9068, -43.1729, "Rio de Janeiro"),
        "rio" to Triple(-22.9068, -43.1729, "Rio de Janeiro"),
        "brasilia" to Triple(-15.7939, -47.8828, "Brasília"),
        "salvador" to Triple(-12.9714, -38.5014, "Salvador"),
        "recife" to Triple(-8.0476, -34.8770, "Recife"),
        "belo horizonte" to Triple(-19.9167, -43.9345, "Belo Horizonte"),
        "curitiba" to Triple(-25.4284, -49.2733, "Curitiba"),
        "porto alegre" to Triple(-30.0346, -51.2177, "Porto Alegre"),
        "manaus" to Triple(-3.1190, -60.0217, "Manaus"),
        "belem" to Triple(-1.4558, -48.4902, "Belém"),
        "goiania" to Triple(-16.6869, -49.2648, "Goiânia"),
        "natal" to Triple(-5.7945, -35.2110, "Natal"),
        "maceio" to Triple(-9.6498, -35.7089, "Maceió"),
        "joao pessoa" to Triple(-7.1195, -34.8450, "João Pessoa"),
        "teresina" to Triple(-5.0892, -42.8019, "Teresina"),
        "aracaju" to Triple(-10.9472, -37.0731, "Aracaju"),
        "campinas" to Triple(-22.9099, -47.0626, "Campinas"),
        "vitoria" to Triple(-20.3155, -40.3128, "Vitória"),
        "ceara" to Triple(-5.4984, -39.3206, "Ceará"),
        "sao goncalo" to Triple(-7.2332, -34.9662, "São Gonçalo do Amarante")
    )

    private fun normalize(s: String): String =
        java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")

    private fun resolveCity(query: String?): Triple<Double, Double, String> {
        if (query.isNullOrBlank()) return Triple(defaultLat, defaultLon, defaultName)
        val q = normalize(query)
        // "clima em sao paulo" → pega o que vem depois de em/de/para
        val cleaned = q.replace(Regex(".*(em|de|para|do|da)\\s+"), "").trim()
        cities.entries.firstOrNull { cleaned.contains(it.key) }?.let { return it.value }
        cities.entries.firstOrNull { q.contains(it.key) }?.let { return it.value }
        return Triple(defaultLat, defaultLon, query.trim().replaceFirstChar { it.uppercase() })
    }

    suspend fun getWeatherResponse(cityQuery: String? = null): String {
        val (lat, lon, name) = resolveCity(cityQuery)
        return try {
            val resp = api.getForecast(lat, lon)
            if (!resp.isSuccessful) return "Não consegui pegar o clima agora. Tenta daqui a pouco."
            val c = resp.body()?.current ?: return "Sem dados de clima no momento."
            val temp = c.temperature.toInt()
            val sens = c.feelsLike.toInt()
            val desc = codeToText(c.code)
            val feel = when {
                temp >= 35 -> "Tá bem quente, hein! Bebe bastante água."
                temp >= 28 -> "Está quente."
                temp >= 20 -> "Clima agradável."
                temp >= 12 -> "Tá fresquinho."
                else -> "Tá frio, pega um casaco!"
            }
            "Agora em $name: $temp°C, $desc. Sensação térmica de $sens°C. Umidade de ${c.humidity}%" +
                " e vento a ${c.windSpeed.toInt()} km/h. $feel"
        } catch (e: Exception) {
            Log.e("WeatherAgent", "erro clima: ${e.message}")
            "Não consegui acessar o clima. Verifica sua conexão com a internet."
        }
    }

    private fun codeToText(code: Int): String = when (code) {
        0 -> "céu limpo"
        1 -> "quase sem nuvens"
        2 -> "parcialmente nublado"
        3 -> "nublado"
        45, 48 -> "com neblina"
        51, 53, 55 -> "garoa leve"
        56, 57 -> "garoa congelante"
        61 -> "chuva fraca"
        63 -> "chuva"
        65 -> "chuva forte"
        66, 67 -> "chuva congelante"
        71, 73 -> "neve fraca"
        75 -> "neve"
        77 -> "grãos de neve"
        80 -> "pancadas de chuva"
        81 -> "pancadas de chuva"
        82 -> "pancadas fortes de chuva"
        85, 86 -> "pancadas de neve"
        95 -> "tempestade"
        96, 99 -> "tempestade com granizo"
        else -> "tempo variável"
    }
}
