package com.yadawar.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", 0)
        val message = intent.getStringExtra("message") ?: "کاتی بیرخەرەوەیە"
        val items = ReminderStore.all(context)
        items.find { it.id == id }?.let {
            it.active = false
            it.sent = true
            ReminderStore.save(context, items)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundEnabled = ReminderStore.sound(context)
        val channelId = if (soundEnabled) "yadawar_sound" else "yadawar_silent"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId, if (soundEnabled) "ئاگادارکردنەوە بە دەنگ" else "ئاگادارکردنەوە بێ دەنگ",
                    NotificationManager.IMPORTANCE_HIGH
                )
                if (!soundEnabled) channel.setSound(null, null)
                manager.createNotificationChannel(channel)
            }
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(context, channelId) else Notification.Builder(context)
        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("یادەوەر")
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(Notification.PRIORITY_HIGH)
        manager.notify(id, builder.build())
    }
}
