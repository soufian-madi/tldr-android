package com.example.tldr_ai.data.model

/**
 * Which gateway backend a model is routed to. Determines the endpoint, request body
 * shape, and whether reasoning text is available in the stream.
 */
enum class ModelFamily { OPENAI, ANTHROPIC }

/**
 * A model the user can pick. [id] is the gateway model id (sent via the x-model header),
 * [displayName] is shown in the picker.
 */
data class AiModel(
    val id: String,
    val displayName: String,
    val family: ModelFamily
)

/**
 * Curated set of models exposed in the picker.
 *
 * Note: reasoning text only streams for OpenAI (gpt-*) models; Anthropic models stream
 * the answer only (thinking is redacted by the gateway), so the faint-reasoning phase is
 * empty for Claude. The default is a GPT model so reasoning is visible out of the box.
 */
object Models {
    val ALL = listOf(
        AiModel("gpt-5.5", "GPT-5.5", ModelFamily.OPENAI),
        AiModel("gpt-5", "GPT-5", ModelFamily.OPENAI),
        AiModel("gpt-5-mini", "GPT-5 mini", ModelFamily.OPENAI),
        AiModel("gpt-5-nano", "GPT-5 nano", ModelFamily.OPENAI),
        AiModel("claude-opus-4-8", "Claude Opus 4.8", ModelFamily.ANTHROPIC),
        AiModel("claude-sonnet-4-6", "Claude Sonnet 4.6", ModelFamily.ANTHROPIC),
        AiModel("claude-haiku-4-5", "Claude Haiku 4.5", ModelFamily.ANTHROPIC),
    )

    val DEFAULT = ALL.first { it.id == "gpt-5-mini" }

    fun byId(id: String?): AiModel = ALL.firstOrNull { it.id == id } ?: DEFAULT
}
