package com.example.scanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.os.Handler
import android.os.Message
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.MediaController
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class loadingMedia : AppCompatActivity(),MediaClickListener {
    private val connected = AtomicBoolean(true)
    private lateinit var recyView: RecyclerView
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var recycleviewAdapter: RecyclerViewAdapter
    private lateinit var notificationManager: NotificationManagerCompat

    companion object {
        const val MESSAGE_READ = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Activity that allows us to read shared media from the connected device within a RecyclerView.
        setContentView(R.layout.activity_loading_media)
        recyView = findViewById(R.id.recycleView)
        layoutManager = GridLayoutManager(this, 3)
        recyView.layoutManager = layoutManager
        notificationManager = NotificationManagerCompat.from(this)
        recycleviewAdapter = RecyclerViewAdapter(this, this)
        recyView.adapter = recycleviewAdapter
        recyView.setHasFixedSize(true)
        recyView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (!recyclerView.canScrollVertically(1)) {
                    SendReceive.getInstance()?.write("send_media", "send_media".toByteArray())
                }
            }
        })

        val b = intent.extras
        if (b != null && b.containsKey("ip")) {
            val address = b.get("ip") as InetAddress
            val client = ClientClass(
                true,
                address,
                handler,
                connected,
                cacheDir.toString(),
                recycleviewAdapter,
                contentResolver,
                notificationManager,
                this
            )
            client.start()
        } else {
            val server = ServerClass(
                true,
                handler,
                connected,
                cacheDir.toString(),
                recycleviewAdapter,
                contentResolver,
                notificationManager,
                this
            )
            server.start()
        }
    }
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == SendReceive.DISCONNECT) {
                Toast.makeText(applicationContext, "Disconnected", Toast.LENGTH_SHORT).show()
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
        }
    }

    override fun onMediaClick(path: String, id: String) {
        val i = Intent(this, ShowMedia::class.java)
        i.putExtra("media", path)
        i.putExtra("id", id)
        startActivity(i)
    }
}