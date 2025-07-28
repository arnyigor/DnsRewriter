package com.arny.dnsrewriter.data.local

import android.content.SharedPreferences
import com.arny.dnsrewriter.domain.model.NextDnsProfile
import androidx.core.content.edit

class ProfileStorage(private val prefs: SharedPreferences) {

    fun saveProfile(profile: NextDnsProfile) {
        prefs.edit {
            putString(KEY_CONFIG_ID, profile.configId)
                .putString(KEY_COOKIES, profile.cookies)
        }
    }

    fun getProfile(): NextDnsProfile? {
        val configId = prefs.getString(KEY_CONFIG_ID, null)
        val cookies = prefs.getString(KEY_COOKIES, null)

        return if (configId != null && cookies != null) {
            NextDnsProfile(configId, cookies)
        } else {
            null
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val KEY_CONFIG_ID = "nextdns_config_id"
        private const val KEY_COOKIES = "nextdns_cookies"
    }
}