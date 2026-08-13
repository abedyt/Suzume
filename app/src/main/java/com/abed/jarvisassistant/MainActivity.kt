package com.abed.jarvisassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var statusText: TextView
    private lateinit var micButton: Button

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.SEND_SMS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        micButton = findViewById(R.id.micButton)

        requestNeededPermissions()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        micButton.setOnClickListener {
            startListening()
        }
    }

    private fun requestNeededPermissions() {
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 100)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                statusText.text = "Sun raha hoon..."
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val command = matches?.firstOrNull()?.lowercase() ?: ""
                statusText.text = "Command: $command"
                handleCommand(command)
            }

            override fun onError(error: Int) {
                statusText.text = "Samajh nahi aaya, phir se koshish karein"
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    private fun handleCommand(command: String) {
        when {
            command.contains("call") -> {
                val name = command.substringAfter("call").trim()
                if (name.isNotEmpty()) callContactByName(name)
            }
            command.contains("message") || command.contains("text") -> {
                Toast.makeText(this, "SMS feature: add message body handling here", Toast.LENGTH_SHORT).show()
            }
            command.contains("open") -> {
                val appName = command.substringAfter("open").trim()
                openAppByName(appName)
            }
            else -> {
                statusText.text = "Yeh command abhi supported nahi hai: $command"
            }
        }
    }

    private fun callContactByName(name: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Call permission nahi mili", Toast.LENGTH_SHORT).show()
            return
        }

        val phoneNumber = findPhoneNumberByContactName(name)
        if (phoneNumber == null) {
            statusText.text = "\"$name\" naam ka contact nahi mila"
            return
        }

        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(callIntent)
    }

    private fun findPhoneNumberByContactName(name: String): String? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return it.getString(numberIndex)
            }
        }
        return null
    }

    private fun openAppByName(appName: String) {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val match = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().lowercase().contains(appName)
        }
        if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
                return
            }
        }
        statusText.text = "\"$appName\" app nahi mili"
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
