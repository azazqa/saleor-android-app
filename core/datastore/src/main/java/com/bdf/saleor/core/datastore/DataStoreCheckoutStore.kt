package com.bdf.saleor.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DataStoreCheckoutStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : CheckoutStore {
    override suspend fun checkoutId(channel: String): String? = dataStore.data.first()[key(channel)]

    override suspend fun setCheckoutId(channel: String, id: String?) {
        dataStore.edit { prefs ->
            val prefsKey = key(channel)
            if (id.isNullOrBlank()) {
                prefs.remove(prefsKey)
            } else {
                prefs[prefsKey] = id
            }
        }
    }

    override suspend fun clear(channel: String) {
        setCheckoutId(channel, null)
    }

    override suspend fun heldCartLines(channel: String): List<HeldCartLine> {
        val raw = dataStore.data.first()[heldKey(channel)] ?: return emptyList()
        return decodeHeldLines(raw)
    }

    override suspend fun setHeldCartLines(channel: String, lines: List<HeldCartLine>) {
        dataStore.edit { prefs ->
            val prefsKey = heldKey(channel)
            if (lines.isEmpty()) {
                prefs.remove(prefsKey)
            } else {
                prefs[prefsKey] = encodeHeldLines(lines)
            }
        }
    }

    private fun key(channel: String) = stringPreferencesKey("checkout_id_$channel")

    private fun heldKey(channel: String) = stringPreferencesKey("checkout_held_$channel")
}

internal fun encodeHeldLines(lines: List<HeldCartLine>): String =
    lines.joinToString("\n") { "${it.quantity}\t${it.variantId}" }

internal fun decodeHeldLines(raw: String): List<HeldCartLine> =
    raw.lineSequence().mapNotNull { line ->
        val tab = line.indexOf('\t')
        if (tab <= 0) return@mapNotNull null
        val quantity = line.substring(0, tab).toIntOrNull() ?: return@mapNotNull null
        val variantId = line.substring(tab + 1)
        if (variantId.isBlank() || quantity <= 0) null
        else HeldCartLine(variantId = variantId, quantity = quantity)
    }.toList()
