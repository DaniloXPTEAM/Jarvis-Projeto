package com.javis.assistant.di

import com.javis.assistant.data.remote.api.DeepSeekApiService
import com.javis.assistant.data.remote.api.GroqApiService
    @Named("openrouter")
    fun provideOpenRouterRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton63378f4 (chore: finish Gabi setup)
    fun provideGroqApiService(@Named("groq") retrofit: Retrofit): GroqApiService {
        return retrofit.create(GroqApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDeepSeekApiService(@Named("deepseek") retrofit: Retrofit): DeepSeekApiService {
        return retrofit.create(DeepSeekApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenRouterApiService(@Named("openrouter") retrofit: Retrofit): OpenRouterApiService {
        return retrofit.create(OpenRouterApiService::class.java)
    }
}
