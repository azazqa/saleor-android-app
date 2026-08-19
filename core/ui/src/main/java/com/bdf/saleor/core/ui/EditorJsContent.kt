package com.bdf.saleor.core.ui

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import coil3.compose.AsyncImage
import com.bdf.saleor.core.designsystem.theme.AppSpacing
import com.bdf.saleor.core.model.EditorJsBlock

@Composable
fun EditorJsContent(
    blocks: List<EditorJsBlock>,
    modifier: Modifier = Modifier,
) {
    if (blocks.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.CardContent),
    ) {
        blocks.forEach { block ->
            when (block) {
                is EditorJsBlock.Header -> Text(
                    text = block.text,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.headlineSmall
                        3 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                )
                is EditorJsBlock.Paragraph -> Text(
                    text = rememberHtmlAnnotatedString(block.html),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                is EditorJsBlock.Image -> {
                    AsyncImage(
                        model = block.url,
                        contentDescription = block.caption,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium),
                    )
                    val caption = block.caption
                    if (!caption.isNullOrBlank()) {
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is EditorJsBlock.ListBlock -> Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    block.items.forEachIndexed { index, item ->
                        val marker = if (block.ordered) "${index + 1}." else "•"
                        Text(
                            text = "$marker $item",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                is EditorJsBlock.Quote -> Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
                EditorJsBlock.Delimiter -> HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun rememberHtmlAnnotatedString(html: String): AnnotatedString {
    return remember(html) { html.toAnnotatedString() }
}

private fun String.toAnnotatedString(): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)
    val builder = AnnotatedString.Builder(spanned.toString())
    spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
        val start = spanned.getSpanStart(span)
        val end = spanned.getSpanEnd(span)
        if (start < 0 || end <= start) return@forEach
        when (span) {
            is StyleSpan -> when (span.style) {
                Typeface.BOLD, Typeface.BOLD_ITALIC -> builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    start,
                    end,
                )
                Typeface.ITALIC -> builder.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    start,
                    end,
                )
            }
            is URLSpan -> {
                builder.addStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline),
                    start,
                    end,
                )
                builder.addLink(LinkAnnotation.Url(span.url), start, end)
            }
        }
    }
    return builder.toAnnotatedString()
}
