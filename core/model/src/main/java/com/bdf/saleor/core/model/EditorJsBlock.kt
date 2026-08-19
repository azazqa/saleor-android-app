package com.bdf.saleor.core.model

sealed class EditorJsBlock {
    data class Header(val text: String, val level: Int) : EditorJsBlock()
    data class Paragraph(val html: String) : EditorJsBlock()
    data class Image(val url: String, val caption: String?) : EditorJsBlock()
    data class ListBlock(val ordered: Boolean, val items: List<String>) : EditorJsBlock()
    data class Quote(val text: String) : EditorJsBlock()
    data object Delimiter : EditorJsBlock()
}
