package com.yadawar.app

import org.json.JSONObject

data class Reminder(
    val id: Int,
    val message: String,
    var time: Long,
    var active: Boolean = true,
    var sent: Boolean = false
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("message", message); put("time", time)
        put("active", active); put("sent", sent)
    }

    companion object {
        fun fromJson(o: JSONObject) = Reminder(
            o.getInt("id"), o.getString("message"), o.getLong("time"),
            o.optBoolean("active", true), o.optBoolean("sent", false)
        )
    }
}
