package com.jeromelens.app.util

/**
 * Predefined categories for organizing OCR clips.
 * Users can also type a custom category.
 */
object Categories {
    const val UNCATEGORIZED = "Uncategorized"

    val PREDEFINED = listOf(
        UNCATEGORIZED,
        "Work",
        "Personal",
        "Code",
        "Receipts",
        "Notes",
        "Links",
        "Study",
        "Other"
    )

    const val MAX_BATCH_IMAGES = 10
}
