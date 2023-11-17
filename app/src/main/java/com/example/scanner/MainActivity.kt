package com.example.scanner

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private lateinit var Scannerbtn: Button
    private lateinit var generateBtn: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Scannerbtn=findViewById(R.id.scan_btn)
        generateBtn=findViewById(R.id.generateQR_btn)
        Scannerbtn.setOnClickListener{
            val intent= Intent(this,scannerPage::class.java)
            startActivity(intent)
        }
        generateBtn.setOnClickListener{
            // Launch the QR code generator that first asks for the Wi-Fi information and
            //  then takes the device specifications and generates the QR code.
            val intent= Intent(this,generateQRCode::class.java)
            startActivity(intent)
        }
    }
}