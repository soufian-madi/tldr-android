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
 * Reads the gateway's live model catalog (`meta/models`) so the picker reflects what the
 * API key can actually call instead of a hardcoded list.
 *
 * Only OpenAI and Anthropic models are kept; other providers (e.g. Gemini) are dropped
 * because [ModelFamily] has no request shape for them. Superseded entries are dropped too:
 * anything carrying a `replacementModelId` (whatever its value) or marked
 * `lifecycle: deprecated` is an old model the gateway still routes but has replaced.
 */
class ModelCatalogService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchModels(): Result<List<AiModel>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.CLAUDE_API_KEY
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("API key not configured"))
        }

        try {
            val request = Request.Builder()
                .url("$GATEWAY/meta/models")
                .addHeader("x-api-key", apiKey)
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
        return catalog?.models.orEmpty()
            .filter { it.id != null && it.replacementModelId == null && it.lifecycle != "deprecated" }
            .mapNotNull { entry ->
                val family = when (entry.provider) {
                    "openai" -> ModelFamily.OPENAI
                    "anthropic" -> ModelFamily.ANTHROPIC
                    else -> return@mapNotNull null
                }
                val canonicalId = entry.id!!
                // Request with the short alias, not the canonical id — the gateway's Bedrock
                // mapping for Anthropic models is keyed on the short form (see [AiModel.id]).
                val requestId = entry.aliases.orEmpty()
                    .firstOrNull { it != canonicalId } ?: canonicalId
                AiModel(
                    id = requestId,
                    displayName = Models.prettyName(canonicalId),
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
        val models: List<CatalogModel>?
    )

    private data class CatalogModel(
        val id: String?,
        val provider: String?,
        val aliases: List<String>?,
        val lifecycle: String?,
        /** Present only on superseded models; its mere presence disqualifies the entry. */
        val replacementModelId: String?
    )
}
