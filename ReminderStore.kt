package com.yadawar.app

import android.content.Context
import org.json.JSONArray

object ReminderStore {
    private const val PREF = "yadawar"
    private const val ITEMS = "items"
    fun all(context: Context): MutableList<Reminder> {
        val raw = context.getSharedPreferences(PREF, 0).getString(ITEMS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return MutableList(arr.length()) { i -> Reminder.fromJson(arr.getJSONObject(i)) }
    }
    fun save(context: Context, items: List<Reminder>) {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREF, 0).edit().putString(ITEMS, arr.toString()).apply()
    }
    fun sound(context: Context) =
        context.getSharedPreferences(PREF, 0).getBoolean("sound", true)
    fun setSound(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF, 0).edit().putBoolean("sound", enabled).apply()
    }
}
