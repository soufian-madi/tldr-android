package com.example.tldr_ai

import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.ModelFamily
import com.example.tldr_ai.data.model.Models
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {

    @Test
    fun prettyName_labelsGatewayIds() {
        assertEquals("GPT-5.4", Models.prettyName("openai-gpt-5.4"))
        assertEquals("GPT-5.6 Sol", Models.prettyName("openai-gpt-5.6-sol"))
        assertEquals("GPT-5 mini", Models.prettyName("openai-gpt-5-mini"))
        assertEquals("Claude Opus 5", Models.prettyName("anthropic-claude-opus-5"))
        assertEquals("Claude Opus 4.8", Models.prettyName("anthropic-claude-opus-4-8"))
        assertEquals("Claude Haiku 4.5", Models.prettyName("anthropic-claude-haiku-4-5"))
        assertEquals("Claude Fable 5 (direct)", Models.prettyName("anthropic-direct-claude-fable-5"))
        assertEquals(
            "Claude Sonnet 4.6 (direct)",
            Models.prettyName("anthropic-direct-claude-sonnet-4-6")
        )
    }

    @Test
    fun prettyName_acceptsShortAliases() {
        assertEquals("GPT-5.6 Sol", Models.prettyName("gpt-5.6-sol"))
        assertEquals("GPT-5 mini", Models.prettyName("gpt-5-mini"))
        assertEquals("Claude Haiku 4.5", Models.prettyName("claude-haiku-4-5"))
        assertEquals("Claude Fable 5 (direct)", Models.prettyName("direct-claude-fable-5"))
    }

    @Test
    fun find_matchesCanonicalIdThenAliases() {
        val catalog = listOf(
            AiModel("gpt-5-mini", "GPT-5 mini", ModelFamily.OPENAI, listOf("openai-gpt-5-mini")),
            AiModel("claude-opus-5", "Claude Opus 5", ModelFamily.ANTHROPIC, listOf("anthropic-claude-opus-5"))
        )

        assertEquals(catalog[0], Models.find(catalog, "gpt-5-mini"))
        assertEquals(catalog[1], Models.find(catalog, "anthropic-claude-opus-5"))
        // A model that is no longer in the catalog resolves to nothing.
        assertEquals(null, Models.find(catalog, "gpt-5-nano"))
        assertEquals(null, Models.find(catalog, null))
    }

    @Test
    fun preferredDefault_prefersCheapOpenAiModel() {
        val catalog = listOf(
            AiModel("claude-opus-5", "Claude Opus 5", ModelFamily.ANTHROPIC),
            AiModel("gpt-5.5", "GPT-5.5", ModelFamily.OPENAI),
            AiModel("gpt-5-mini", "GPT-5 mini", ModelFamily.OPENAI)
        )

        assertEquals(catalog[2], Models.preferredDefault(catalog))
        assertEquals(catalog[0], Models.preferredDefault(catalog.take(1)))
        assertEquals(Models.DEFAULT, Models.preferredDefault(emptyList()))
    }
}
