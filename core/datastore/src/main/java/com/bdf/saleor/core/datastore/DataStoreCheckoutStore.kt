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

    private fun key(channel: String) = stringPreferencesKey("checkout_id_$channel")
}
