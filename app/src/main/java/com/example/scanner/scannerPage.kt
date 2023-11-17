package com.example.scanner

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.zxing.Result;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


class scannerPage : AppCompatActivity(),DecodeCallback,MultiplePermissionsListener {

    private lateinit var codeScanner: CodeScanner
    private lateinit var scannView: CodeScannerView
    private lateinit var resultData: TextView

    private lateinit var mManager: WifiP2pManager
    private lateinit var wifiManager: WifiManager
    private lateinit var mChannel: WifiP2pManager.Channel
    private lateinit var mReceiver: BroadcastReceiver
    private lateinit var mIntentFilter: IntentFilter

    private val connected = AtomicBoolean()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner_page)

        scannView = findViewById(R.id.scannerView)
        codeScanner = CodeScanner(this, scannView)
        resultData = findViewById(R.id.resultQR)
        codeScanner.setDecodeCallback(this)
        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Handle the received broadcast
                // You can add logic here based on the received broadcast
            }
        }
        intialize()
    }

    private fun intialize() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager.initialize(this, mainLooper, null)
        //mReceiver = MyBroadcastReceiver(mManager, mChannel, null, this)
        mIntentFilter = IntentFilter()
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.NEARBY_WIFI_DEVICES
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
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
        mManager.discoverPeers(mChannel, object :WifiP2pManager.ActionListener {
            override fun onSuccess() {
            }

            override fun onFailure(p0: Int) {
            }
        })
        connected.set(false)
        requestPermission()
    }

    override fun onDecoded(result: Result) {
        runOnUiThread {
            resultData.text = result.text
            val config = WifiP2pConfig()
            config.deviceAddress = result.text
            config.groupOwnerIntent = 15
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@runOnUiThread
            }
            mManager.connect(mChannel, config, object :WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Handle success, for example, show a Toast message
                    Toast.makeText(applicationContext, "Connected successfully", Toast.LENGTH_SHORT).show()
                }

               override fun onFailure(reason: Int) {
                    // Handle failure, e.g., print a Toast message
                    when (reason) {
                        WifiP2pManager.BUSY -> {
                            Toast.makeText(applicationContext, "Busy", Toast.LENGTH_SHORT).show()
                        }
                        WifiP2pManager.ERROR -> {
                            Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                        }
                        // Handle other failure cases if needed
                    }
                    codeScanner.startPreview()
                }
            })
            mManager.connect(mChannel, config, MyActionListener())
        }
    }
    override fun onPostResume() {
        super.onPostResume()
        requestPermission()
    }

    private fun requestPermission() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(this)
            .check()
    }

    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
        codeScanner.startPreview()
    }

    override fun onPermissionRationaleShouldBeShown(
        permissions: MutableList<PermissionRequest>?,
        token: PermissionToken?
    ) {
        Toast.makeText(this, "Impossible to use camera without proper permission", Toast.LENGTH_SHORT).show()
        token?.continuePermissionRequest()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, mIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }

    private val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
        val groupOwnerAddress = info.groupOwnerAddress
        if (!connected.get()) {
            runOnUiThread {
                Toast.makeText(applicationContext, "Device connected", Toast.LENGTH_SHORT).show()
            }
            if (info.groupFormed && info.isGroupOwner) {
                runOnUiThread {
                   /// val i = Intent(applicationContext, LoadingMedia::class.java)
                 //   startActivity(i)
                }
            } else if (info.groupFormed) {
                runOnUiThread {
                   // val i = Intent(applicationContext, LoadingMedia::class.java)
                //    i.putExtra("ip", groupOwnerAddress)
                //    startActivity(i)
                }
            }
        }
    }

    inner class MyActionListener : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            // Handle the case where the connection is successful
        }

        override fun onFailure(reason: Int) {
            when (reason) {
                WifiP2pManager.BUSY -> {
                    Toast.makeText(applicationContext, "Busy", Toast.LENGTH_SHORT).show()
                    codeScanner.startPreview()
                }
                WifiP2pManager.ERROR -> {
                    Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                    codeScanner.startPreview()
                }
            }
        }
    }
}