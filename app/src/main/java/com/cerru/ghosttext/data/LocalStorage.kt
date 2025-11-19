package com.cerru.ghosttext.data

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PasswordEntry(
    val id: String = UUID.randomUUID().toString(),
    val alias: String? = null,
    val password: String
)

data class ChannelEntry(
    val channelCode: String,
    val saltBase64: String,
    val passwordId: String? = null
)

class LocalStorage(context: Context) {
    private val prefs = context.getSharedPreferences("ghosttext_store", Context.MODE_PRIVATE)

    fun loadPasswords(): List<PasswordEntry> {
        val raw = prefs.getString(KEY_PASSWORDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                PasswordEntry(
                    id = obj.getString("id"),
                    alias = if (obj.has("alias")) obj.optString("alias", null) else null,
                    password = obj.getString("password")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun savePasswords(passwords: List<PasswordEntry>) {
        val array = JSONArray()
        passwords.forEach { entry ->
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("alias", entry.alias)
            obj.put("password", entry.password)
            array.put(obj)
        }
        prefs.edit().putString(KEY_PASSWORDS, array.toString()).apply()
    }

    fun addPassword(entry: PasswordEntry): List<PasswordEntry> {
        val updated = loadPasswords() + entry
        savePasswords(updated)
        return updated
    }

    fun deletePassword(id: String): List<PasswordEntry> {
        val updated = loadPasswords().filterNot { it.id == id }
        savePasswords(updated)
        return updated
    }

    fun loadChannels(): List<ChannelEntry> {
        val raw = prefs.getString(KEY_CHANNELS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                ChannelEntry(
                    channelCode = obj.getString("channelCode"),
                    saltBase64 = obj.getString("saltBase64"),
                    passwordId = if (obj.has("passwordId")) obj.optString("passwordId", null) else null
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveChannels(channels: List<ChannelEntry>) {
        val array = JSONArray()
        channels.forEach { entry ->
            val obj = JSONObject()
            obj.put("channelCode", entry.channelCode)
            obj.put("saltBase64", entry.saltBase64)
            obj.put("passwordId", entry.passwordId)
            array.put(obj)
        }
        prefs.edit().putString(KEY_CHANNELS, array.toString()).apply()
    }

    fun addChannel(entry: ChannelEntry): List<ChannelEntry> {
        val current = loadChannels()
        val withoutOld = current.filterNot { it.channelCode == entry.channelCode }
        val updated = withoutOld + entry
        saveChannels(updated)
        return updated
    }

    fun findChannel(code: String): ChannelEntry? = loadChannels().firstOrNull { it.channelCode == code }

    fun findPassword(id: String?): PasswordEntry? = loadPasswords().firstOrNull { it.id == id }

    fun findPasswordByAlias(alias: String?): PasswordEntry? = loadPasswords().firstOrNull { it.alias == alias }

    companion object {
        private const val KEY_PASSWORDS = "passwords"
        private const val KEY_CHANNELS = "channels"
    }
}
