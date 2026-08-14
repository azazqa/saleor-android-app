package com.bdf.saleor.ui.checkout

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.bdf.saleor.data.model.digitsOnlyMobile
import com.bdf.saleor.data.model.formatKoreanMobileNumber

object KoreanMobileVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = digitsOnlyMobile(text.text)
        return TransformedText(
            AnnotatedString(formatKoreanMobileNumber(digits)),
            KoreanMobileOffsetMapping(digits.length),
        )
    }
}

internal class KoreanMobileOffsetMapping(private val digitCount: Int) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, digitCount)
        return clamped + hyphenCountBefore(clamped)
    }

    override fun transformedToOriginal(offset: Int): Int {
        val formattedLength = formatKoreanMobileNumber("0".repeat(digitCount)).length
        val clamped = offset.coerceIn(0, formattedLength)
        return (clamped - hyphenCountBeforeTransformed(clamped)).coerceIn(0, digitCount)
    }

    private fun hyphenCountBefore(originalOffset: Int): Int {
        val secondGroupEnd = if (digitCount >= 11) 7 else 6
        var count = 0
        if (digitCount > 3 && originalOffset > 3) count++
        if (digitCount > secondGroupEnd && originalOffset > secondGroupEnd) count++
        return count
    }

    private fun hyphenCountBeforeTransformed(transformedOffset: Int): Int {
        val secondHyphenIndex = if (digitCount >= 11) 8 else 7
        var count = 0
        if (digitCount > 3 && transformedOffset > 3) count++
        if (digitCount > 6 && digitCount < 11 && transformedOffset > secondHyphenIndex) count++
        if (digitCount >= 11 && transformedOffset > secondHyphenIndex) count++
        return count
    }
}
