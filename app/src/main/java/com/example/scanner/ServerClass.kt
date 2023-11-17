package com.example.scanner

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class ServerClass(
    private val isClient: Boolean,
    private val handler: Handler,
    private val isConnected: AtomicBoolean,
    private val cacheDir: String,
    private val recyclerViewAdapter: RecyclerViewAdapter,
    private val contentResolver: ContentResolver,
    private val notificationManager: NotificationManagerCompat,
    private val context: Context
) : Thread() {

    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null

    override fun run() {
        try {
            serverSocket = ServerSocket(8888)
            socket = serverSocket?.accept()
            isConnected.set(true)
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notificationManager.notify(0, MyNotification.getServiceNotification(context))
            handler.post {
                Toast.makeText(context, "Service ready", Toast.LENGTH_LONG).show()
            }

            val sendReceive = SendReceive(
                socket!!,
                15,
                isConnected,
                cacheDir,
                recyclerViewAdapter,
                contentResolver,
                notificationManager,
                handler
            )
            sendReceive.start()
            if (isClient) sendReceive.write("send_media", "send_media".toByteArray())

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}