package com.bdf.saleor.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DataStoreTokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : TokenStore {
    @Volatile
    private var memoryAccessToken: String? = null

    override fun accessToken(): String? = memoryAccessToken

    override fun setAccessToken(token: String?) {
        memoryAccessToken = token
    }

    override suspend fun refreshToken(): String? = dataStore.data.first()[REFRESH_TOKEN]

    override suspend fun setRefreshToken(token: String?) {
        dataStore.edit { prefs ->
            if (token.isNullOrBlank()) {
                prefs.remove(REFRESH_TOKEN)
            } else {
                prefs[REFRESH_TOKEN] = token
            }
        }
    }

    override suspend fun clear() {
        memoryAccessToken = null
        dataStore.edit { it.remove(REFRESH_TOKEN) }
    }

    private companion object {
        val REFRESH_TOKEN = stringPreferencesKey("saleor_refresh_token")
    }
}
