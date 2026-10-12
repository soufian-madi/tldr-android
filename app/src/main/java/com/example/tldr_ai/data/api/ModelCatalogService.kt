package com.example.tldr_ai.data.api

import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.ModelFamily
import com.example.tldr_ai.data.model.Models
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Reads Eden AI's public `/v3/models` catalog and keeps only the models this app offers:
 * OpenAI GPT 6 or newer, and the Anthropic Claude 5.5 family (Haiku, Sonnet, Opus).
 * Stable `-latest` aliases are skipped so each model appears once, under its concrete id.
 */
class ModelCatalogService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchModels(): Result<List<AiModel>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$GATEWAY/models")
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
            .filter { !it.id.isNullOrBlank() && it.aliasOf == null }
            .mapNotNull { entry ->
                val name = entry.modelName ?: return@mapNotNull null
                val family = when (entry.ownedBy) {
                    "openai" -> if (GPT_6_PLUS.matches(name)) ModelFamily.OPENAI else null
                    "anthropic" -> if (CLAUDE_5_5.matches(name)) ModelFamily.ANTHROPIC else null
                    else -> null
                } ?: return@mapNotNull null
                AiModel(
                    id = entry.id!!,
                    displayName = Models.prettyName(name),
                    family = family
                )
            }
            .distinctBy { it.id }
            // OpenAI first (their reasoning summaries stream), catalog order within a family.
            .sortedBy { it.family != ModelFamily.OPENAI }
    }

    private data class CatalogResponse(
        val data: List<CatalogModel>?
    )

    private data class CatalogModel(
        val id: String?,
        @SerializedName("owned_by") val ownedBy: String?,
        @SerializedName("model_name") val modelName: String?,
        @SerializedName("alias_of") val aliasOf: String?
    )

    private companion object {
        val GPT_6_PLUS = Regex("""gpt-([6-9]|\d{2,})(\.\d+)?-[a-z]+""")
        val CLAUDE_5_5 = Regex("""claude-(haiku|sonnet|opus)-5-5""")
    }
}
