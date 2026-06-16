package com.baulsanitario.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.profileSessionDataStore by preferencesDataStore(name = "profile_session")

class ProfilePreferencesDataSource(private val context: Context) {

    private val activeProfileIdKey = stringPreferencesKey("active_profile_id")

    suspend fun getActiveProfileId(): String? =
        context.profileSessionDataStore.data.first()[activeProfileIdKey]

    suspend fun setActiveProfileId(profileId: String) {
        context.profileSessionDataStore.edit { it[activeProfileIdKey] = profileId }
    }
}
