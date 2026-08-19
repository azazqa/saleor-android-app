package com.bdf.saleor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonScalarTest {
    @Test
    fun string_isReturnedAsIs() {
        val raw = """{"blocks":[{"type":"paragraph","data":{"text":"Hello"}}]}"""
        assertEquals(raw, jsonScalarToString(raw))
    }

    @Test
    fun emptyJson_isDropped() {
        assertNull(jsonScalarToString(null))
        assertNull(jsonScalarToString(""))
        assertNull(jsonScalarToString("null"))
        assertNull(jsonScalarToString("{}"))
        assertNull(jsonScalarToString("[]"))
    }

    @Test
    fun map_encodesToJsonObject() {
        val value = mapOf(
            "blocks" to listOf(
                mapOf(
                    "type" to "paragraph",
                    "data" to mapOf("text" to "Nice tea"),
                ),
            ),
        )
        val encoded = jsonScalarToString(value)
        assertTrue(encoded!!.contains("Nice tea"))
        assertTrue(encoded.contains("\"blocks\""))
    }
}
