package com.example.scanner

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class ClientClass( private val isClient: Boolean,
private val hostAddress: InetAddress,
private val handler: Handler,
private val connected: AtomicBoolean,
private val cache: String,
private val recyclerViewAdapter: RecyclerViewAdapter,
private val contentResolver: ContentResolver,
private val notificationManager: NotificationManagerCompat,
private val context: Context
) : Thread() {

    private var socket: Socket? = null
    private var attempts = 20

    override fun run() {
        while (attempts > 0) {
            try {
                tryConnect()
            } catch (e: InterruptedException) {
                // Handle InterruptedException
            }
        }
    }

    /**
     * Method to attempt a connection, in case of an exception,
     * wait for 1s, and decrement the number of attempts.
     *
     * @throws InterruptedException
     */
    @Throws(InterruptedException::class)
    private fun tryConnect() {
        try {
            connect()
        } catch (e: SocketTimeoutException) {
            e.printStackTrace()
            SystemClock.sleep(1000)
            attempts--
        } catch (e: IOException) {
            e.printStackTrace()
            SystemClock.sleep(1000)
            attempts--
        }
    }
    /**
     * Method to connect to the server and start the main read and write thread.
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun connect() {
        socket = Socket()
        socket?.connect(InetSocketAddress(hostAddress, 8888), 10000)
        connected.set(true)
        attempts = 0
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
        Log.v("Connected", "Connected socket to client")
        handler.post {
            Toast.makeText(context, "Service ready", Toast.LENGTH_LONG).show()
        }
        val sendReceive =
            SendReceive(socket!!, 0, connected, cache, recyclerViewAdapter, contentResolver, notificationManager, handler)
        sendReceive.start()
        if (isClient) {
            sendReceive.write("send_media", "send_media".toByteArray())
        }
    }

}