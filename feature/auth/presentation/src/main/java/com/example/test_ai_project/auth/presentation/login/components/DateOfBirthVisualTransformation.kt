package com.example.test_ai_project.auth.presentation.login.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Renders eight stored digits as `dd/mm/yyyy`.
 *
 * The slashes are presentation only — the state holds digits, so nothing downstream has
 * to strip separators, and the caret still moves one digit at a time. The [OffsetMapping]
 * is what keeps the caret honest either side of an inserted slash.
 */
internal class DateOfBirthVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(DATE_DIGITS)
        val formatted = buildString {
            digits.forEachIndexed { index, digit ->
                if (index == DAY_DIGITS || index == DAY_DIGITS + MONTH_DIGITS) append('/')
                append(digit)
            }
        }

        return TransformedText(AnnotatedString(formatted), DateOffsetMapping)
    }

    private object DateOffsetMapping : OffsetMapping {

        override fun originalToTransformed(offset: Int): Int = when {
            offset <= DAY_DIGITS -> offset
            offset <= DAY_DIGITS + MONTH_DIGITS -> offset + 1
            else -> offset + 2
        }

        override fun transformedToOriginal(offset: Int): Int = when {
            offset <= DAY_DIGITS -> offset
            offset <= DAY_DIGITS + MONTH_DIGITS + 1 -> offset - 1
            else -> offset - 2
        }
    }

    private companion object {
        const val DAY_DIGITS = 2
        const val MONTH_DIGITS = 2
        const val DATE_DIGITS = 8
    }
}
