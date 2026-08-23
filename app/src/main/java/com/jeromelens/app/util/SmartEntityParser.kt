package com.jeromelens.app.util

import java.util.regex.Pattern

enum class EntityType {
    URL,
    EMAIL,
    PHONE,
    CODE_BLOCK,
    ADDRESS_LIKE,
    PLAIN
}

data class DetectedEntity(
    val type: EntityType,
    val value: String,
    val start: Int,
    val end: Int,
    val label: String = value
)

/**
 * On-device smart parser for common actionable entities.
 * No network, pure regex + heuristics.
 */
object SmartEntityParser {

    private val URL_PATTERN = Pattern.compile(
        "(?i)\\b((?:https?://|www\\.)[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)\\b"
    )

    private val EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    )

    private val PHONE_PATTERN = Pattern.compile(
        "\\b(?:\\+?\\d{1,3}[-.\\s]?)?(?:\\(?\\d{2,4}\\)?[-.\\s]?)?\\d{3,4}[-.\\s]?\\d{3,4}\\b"
    )

    // Simple heuristic for code-like blocks (multiline or single with many symbols)
    private val CODE_HINTS = listOf(
        "fun ", "def ", "class ", "import ", "#include", "public ", "private ",
        "const ", "val ", "var ", "function", "=>", "->", "{", "}", "();", "package "
    )

    fun parse(text: String): List<DetectedEntity> {
        if (text.isBlank()) return emptyList()

        val entities = mutableListOf<DetectedEntity>()
        val occupied = BooleanArray(text.length)

        // Priority: URL > Email > Phone > Code
        findAll(URL_PATTERN, text, EntityType.URL, entities, occupied)
        findAll(EMAIL_PATTERN, text, EntityType.EMAIL, entities, occupied)
        findAll(PHONE_PATTERN, text, EntityType.PHONE, entities, occupied)

        // Code heuristic on remaining text
        if (looksLikeCode(text)) {
            entities.add(
                DetectedEntity(
                    type = EntityType.CODE_BLOCK,
                    value = text.trim(),
                    start = 0,
                    end = text.length,
                    label = "Code / Snippet"
                )
            )
        }

        return entities.sortedBy { it.start }
    }

    private fun findAll(
        pattern: Pattern,
        text: String,
        type: EntityType,
        out: MutableList<DetectedEntity>,
        occupied: BooleanArray
    ) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            if (start < 0 || end > text.length) continue
            if ((start until end).any { occupied[it] }) continue

            val value = matcher.group().trim()
            if (value.length < 4) continue

            // Extra validation
            when (type) {
                EntityType.URL -> if (!value.contains(".") && !value.startsWith("http")) continue
                EntityType.PHONE -> if (value.replace(Regex("[^\\d]"), "").length < 7) continue
                else -> {}
            }

            for (i in start until end) occupied[i] = true
            out.add(DetectedEntity(type, value, start, end))
        }
    }

    private fun looksLikeCode(text: String): Boolean {
        val lower = text.lowercase()
        val hintCount = CODE_HINTS.count { lower.contains(it.lowercase()) }
        val symbolRatio = text.count { it in "{}();=<>[]" }.toFloat() / text.length.coerceAtLeast(1)
        return hintCount >= 2 || (hintCount >= 1 && symbolRatio > 0.08f) || symbolRatio > 0.15f
    }

    fun primaryActionLabel(type: EntityType): String = when (type) {
        EntityType.URL -> "Open"
        EntityType.EMAIL -> "Email"
        EntityType.PHONE -> "Call"
        EntityType.CODE_BLOCK -> "Copy Code"
        EntityType.ADDRESS_LIKE -> "Maps"
        EntityType.PLAIN -> "Copy"
    }
}
