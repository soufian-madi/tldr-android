package com.example.tldr_ai.data.repository

import android.content.Context
import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.ModelFamily
import com.example.tldr_ai.data.model.Models

/**
 * Persists user settings (currently the selected model) in SharedPreferences so the choice
 * is shared between the main screen and the floating card overlay.
 *
 * The whole model is stored — not just its id — so the overlay can build a request without
 * consulting the (asynchronously loaded) model catalog.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedModel(): AiModel {
        val id = prefs.getString(KEY_MODEL, null) ?: return Models.DEFAULT
        val family = prefs.getString(KEY_FAMILY, null)?.let { name ->
            runCatching { ModelFamily.valueOf(name) }.getOrNull()
        }
        // A selection saved before the family was persisted: resolve it against the fallback
        // list, and derive the family from the id for models that aren't in that list.
        if (family == null) {
            return Models.find(Models.FALLBACK, id) ?: AiModel(
                id = id,
                displayName = Models.prettyName(id),
                family = if (id.contains("claude")) ModelFamily.ANTHROPIC else ModelFamily.OPENAI
            )
        }
        return AiModel(
            id = id,
            displayName = prefs.getString(KEY_NAME, null) ?: Models.prettyName(id),
            family = family
        )
    }

    fun setSelectedModel(model: AiModel) {
        prefs.edit()
            .putString(KEY_MODEL, model.id)
            .putString(KEY_NAME, model.displayName)
            .putString(KEY_FAMILY, model.family.name)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "tldr_settings"
        private const val KEY_MODEL = "selected_model"
        private const val KEY_NAME = "selected_model_name"
        private const val KEY_FAMILY = "selected_model_family"
    }
}
