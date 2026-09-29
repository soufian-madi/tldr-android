package com.example.tldr_ai.data.repository

import android.content.Context
import com.example.tldr_ai.data.model.HistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "tldr_history"
        private const val KEY_ITEMS = "history_items"
        private const val MAX_ITEMS = 50
    }

    fun getItems(): List<HistoryItem> {
        val json = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addItem(item: HistoryItem) {
        val items = getItems().toMutableList()
        items.add(0, item)
        if (items.size > MAX_ITEMS) {
            items.subList(MAX_ITEMS, items.size).clear()
        }
        prefs.edit().putString(KEY_ITEMS, gson.toJson(items)).apply()
    }

    fun removeItem(id: String) {
        val items = getItems().filterNot { it.id == id }
        prefs.edit().putString(KEY_ITEMS, gson.toJson(items)).apply()
    }

    fun clearAll() {
        prefs.edit().remove(KEY_ITEMS).apply()
    }
}
