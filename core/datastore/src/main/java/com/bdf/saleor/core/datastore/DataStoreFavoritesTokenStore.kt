package com.bdf.saleor.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DataStoreFavoritesTokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FavoritesTokenStore {
    override suspend fun token(): String? {
        val prefs = dataStore.data.first()
        val savedAt = prefs[SAVED_AT] ?: return null
        if (System.currentTimeMillis() - savedAt > TOKEN_TTL_MS) {
            clear()
            return null
        }
        return prefs[TOKEN]
    }

    override suspend fun setToken(token: String) {
        dataStore.edit { prefs ->
            prefs[TOKEN] = token
            prefs[SAVED_AT] = System.currentTimeMillis()
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(TOKEN)
            prefs.remove(SAVED_AT)
        }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("saleor_favorites_token")
        val SAVED_AT = longPreferencesKey("saleor_favorites_token_saved_at")
        const val TOKEN_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
