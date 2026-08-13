package com.bdf.saleor.data

import java.util.Base64

internal object Jwt {
    fun isExpired(token: String, graceSeconds: Long = 2): Boolean {
        val parts = token.split('.')
        if (parts.size < 2) return true
        val payload = runCatching {
            String(Base64.getUrlDecoder().decode(parts[1]))
        }.getOrNull() ?: return true
        val exp = Regex("\"exp\"\\s*:\\s*(\\d+)")
            .find(payload)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()
            ?: return true
        val nowSeconds = System.currentTimeMillis() / 1000
        return nowSeconds + graceSeconds >= exp
    }
}
