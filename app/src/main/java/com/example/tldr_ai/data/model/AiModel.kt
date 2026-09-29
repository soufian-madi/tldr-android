package com.example.tldr_ai.data.model

/**
 * Which gateway backend a model is routed to. Determines the endpoint, request body
 * shape, and whether reasoning text is available in the stream.
 */
enum class ModelFamily { OPENAI, ANTHROPIC }

/**
 * A model the user can pick.
 *
 * [id] is the id sent in the x-model header. This is deliberately the catalog's *short*
 * alias (`claude-haiku-4-5`), not its canonical id (`anthropic-claude-haiku-4-5`): the
 * gateway resolves Anthropic models to Bedrock by the short alias and rejects the canonical
 * form with "No Bedrock mapping for model ...". OpenAI accepts either.
 *
 * [displayName] is shown in the picker, [aliases] are the other ids the gateway accepts for
 * the same model (including the canonical one) and are used to match a saved selection.
 */
data class AiModel(
    val id: String,
    val displayName: String,
    val family: ModelFamily,
    val aliases: List<String> = emptyList()
)

/**
 * Model catalog helpers.
 *
 * The picker list is loaded at runtime from the gateway's `meta/models` endpoint
 * (see ModelCatalogRepository). [FALLBACK] is only used before the first successful
 * fetch, or when the gateway is unreachable and nothing has been cached yet.
 *
 * Note: reasoning text only streams for OpenAI (gpt-*) models; Anthropic models stream
 * the answer only (thinking is redacted by the gateway), so the faint-reasoning phase is
 * empty for Claude.
 */
object Models {
    val FALLBACK = listOf(
        AiModel("gpt-5-mini", "GPT-5 mini", ModelFamily.OPENAI, listOf("openai-gpt-5-mini")),
        AiModel("gpt-5.5", "GPT-5.5", ModelFamily.OPENAI, listOf("openai-gpt-5.5")),
        AiModel("claude-opus-5", "Claude Opus 5", ModelFamily.ANTHROPIC, listOf("anthropic-claude-opus-5")),
        AiModel("claude-sonnet-5", "Claude Sonnet 5", ModelFamily.ANTHROPIC, listOf("anthropic-claude-sonnet-5")),
        AiModel("claude-haiku-4-5", "Claude Haiku 4.5", ModelFamily.ANTHROPIC, listOf("anthropic-claude-haiku-4-5")),
    )

    val DEFAULT = FALLBACK.first()

    /** Matches [id] against canonical ids first, then aliases, so saved legacy ids resolve. */
    fun find(models: List<AiModel>, id: String?): AiModel? {
        if (id == null) return null
        return models.firstOrNull { it.id == id } ?: models.firstOrNull { id in it.aliases }
    }

    /**
     * Picks a sensible selection out of a freshly loaded catalog: the cheapest OpenAI
     * model if one is offered (reasoning streams for those), else the first entry.
     */
    fun preferredDefault(models: List<AiModel>): AiModel = models.firstOrNull {
        it.family == ModelFamily.OPENAI && it.id.contains("mini")
    } ?: models.firstOrNull() ?: DEFAULT

    private val ACRONYMS = mapOf("gpt" to "GPT")
    private val KEEP_LOWERCASE = setOf("mini", "nano")
    private val PROVIDER_PREFIXES = listOf("openai-", "anthropic-", "gemini-", "google-")

    /**
     * Turns a gateway model id into a picker label. Accepts canonical ids and short aliases:
     * `openai-gpt-5.6-sol` and `gpt-5.6-sol` → "GPT-5.6 Sol",
     * `anthropic-claude-opus-4-8` → "Claude Opus 4.8",
     * `anthropic-direct-claude-fable-5` → "Claude Fable 5 (direct)".
     */
    fun prettyName(id: String): String {
        var rest = PROVIDER_PREFIXES.firstOrNull { id.startsWith(it) }
            ?.let { id.removePrefix(it) } ?: id
        val direct = rest.startsWith("direct-")
        if (direct) rest = rest.removePrefix("direct-")

        // Merge adjacent numeric segments into a dotted version: "4", "8" → "4.8".
        val raw = rest.split('-').filter { it.isNotEmpty() }
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < raw.size) {
            var token = raw[i]
            while (i + 1 < raw.size && token.first().isDigit() && raw[i + 1].all(Char::isDigit)) {
                token = "$token.${raw[i + 1]}"
                i++
            }
            tokens.add(token)
            i++
        }

        val words = tokens.map { word ->
            when {
                ACRONYMS.containsKey(word) -> ACRONYMS.getValue(word)
                word in KEEP_LOWERCASE || word.first().isDigit() -> word
                else -> word.replaceFirstChar(Char::uppercaseChar)
            }
        }.toMutableList()

        // "GPT" reads better hyphenated to its version: "GPT-5.6 Sol", not "GPT 5.6 Sol".
        if (words.size > 1 && words[0] == "GPT" && words[1].first().isDigit()) {
            words[0] = "${words[0]}-${words[1]}"
            words.removeAt(1)
        }

        val label = words.joinToString(" ").ifBlank { id }
        return if (direct) "$label (direct)" else label
    }
}
