package com.example.scanner

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("disconnected")
        if (message != null && message == "yes") {
            val sendReceiveInstance = SendReceive.getInstance()
            if (sendReceiveInstance != null) {
                sendReceiveInstance.disconnect()
            } else {
                // Handle the case where SendReceive.getInstance() is null
                Toast.makeText(context, "SendReceive instance is null", Toast.LENGTH_SHORT).show()
            }
        }
    }
}