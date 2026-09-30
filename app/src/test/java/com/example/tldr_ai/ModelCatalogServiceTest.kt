package com.example.tldr_ai

import com.example.tldr_ai.data.api.ModelCatalogService
import com.example.tldr_ai.data.model.ModelFamily
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelCatalogServiceTest {
    @Test
    fun parseCatalog_usesNexosIdsAndFiltersUnsupportedEndpointsAndDevelopers() {
        val models = ModelCatalogService().parseCatalog("""
            {"data":[
              {"id":"Claude Sonnet 5","developer":"Anthropic","endpoints":["chat_completion"]},
              {"id":"GPT 6 Luna","name":"GPT 6 Luna","developer":"OpenAI","endpoints":["chat_completion","responses"]},
              {"id":"Whisper 1","developer":"OpenAI","endpoints":["transcription"]},
              {"id":"Gemini 3.5 Flash","developer":"Google","endpoints":["chat_completion"]},
              {"developer":"OpenAI","endpoints":["chat_completion"]},
              {"id":"","developer":"OpenAI","endpoints":["chat_completion"]}
            ]}
        """.trimIndent())
        assertEquals(listOf("GPT 6 Luna", "Claude Sonnet 5"), models.map { it.id })
        assertEquals(listOf(ModelFamily.OPENAI, ModelFamily.ANTHROPIC), models.map { it.family })
        assertEquals("Claude Sonnet 5", models.last().displayName)
        assertEquals(emptyList<String>(), models.last().aliases)
    }

    @Test
    fun parseCatalog_toleratesEmptyAndMissingCatalog() {
        assertEquals(emptyList<Any>(), ModelCatalogService().parseCatalog("""{"data":[]}"""))
        assertEquals(emptyList<Any>(), ModelCatalogService().parseCatalog("""{}"""))
    }
}
