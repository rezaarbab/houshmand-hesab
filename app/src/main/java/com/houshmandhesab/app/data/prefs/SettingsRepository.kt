package com.houshmandhesab.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.houshmandhesab.app.util.Format
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val PERSIAN_DIGITS = booleanPreferencesKey("persian_digits")
        val CURRENCY = stringPreferencesKey("currency_symbol")
        val CF_ACCOUNT = stringPreferencesKey("cf_account_id")
        val CF_TOKEN = stringPreferencesKey("cf_api_token")
        val CF_MODEL = stringPreferencesKey("cf_model")
        val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_LENGTH = intPreferencesKey("pin_length")
        val FIRST_RUN = booleanPreferencesKey("first_run_done")
        val LAST_NOTIFY_DAY = longPreferencesKey("last_notify_day")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }

    val themeMode: Flow<String> = dataStore.data.map { it[Keys.THEME] ?: "SYSTEM" }
    val persianDigits: Flow<Boolean> = dataStore.data.map { it[Keys.PERSIAN_DIGITS] ?: true }
    val currencySymbol: Flow<String> = dataStore.data.map { it[Keys.CURRENCY] ?: "" }
    val cfAccountId: Flow<String> = dataStore.data.map { it[Keys.CF_ACCOUNT] ?: "" }
    val cfToken: Flow<String> = dataStore.data.map { it[Keys.CF_TOKEN] ?: "" }
    val cfModel: Flow<String> = dataStore.data.map { it[Keys.CF_MODEL] ?: DEFAULT_MODEL }
    val lockEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.LOCK_ENABLED] ?: false }
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.BIOMETRIC] ?: true }
    val pinHash: Flow<String> = dataStore.data.map { it[Keys.PIN_HASH] ?: "" }
    val pinLength: Flow<Int> = dataStore.data.map { it[Keys.PIN_LENGTH] ?: 4 }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.NOTIFICATIONS] ?: true }

    suspend fun isFirstRun(): Boolean = dataStore.data.first()[Keys.FIRST_RUN] != true

    suspend fun setFirstRunDone() = dataStore.edit { it[Keys.FIRST_RUN] = true }

    suspend fun setThemeMode(v: String) = dataStore.edit { it[Keys.THEME] = v }

    suspend fun setPersianDigits(v: Boolean) = dataStore.edit { it[Keys.PERSIAN_DIGITS] = v }

    suspend fun setCurrencySymbol(v: String) = dataStore.edit { it[Keys.CURRENCY] = v }

    suspend fun setCfAccountId(v: String) = dataStore.edit { it[Keys.CF_ACCOUNT] = v.trim() }

    suspend fun setCfToken(v: String) = dataStore.edit { it[Keys.CF_TOKEN] = v.trim() }

    suspend fun setCfModel(v: String) = dataStore.edit { it[Keys.CF_MODEL] = v.trim() }

    suspend fun setLockEnabled(v: Boolean) = dataStore.edit { it[Keys.LOCK_ENABLED] = v }

    suspend fun setBiometricEnabled(v: Boolean) = dataStore.edit { it[Keys.BIOMETRIC] = v }

    suspend fun setPinHash(v: String) = dataStore.edit {
        it[Keys.PIN_HASH] = v
        it[Keys.PIN_LENGTH] = 4
    }

    suspend fun setPin(pin: String) = dataStore.edit {
        it[Keys.PIN_HASH] = Format.sha256(pin)
        it[Keys.PIN_LENGTH] = pin.length.coerceIn(4, 6)
    }

    suspend fun setNotificationsEnabled(v: Boolean) = dataStore.edit { it[Keys.NOTIFICATIONS] = v }

    suspend fun lastNotifyDay(): Long = dataStore.data.first()[Keys.LAST_NOTIFY_DAY] ?: 0L

    suspend fun setLastNotifyDay(v: Long) = dataStore.edit { it[Keys.LAST_NOTIFY_DAY] = v }

    companion object {
        const val DEFAULT_MODEL = "@cf/meta/llama-3.1-8b-instruct"
        val AVAILABLE_MODELS = listOf(
            "@cf/meta/llama-3.1-8b-instruct",
            "@cf/meta/llama-3.3-70b-instruct-fp8-fast",
            "@cf/qwen/qwen1.5-14b-chat-awq",
            "@cf/google/gemma-7b-it",
            "@cf/mistral/mistral-7b-instruct-v0.1"
        )
    }
}
