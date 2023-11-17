package com.example.scanner

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;

import androidx.core.app.NotificationManagerCompat;
import kotlinx.coroutines.sync.Mutex


/**
* Thread that effectively implements the background service for sending and receiving messages on the TCP socket.
* This thread is instantiated on both the client and server sides, to prepare the application for future implementation
* of real-time gallery sharing on both devices.
*/


class SendReceive (
    private val socket: Socket,
    private val type: Int,
    private val isConnected: AtomicBoolean,
    private val cacheDir: String,
    private val recyclerViewAdapter: RecyclerViewAdapter,
    private val contentResolver: ContentResolver,
    private val notificationManager: NotificationManagerCompat,
    private val handler: Handler
) : Thread() {
    private var inputStream: DataInputStream?= null
    private var outputStream: DataOutputStream?=null
    private var imagePaths: ArrayList<String>? = null
    private val lock= Mutex()
    private var photoDownloaded = StringBuilder()
    private var photoRead = 0

    companion object {
        private var Instance: SendReceive? = null
        const val HEADER_SIZE = 512
        const val DISCONNECT = -1

        @JvmStatic
        fun getInstance(): SendReceive? {
            return Instance
        }
    }

    init {
        try {
            inputStream = DataInputStream(socket.getInputStream())
            outputStream = DataOutputStream(socket.getOutputStream())
            Instance=this
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException("Error initializing SendReceive")
        }
    }
    override fun run() {
        while (socket != null && !socket.isClosed) {
            try {
                Log.v("Connected", socket.toString())
                if (!socket.isConnected && !isConnected.get()) {
                    disconnect()
                } else {
                    val length = inputStream!!.readInt()
                    if (length > 0) {
                        val hd = ByteArray(HEADER_SIZE)
                        inputStream!!.readFully(hd, 0, HEADER_SIZE)
                        val header = String(hd, 0, hd.size)

                        if (header.contains(",") && !header.contains("id") && !header.contains("path")) {
                            val split = header.split(",")
                            val n_foto = split.size - 1
                            val sizes = IntArray(n_foto)
                            var len = 0
                            for (i in 0 until n_foto) {
                                sizes[i] = split[i].split(":")[0].toInt()
                                len += sizes[i]
                            }
                            val message = ByteArray(len)
                            inputStream!!.readFully(message, 0, len)
                            imagePaths = ArrayList()
                            val ids = ArrayList<String>()
                            var letti = 0
                            for (i in 0 until n_foto) {
                                try {
                                    ids.add(split[i].split(":")[1])
                                    val downloadingMediaFile = File(cacheDir, "${split[i].split(":")[1]}.jpg")
                                    val out = FileOutputStream(downloadingMediaFile)
                                    out.write(message, letti, sizes[i])
                                    try {
                                        out.close()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                    val path = /*"file://" +*/ "$cacheDir/${split[i].split(":")[1]}.jpg"
                                    imagePaths!!.add(path)
                                    letti += sizes[i]
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            Log.v("Connected", "I loaded:$n_foto foto")

                            if (imagePaths != null && imagePaths!!.size > 0 && recyclerViewAdapter != null) {
                                handler.post {
                                    recyclerViewAdapter.addFoto(imagePaths!!, ids)
                                }
                            }
                        } else if (header.contains("send_media")) {
                            val n_foto = 12
                            val queryresult = MyphotoMedia(photoRead, n_foto)
                            photoRead += queryresult.paths.size
                            var total: ByteArray
                            val sizes = ArrayList<Int>()
                            var tot = 0
                            var new_header = ""
                            val elem = ArrayList<ByteArray>()
                            for (i in queryresult.paths.indices) {
                                try {
                                    val bytesImg =
                                        BitmapUtils.getBytesImageCompressedFromPath(queryresult.paths[i], 80, 40, 40)
                                    tot += bytesImg.size
                                    sizes.add(bytesImg.size)
                                    elem.add(bytesImg)
                                    new_header += "${bytesImg.size}:${queryresult.ids[i]},"
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            total = ByteArray(tot)
                            var scritti = 0
                            for (i in sizes.indices) {
                                try {
                                    System.arraycopy(elem[i], 0, total, scritti, sizes[i])
                                    scritti += sizes[i]
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            write(new_header, total)
                        } else if (header.contains("id")) {
                            val id = header.split(",")[0].split(":")[1]
                            val path = getMediaFromID(id)
                            val f = File(path)
                            val message = ByteArray(f.length().toInt())
                            val buf = BufferedInputStream(FileInputStream(f))
                            buf.read(message, 0, message.size)
                            buf.close()
                            write("path:hd_$id,", message)
                        } else if (header.contains("path")) {
                            val message = ByteArray(length - HEADER_SIZE)
                            val name = header.split(",")[0].split(":")[1]
                            inputStream!!.readFully(message, 0, length - HEADER_SIZE)
                            val downloadingMediaFile = File(cacheDir, "$name.jpg")
                            val out = FileOutputStream(downloadingMediaFile)
                            out.write(message, 0, message.size)
                            out.close()
                            synchronized(photoDownloaded) {
                                photoDownloaded.append(downloadingMediaFile.absolutePath)
                                photoDownloaded.notifyAll()
                            }
                            Log.v("Connected", "Photo arrived:$photoDownloaded")
                        }
                    }
                }
            } catch (e: IOException) {
                disconnect()
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket.close()
            isConnected.set(false)
            notificationManager.cancel(0)
            val msg = handler.obtainMessage()
            msg.what = DISCONNECT
            handler.sendMessage(msg)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }
    fun write(header: String, bytes: ByteArray) {
        val invite = outputStream?.let { Send(it) }
        invite?.setBytes(header, bytes)
        invite?.start()
    }
    fun writeWithReturn(header: String, bytes: ByteArray): String? {
        val pool = Executors.newSingleThreadExecutor()
        val req = outputStream?.let { MediaRequest(bytes, it, header) }
        val res: Future<String> = pool.submit(req)
        return try {
            res.get(20000L, TimeUnit.MILLISECONDS)
        } catch (e: ExecutionException) {
            e.printStackTrace()
            null
        } catch (e: InterruptedException) {
            e.printStackTrace()
            null
        } catch (e: TimeoutException) {
            e.printStackTrace()
            null
        }
    }

    private fun MyphotoMedia(letti: Int, n: Int): MediaIdsPaths {
        val columns = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media._ID,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME
        )
        val orderBy = "${MediaStore.Images.Media.DATE_TAKEN} DESC LIMIT $letti,$n"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            columns,
            null,
            null,
            orderBy
        )

        val paths = Array(cursor?.count ?: 0) { "" }
        val ids = Array(cursor?.count ?: 0) { "" }

        cursor?.use {
            for (i in 0 until it.count) {
                it.moveToPosition(i)
                val dataColumnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                val idColumn = it.getColumnIndex(MediaStore.Images.Media._ID)
                val path = it.getString(dataColumnIndex)
                paths[i] = path
                ids[i] = it.getString(idColumn)
            }
        }

        return MediaIdsPaths(ids, paths)
    }

    private fun getMediaFromID(id: String): String {
        val path: Uri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id.toLong()
        )

        val columns = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media._ID
        )

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            columns,
            "${MediaStore.Images.Media._ID} = $id",
            null,
            null
        )

        cursor?.use {
            it.moveToPosition(0)
            val idColumn = it.getColumnIndex(MediaStore.Images.Media.DATA)
            return it.getString(idColumn)
        }

        return ""
    }

    data class MediaIdsPaths(val ids: Array<String>, val paths: Array<String>)

    inner class MediaRequest(
        private val bytes: ByteArray,
        private val outputStream: DataOutputStream,
        private val header: String
    ) : Callable<String> {

        @Throws(Exception::class)
        override fun call(): String {
            try {
                val headerBytes = header.toByteArray()
                val total = ByteArray(bytes.size + HEADER_SIZE)
                System.arraycopy(headerBytes, 0, total, 0, headerBytes.size)
                System.arraycopy(bytes, 0, total, HEADER_SIZE, bytes.size)
                outputStream.writeInt(total.size)
                outputStream.write(total)
                outputStream.flush()
                var res = ""
                synchronized(photoDownloaded) {
                    while (photoDownloaded.length==0)
                        try {
                            photoDownloaded.wait()
                        }catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    res = photoDownloaded.toString()
                    photoDownloaded.setLength(0)
                }
                return res
            } catch (e: IOException) {
                e.printStackTrace()
                return ""
            }
        }
    }

    inner class Send(private val outputStream: DataOutputStream) : Thread() {

        private lateinit var bytes: ByteArray
        private lateinit var header: String

        fun setBytes(hd: String, b: ByteArray) {
            bytes = b
            header = hd
        }

        override fun run() {
            try {
                val headerBytes = header.toByteArray()
                val total = ByteArray(bytes.size + HEADER_SIZE)
                System.arraycopy(headerBytes, 0, total, 0, headerBytes.size)
                System.arraycopy(bytes, 0, total, HEADER_SIZE, bytes.size)
                outputStream.writeInt(total.size)
                outputStream.write(total)
                outputStream.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}