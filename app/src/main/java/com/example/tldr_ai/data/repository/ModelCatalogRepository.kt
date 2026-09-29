package com.example.tldr_ai.data.repository

import android.content.Context
import com.example.tldr_ai.data.api.ModelCatalogService
import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.Models
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Supplies the model list for the picker: the last catalog fetched from the gateway, kept in
 * SharedPreferences so the picker is populated instantly (and offline) on the next launch,
 * refreshed in the background via [refresh].
 */
class ModelCatalogRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val service = ModelCatalogService()
    private val gson = Gson()

    /** Cached catalog, or the bundled fallback list if nothing has been fetched yet. */
    fun getModels(): List<AiModel> = readCache() ?: Models.FALLBACK

    /** Fetches the live catalog and caches it on success. */
    suspend fun refresh(): Result<List<AiModel>> =
        service.fetchModels().onSuccess { models ->
            prefs.edit().putString(KEY_CATALOG, gson.toJson(models)).apply()
        }

    private fun readCache(): List<AiModel>? {
        val json = prefs.getString(KEY_CATALOG, null) ?: return null
        return try {
            val type = object : TypeToken<List<AiModel>>() {}.type
            gson.fromJson<List<AiModel>>(json, type)?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "tldr_model_catalog"
        private const val KEY_CATALOG = "catalog"
    }
}
