package com.example.tldr_ai

import com.example.tldr_ai.data.api.ModelCatalogService
import com.example.tldr_ai.data.model.ModelFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogServiceTest {

    /** Trimmed but structurally faithful sample of a `meta/models` response. */
    private val catalogJson = """
    {
      "api": "ai-gateway",
      "selection": { "header": "x-model", "queryParameter": "model", "required": true },
      "models": [
        { "id": "openai-gpt-5.6-sol", "aliases": ["gpt-5.6-sol", "openai-gpt-5.6-sol"],
          "provider": "openai", "backend": "primary" },
        { "id": "openai-gpt-5-mini", "aliases": ["gpt-5-mini", "openai-gpt-5-mini"],
          "provider": "openai", "backend": "primary" },
        { "id": "anthropic-claude-opus-4-7", "aliases": ["claude-opus-4-7"],
          "provider": "anthropic", "backend": "thirdparty",
          "lifecycle": "deprecated", "replacementModelId": "anthropic-claude-opus-4-8" },
        { "id": "openai-gpt-5.3", "aliases": ["gpt-5.3"], "provider": "openai",
          "backend": "primary", "replacementModelId": "openai-gpt-5.4" },
        { "id": "openai-gpt-5.2", "aliases": ["gpt-5.2"], "provider": "openai",
          "backend": "primary", "replacementModelId": "" },
        { "id": "anthropic-claude-haiku-4-5", "aliases": ["claude-haiku-4-5", "anthropic-claude-haiku-4-5"],
          "provider": "anthropic", "backend": "thirdparty" },
        { "id": "anthropic-direct-claude-fable-5", "aliases": ["direct-claude-fable-5", "anthropic-direct-claude-fable-5"],
          "provider": "anthropic", "backend": "thirdparty", "dataResidency": "non-eu" },
        { "id": "gemini-2.5-flash", "aliases": ["gemini-2.5-flash"],
          "provider": "gemini", "backend": "gemini" }
      ]
    }
    """.trimIndent()

    @Test
    fun parseCatalog_keepsOnlyActiveOpenAiAndAnthropicModels() {
        val models = ModelCatalogService().parseCatalog(catalogJson)

        assertEquals(
            listOf("gpt-5.6-sol", "gpt-5-mini", "claude-haiku-4-5", "direct-claude-fable-5"),
            models.map { it.id }
        )
        // Gemini has no request shape, the 4-7 entry is superseded — both dropped.
        assertTrue(models.none { it.id.contains("gemini") || it.id.contains("4-7") })
    }

    @Test
    fun parseCatalog_dropsAnyModelCarryingAReplacementId() {
        val models = ModelCatalogService().parseCatalog(catalogJson)

        // gpt-5.3 names a replacement without being flagged deprecated, and gpt-5.2 carries an
        // empty replacement id: the attribute's presence alone disqualifies both.
        assertTrue(models.none { it.id == "gpt-5.3" || it.id == "gpt-5.2" })
    }

    @Test
    fun parseCatalog_usesShortAliasAsRequestIdAndKeepsCanonicalAsAlias() {
        val models = ModelCatalogService().parseCatalog(catalogJson)
        val haiku = models.first { it.displayName == "Claude Haiku 4.5" }

        // The gateway rejects the canonical id in x-model ("No Bedrock mapping"), so the
        // short alias must be the id we send.
        assertEquals("claude-haiku-4-5", haiku.id)
        assertEquals(listOf("anthropic-claude-haiku-4-5"), haiku.aliases)
        assertEquals(ModelFamily.ANTHROPIC, haiku.family)
    }

    @Test
    fun parseCatalog_ordersOpenAiFirstThenCatalogOrder() {
        val models = ModelCatalogService().parseCatalog(catalogJson)

        assertEquals(
            listOf(ModelFamily.OPENAI, ModelFamily.OPENAI, ModelFamily.ANTHROPIC, ModelFamily.ANTHROPIC),
            models.map { it.family }
        )
        assertEquals("Claude Fable 5 (direct)", models.last().displayName)
    }

    @Test
    fun parseCatalog_toleratesEmptyAndMalformedPayloads() {
        assertEquals(emptyList<Any>(), ModelCatalogService().parseCatalog("""{"models":[]}"""))
        assertEquals(emptyList<Any>(), ModelCatalogService().parseCatalog("""{"api":"ai-gateway"}"""))
    }
}
