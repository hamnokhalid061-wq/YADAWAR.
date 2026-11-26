package com.yadawar.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderStore.all(context).filter { it.active && !it.sent && it.time > System.currentTimeMillis() }
            .forEach { AlarmScheduler.schedule(context, it) }
    }
}
