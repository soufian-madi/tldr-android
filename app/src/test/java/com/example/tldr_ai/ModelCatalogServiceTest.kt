package com.example.tldr_ai

import com.example.tldr_ai.data.api.ModelCatalogService
import com.example.tldr_ai.data.model.ModelFamily
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelCatalogServiceTest {
    @Test
    fun parseCatalog_keepsOnlyGpt6PlusAndClaude55() {
        val models = ModelCatalogService().parseCatalog("""
            {"data":[
              {"id":"anthropic/claude-sonnet-5-5","owned_by":"anthropic","model_name":"claude-sonnet-5-5"},
              {"id":"anthropic/claude-haiku-5-5","owned_by":"anthropic","model_name":"claude-haiku-5-5"},
              {"id":"anthropic/claude-opus-5","owned_by":"anthropic","model_name":"claude-opus-5"},
              {"id":"anthropic/claude-opus-4-8","owned_by":"anthropic","model_name":"claude-opus-4-8"},
              {"id":"anthropic/claude-sonnet-latest","owned_by":"anthropic","model_name":"claude-sonnet-5-5","alias_of":"anthropic/claude-sonnet-5-5"},
              {"id":"openai/gpt-6.1-sol","owned_by":"openai","model_name":"gpt-6.1-sol"},
              {"id":"openai/gpt-6-luna","owned_by":"openai","model_name":"gpt-6-luna"},
              {"id":"openai/gpt-5.6-sol","owned_by":"openai","model_name":"gpt-5.6-sol"},
              {"id":"openai/gpt-5.5","owned_by":"openai","model_name":"gpt-5.5"},
              {"id":"openai/gpt-latest","owned_by":"openai","model_name":"gpt-6-astra","alias_of":"openai/gpt-6-astra"},
              {"id":"google/gemini-3.6-flash","owned_by":"google","model_name":"gemini-3.6-flash"}
            ]}
        """.trimIndent())
        assertEquals(
            listOf(
                "openai/gpt-6.1-sol", "openai/gpt-6-luna",
                "anthropic/claude-sonnet-5-5", "anthropic/claude-haiku-5-5"
            ),
            models.map { it.id }
        )
        assertEquals(
            listOf(ModelFamily.OPENAI, ModelFamily.OPENAI, ModelFamily.ANTHROPIC, ModelFamily.ANTHROPIC),
            models.map { it.family }
        )
    }

    @Test
    fun parseCatalog_toleratesEmptyAndMissingCatalog() {
        assertEquals(emptyList<Any>(), ModelCatalogService().parseCatalog("""{"data":[]}"""))
        assertEquals(emptyList<Any>(), ModelCatalogService().parseCatalog("""{}"""))
    }
}
