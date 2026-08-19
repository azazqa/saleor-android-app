package com.bdf.saleor.core.network

import com.bdf.saleor.core.model.ProductCmsBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCmsParserTest {
    private val cmsBase = "https://saleor-cms.klms.co.kr"

    @Test
    fun productCmsUrl_encodesSlug() {
        val url = productCmsUrl(cmsBase, "kendamil-keulraesig-1dangye")
        assertTrue(url.startsWith("$cmsBase/api/product-contents?"))
        assertTrue(url.contains("filters[saleorSlug][\$eq]=kendamil-keulraesig-1dangye"))
        assertTrue(url.contains("populate[blocks][populate]=*"))
    }

    @Test
    fun parse_classicStage1_blocks() {
        val json = """
            {"data":[{"saleorSlug":"kendamil-keulraesig-1dangye","blocks":[
              {"body":"# 상품설명","__component":"shared.rich-text"},
              {"file":{"url":"/uploads/Classic1tin_PDP_2.webp","alternativeText":null},"__component":"shared.media"},
              {"title":"title","body":"body","__component":"shared.quote"}
            ]}]}
        """.trimIndent()

        val blocks = parseProductCmsBlocks(json, cmsBase)
        assertEquals(3, blocks.size)
        assertEquals(ProductCmsBlock.Heading("상품설명", 1), blocks[0])
        assertEquals(
            ProductCmsBlock.Image("$cmsBase/uploads/Classic1tin_PDP_2.webp", null),
            blocks[1],
        )
        assertEquals(ProductCmsBlock.Quote("title", "body"), blocks[2])
    }

    @Test
    fun parse_richText_withMarkdownImage() {
        val markdown = "상품설명\n\n![alt](https://saleor-cms.klms.co.kr/uploads/a.png)"
        val blocks = parseRichTextMarkdown(markdown, cmsBase)
        assertEquals(ProductCmsBlock.Paragraph("상품설명"), blocks[0])
        assertEquals(
            ProductCmsBlock.Image("https://saleor-cms.klms.co.kr/uploads/a.png", "alt"),
            blocks[1],
        )
    }

    @Test
    fun parse_emptyOrUnknown_isEmpty() {
        assertTrue(parseProductCmsBlocks("{}", cmsBase).isEmpty())
        assertTrue(parseProductCmsBlocks("""{"data":[{"blocks":[{"__component":"shared.unknown"}]}]}""", cmsBase).isEmpty())
    }
}
