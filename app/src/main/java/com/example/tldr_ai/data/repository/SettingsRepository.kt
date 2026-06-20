package com.example.tldr_ai.data.repository

import android.content.Context
import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.Models

/**
 * Persists user settings (currently the selected model) in SharedPreferences so the choice
 * is shared between the main screen and the floating card overlay.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedModel(): AiModel = Models.byId(prefs.getString(KEY_MODEL, null))

    fun setSelectedModel(model: AiModel) {
        prefs.edit().putString(KEY_MODEL, model.id).apply()
    }

    companion object {
        private const val PREFS_NAME = "tldr_settings"
        private const val KEY_MODEL = "selected_model"
    }
}
