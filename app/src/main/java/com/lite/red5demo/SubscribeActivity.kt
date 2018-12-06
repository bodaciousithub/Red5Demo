package com.lite.red5demo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.red5pro.streaming.config.R5Configuration
import com.red5pro.streaming.R5StreamProtocol
import kotlinx.android.synthetic.main.activity_subscribe.*
import com.red5pro.streaming.R5Connection
import com.red5pro.streaming.R5Stream
import com.red5pro.streaming.media.R5AudioController

class SubscribeActivity : AppCompatActivity() {

    private var configuration: R5Configuration? = null
    private var isSubscribing: Boolean = false
    private var stream: R5Stream? = null

    private var serverAddress: String? = null
    private var serverPort: String? = null
    private var streamName: String? = null
    private var selectedDimension: Int = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscribe)

        serverAddress = intent.getStringExtra("server")
        serverPort = intent.getStringExtra("port")
        streamName = intent.getStringExtra("stream")
        selectedDimension = intent.getIntExtra("dimension", 1)

        configuration = R5Configuration(R5StreamProtocol.RTSP, serverAddress, 8554, "live", 0.5f)
        configuration!!.licenseKey = "HF4E-TAIB-DZJG-PGV7"
        configuration!!.bundleID = packageName

        subscribeButton.setOnClickListener { onSubscribeToggle() }
    }

    private fun onSubscribeToggle() {
        if (isSubscribing) {
            stop()
        } else {
            start()
        }
        isSubscribing = !isSubscribing
        subscribeButton.text = if (isSubscribing) "stop" else "start"
    }

    private fun start() {
        updateStatus("connecting...")
        stream = R5Stream(R5Connection(configuration))
        stream!!.audioController = R5AudioController()
        stream!!.audioController.sampleRate = 44100
        subscribeView.attachStream(stream)
        subscribeView.showDebugView(false)
        stream!!.play(streamName)

        stream!!.setListener {
            Log.e("TAG", "message : ${it.message},   value: ${it.value()}")
            if (it.message == "Connected") {
                updateStatus("Connected")
            }
            if (it.message == "Started Streaming") {
                updateStatus("Started Streaming")
            }
            if (it.message == "Video Render Start") {
                updateStatus("Video Render Start")
            }
            if (it.message == "No Valid Media Found") {
                updateStatus("No Valid Media Found")
                runOnUiThread {
                    onSubscribeToggle()
                    Toast.makeText(this, "No Valid Media Found", Toast.LENGTH_SHORT).show()
                }
            }
            if (it.message == "host is unreachable") {
                updateStatus("host is unreachable")
                runOnUiThread { onSubscribeToggle() }
                stream!!.removeListener()
            }
            if (it.message == "NetStream.Play.UnpublishNotify") {
                updateStatus("Live Stream Completed")
                runOnUiThread { onSubscribeToggle() }
                stream!!.removeListener()
            }
            if (it.message == "Disconnected") {
                updateStatus("Disconnected")
            }
            if (it.message == "Closed") {
                updateStatus("Closed")
            }
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            status_txt.text = message
        }
    }

    private fun stop() {
        if (stream != null) {
            stream!!.stop()
        }
    }

    override fun onPause() {
        super.onPause()
        if(isSubscribing) {
            onSubscribeToggle();
        }
    }

}