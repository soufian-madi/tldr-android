package com.example.tldr_ai.data.api

import com.example.tldr_ai.BuildConfig
import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.ModelFamily
import com.example.tldr_ai.data.model.Models
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Reads Nexos's live `/models` catalog. The picker keeps OpenAI, Anthropic, and Google models
 * that support chat completions, using the exact model IDs returned by Nexos.
 */
class ModelCatalogService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchModels(): Result<List<AiModel>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.NEXOS_API_KEY
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("API key not configured"))
        }

        try {
            val request = Request.Builder()
                .url("$GATEWAY/models")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("User-Agent", "Mozilla/5.0 TLDR-Android")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Failed to load models: ${response.code}")
                    )
                }
                if (body.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Empty model catalog response"))
                }

                val models = parseCatalog(body)
                if (models.isEmpty()) {
                    Result.failure(Exception("No usable models in catalog"))
                } else {
                    Result.success(models)
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to load models: ${e.message}"))
        }
    }

    internal fun parseCatalog(body: String): List<AiModel> {
        val catalog = gson.fromJson(body, CatalogResponse::class.java)
        return catalog?.data.orEmpty()
            .filter { !it.id.isNullOrBlank() && "chat_completion" in it.endpoints.orEmpty() }
            .mapNotNull { entry ->
                val family = when (entry.developer) {
                    "OpenAI" -> ModelFamily.OPENAI
                    "Anthropic" -> ModelFamily.ANTHROPIC
                    "Google" -> ModelFamily.GOOGLE
                    else -> return@mapNotNull null
                }
                val canonicalId = entry.id!!
                // Nexos accepts its catalog ID directly in the request body.
                val requestId = canonicalId
                AiModel(
                    id = requestId,
                    displayName = entry.name ?: canonicalId,
                    family = family,
                    aliases = (listOf(canonicalId) + entry.aliases.orEmpty())
                        .distinct()
                        .filter { it != requestId }
                )
            }
            // OpenAI first (their reasoning summaries stream), catalog order within a family.
            .sortedBy { it.family != ModelFamily.OPENAI }
    }

    private data class CatalogResponse(
        val data: List<CatalogModel>?
    )

    private data class CatalogModel(
        val id: String?,
        val developer: String?,
        val name: String?,
        val endpoints: List<String>?,
        val aliases: List<String>?,
        val lifecycle: String?,
        /** Present only on superseded models; its mere presence disqualifies the entry. */
        val replacementModelId: String?
    )
}
