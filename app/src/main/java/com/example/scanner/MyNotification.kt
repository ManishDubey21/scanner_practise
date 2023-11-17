package com.example.scanner

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.scanner.App.Companion.CHANNEL_1_ID
import com.example.scanner.App.Companion.CHANNEL_2_ID


// Factory class that generates Pending Intent and Notification for the two channels.

object MyNotification {
    // Creates a pending intent to open the photo in the gallery.
    fun galleryIntent(context: Context, tmp: String): PendingIntent {
        val u: Uri = Uri.parse("content://$tmp")
        val intent = Intent(Intent.ACTION_QUICK_VIEW)
        intent.data = u
        intent.type = "image/*"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, 0)
        return pendingIntent
    }
// Creates a notification to indicate that the media has been downloaded successfully and can be viewed by clicking on the gallery.
fun getDownloadNotification(context: Context, path: String): Notification {
    // val p = galleryIntent(context, path)
    return NotificationCompat.Builder(context, CHANNEL_2_ID)
        .setSmallIcon(R.drawable.icon_download)
        .setContentTitle("Your photo has been downloaded")
        .setContentText("Your photo has been successfully saved in your gallery")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_EVENT)
        .setAutoCancel(true)
        .build()
}

// Creates a pending intent to be included in the service notification to disable it.
/**
 * @return
 */
fun serviceIntent(context: Context): PendingIntent {
    val broadcastIntent = Intent(context, Notificationreceiver::class.java)
    broadcastIntent.putExtra("disconnectedness", "si")
    return PendingIntent.getBroadcast(context, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT)
}


fun getServiceNotification(context: Context): Notification {
    val p = serviceIntent(context)
    return NotificationCompat.Builder(context, CHANNEL_1_ID)
        .setSmallIcon(R.drawable.ic_code_scanner_flash_on)
        .setContentTitle("MediaMe service")
        .setContentText("The service is running...")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_EVENT)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setColor(Color.RED)
        .addAction(R.drawable.ic_code_scanner_flash_on, "Disconnect", p)
        .build()
}
}