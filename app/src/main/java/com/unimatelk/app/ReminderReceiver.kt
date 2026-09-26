package com.unimatelk.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val title = intent.getStringExtra("title")
            ?: "UniMate Reminder"

        val message = intent.getStringExtra("message")
            ?: "You have an upcoming task."

        val notificationId = intent.getIntExtra(
            "notificationId",
            System.currentTimeMillis().toInt()
        )

        ReminderManager.showReminder(
            context = context,
            title = title,
            message = message,
            notificationId = notificationId
        )
    }
}