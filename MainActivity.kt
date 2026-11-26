package com.yadawar.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val bg = Color.rgb(16, 13, 25)
    private val panel = Color.rgb(27, 22, 40)
    private val purple = Color.rgb(167, 139, 250)
    private var chosenHour = 21
    private var chosenMinute = 0
    private lateinit var root: LinearLayout
    private lateinit var clock: TextView
    private lateinit var stats: TextView
    private lateinit var list: LinearLayout
    private lateinit var soundButton: Button
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            if (::clock.isInitialized) {
                clock.text = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            }
            clockHandler.postDelayed(this, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        clockHandler.post(clockTick)
        if (::list.isInitialized) refresh()
    }

    override fun onPause() {
        clockHandler.removeCallbacks(clockTick)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        buildUi()
        refresh()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!am.canScheduleExactAlarms()) {
                AlertDialog.Builder(this).setTitle("ڕێگەپێدانی کاتژمێری ورد")
                    .setMessage("بۆ ئەوەی بیرخەرەوەکان لە کاتی خۆیاندا بگەن، ڕێگە بە ئاگادارکردنەوەی کاتژمێری ورد بدە.")
                    .setPositiveButton("کردنەوەی ڕێکخستنەکان") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName")))
                    }.setNegativeButton("دواتر", null).show()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 40)
        }
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 28, 20, 32)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        scroll.addView(root)
        setContentView(scroll)

        val header = row()
        val title = text("◷  یادەوەر", 25, Color.WHITE, true)
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        soundButton = button(soundLabel())
        soundButton.setOnClickListener {
            val enabled = !ReminderStore.sound(this)
            ReminderStore.setSound(this, enabled)
            soundButton.text = soundLabel()
        }
        header.addView(soundButton)
        root.addView(header)

        val hero = card()
        hero.addView(text("کاتی ئێستا", 14, purple))
        clock = text("--:--:--", 42, Color.WHITE, true)
        hero.addView(clock)
        hero.addView(text(SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ckb")).format(Date()), 14, 0xFFB8ACC9.toInt()))
        stats = text("", 14, Color.WHITE)
        val p = LinearLayout.LayoutParams(-1, -2); p.topMargin = 18
        hero.addView(stats, p)
        root.addView(hero)

        val form = card()
        form.addView(text("＋ زیادکردنی بیرخەرەوە", 19, Color.WHITE, true))
        val message = EditText(this).apply {
            hint = "نامە یان بیرخەرەوەکەت بنووسە"
            setHintTextColor(0xFF998DAA.toInt()); setTextColor(Color.WHITE)
            textSize = 16f; setSingleLine(false); minLines = 1; maxLines = 3
            setPadding(14, 12, 14, 12); setBackgroundColor(0xFF100D19.toInt())
        }
        val mp = LinearLayout.LayoutParams(-1, -2); mp.topMargin = 14
        form.addView(message, mp)
        val timeButton = button("کات: ٢١:٠٠")
        timeButton.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                chosenHour = h; chosenMinute = m
                timeButton.text = "کات: " + String.format(Locale.US, "%02d:%02d", h, m)
            }, chosenHour, chosenMinute, true).show()
        }
        form.addView(timeButton)
        val quick = row()
        listOf(8 to 0, 12 to 0, 18 to 0, 21 to 0).forEach { (h, m) ->
            val b = button(String.format(Locale.US, "%02d:00", h))
            b.setOnClickListener { chosenHour = h; chosenMinute = m; timeButton.text = "کات: " + String.format(Locale.US, "%02d:%02d", h, m) }
            quick.addView(b, LinearLayout.LayoutParams(0, -2, 1f))
        }
        form.addView(quick)
        val add = button("زیادکردنی بیرخەرەوە").apply {
            setBackgroundColor(0xFF7C3AED.toInt())
            setTextColor(Color.WHITE)
            setOnClickListener {
                val msg = message.text.toString().trim()
                if (msg.isEmpty()) { message.error = "نامەکە بنووسە"; return@setOnClickListener }
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, chosenHour); set(Calendar.MINUTE, chosenMinute)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                }
                val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                val item = Reminder(id, msg, cal.timeInMillis)
                val items = ReminderStore.all(this@MainActivity)
                items.add(item); ReminderStore.save(this@MainActivity, items)
                AlarmScheduler.schedule(this@MainActivity, item)
                message.text.clear()
                Toast.makeText(this@MainActivity, "بیرخەرەوەکە زیاد کرا", Toast.LENGTH_SHORT).show()
                refresh()
            }
        }
        form.addView(add)
        form.addView(text("ئەگەر کاتەکەی ئەمڕۆ تێپەڕیبوو، بۆ سبەی لە هەمان کات دادەنرێت.", 12, 0xFFAA9EBB.toInt()))
        root.addView(form)

        val section = card()
        section.addView(text("📋 لیستی بیرخەرەوەکان", 19, Color.WHITE, true))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        section.addView(list)
        root.addView(section)
    }

    private fun refresh() {
        if (!::list.isInitialized) return
        clock.text = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val items = ReminderStore.all(this).sortedBy { it.time }
        stats.text = "هەموو: ${items.size}     چاوەڕوان: ${items.count { it.active && !it.sent }}     نێردراو: ${items.count { it.sent }}"
        list.removeAllViews()
        if (items.isEmpty()) list.addView(text("هێشتا بیرخەرەوەیەک نییە", 14, 0xFFAA9EBB.toInt()))
        items.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(14, 12, 14, 12)
                setBackgroundColor(0xFF241C35.toInt())
            }
            val whenText = SimpleDateFormat("HH:mm  ·  yyyy/MM/dd", Locale.US).format(Date(item.time))
            card.addView(text(whenText, 13, purple, true))
            card.addView(text(item.message, 16, Color.WHITE, true))
            card.addView(text(if (item.sent) "ئاگادارکراوە" else if (item.active) "چاوەڕوانی کاتە" else "ناچالاک", 12,
                if (item.active) 0xFF86EFAC.toInt() else 0xFFFCD34D.toInt()))
            val actions = row()
            val toggle = button(if (item.active) "ناچالاککردن" else "چالاککردن")
            toggle.setOnClickListener {
                if (item.active) {
                    AlarmScheduler.cancel(this, item); item.active = false
                } else {
                    item.active = true; item.sent = false
                    if (item.time <= System.currentTimeMillis()) {
                        val c = Calendar.getInstance().apply {
                            timeInMillis = item.time
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        item.time = c.timeInMillis
                    }
                    AlarmScheduler.schedule(this, item)
                }
                ReminderStore.save(this, ReminderStore.all(this).map { if (it.id == item.id) item else it })
                refresh()
            }
            val delete = button("سڕینەوە")
            delete.setOnClickListener {
                AlarmScheduler.cancel(this, item)
                ReminderStore.save(this, ReminderStore.all(this).filterNot { it.id == item.id })
                refresh()
            }
            actions.addView(toggle, LinearLayout.LayoutParams(0, -2, 1f))
            actions.addView(delete, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(actions)
            val lp = LinearLayout.LayoutParams(-1, -2); lp.topMargin = 9
            list.addView(card, lp)
        }
    }

    private fun soundLabel() = if (ReminderStore.sound(this)) "🔔 دەنگ: چالاک" else "🔕 دەنگ: ناچالاک"
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(16, 17, 16, 17); setBackgroundColor(panel)
        val p = LinearLayout.LayoutParams(-1, -2); p.topMargin = 16
        layoutParams = p
    }
    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(2, 5, 2, 5)
    }
    private fun button(value: String) = Button(this).apply {
        text = value; textSize = 12f; isAllCaps = false; setTextColor(Color.WHITE)
        setBackgroundColor(0xFF392653.toInt())
        val p = LinearLayout.LayoutParams(-2, -2); p.setMargins(3, 6, 3, 6)
        layoutParams = p
    }
}
