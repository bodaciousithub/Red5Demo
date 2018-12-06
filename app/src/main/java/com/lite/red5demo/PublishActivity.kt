package com.lite.red5demo

import android.content.Context
import android.graphics.Rect
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import com.red5pro.streaming.config.R5Configuration
import com.red5pro.streaming.R5StreamProtocol
import com.red5pro.streaming.R5Stream
import kotlinx.android.synthetic.main.activity_publish.*
import com.red5pro.streaming.R5Stream.RecordType
import com.red5pro.streaming.source.R5Microphone
import com.red5pro.streaming.source.R5Camera
import com.red5pro.streaming.R5Connection

class PublishActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private var configuration: R5Configuration? = null
    private var cam: Camera? = null
    private var isPublishing = false
    private var stream: R5Stream? = null
    private var camOrientation: Int = 0         // Used for camera's orientation
    private var micEnable: Boolean = true       //change Mic flag according to requirement

    private var serverAddress: String? = null
    private var serverPort: String? = null
    private var streamName: String? = null
    private var selectedDimension: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publish)

        serverAddress = intent.getStringExtra("server")
        serverPort = intent.getStringExtra("port")
        streamName = intent.getStringExtra("stream")
        selectedDimension = intent.getIntExtra("dimension", 1)

        configuration = R5Configuration(R5StreamProtocol.RTSP, serverAddress!!, 8554, "live", 0.5f)
        configuration!!.licenseKey = "HF4E-TAIB-DZJG-PGV7"
        configuration!!.bundleID = packageName

        publishButton.setOnClickListener { onPublishToggle() }
    }

    private fun onPublishToggle() {
        if (isPublishing) {
            stop()
        } else {
            start()
        }
        isPublishing = !isPublishing
        publishButton.text = if (isPublishing) "stop" else "start"
    }

    private fun start() {
        updateStatus("connecting...")
        cam!!.stopPreview()

        stream = R5Stream(R5Connection(configuration))
        stream!!.audioController.sampleRate = 44100
        stream!!.setView(surfaceView)
        if (micEnable) {
            val r5Microphone = R5Microphone()
            stream!!.attachMic(r5Microphone)
        }

        /*
        * 1920*1080 -- 4500(bitrate)
        * 640*480 --  1000(bitrate)
        * 640*360 -- 750(bitrate)
        * */

        var width = 0
        var height = 0
        var bitrate = 0
        if (selectedDimension == 1) {
            width = 1920
            height = 1080
            bitrate = 4500
        } else if (selectedDimension == 2) {
            width = 640
            height = 480
            bitrate = 1000
        } else if (selectedDimension == 3) {
            width = 640
            height = 360
            bitrate = 750
        }
        val r5Camera = R5Camera(cam, width, height)
        r5Camera.bitrate = bitrate
        r5Camera.orientation = camOrientation
        r5Camera.framerate = 15
        stream!!.attachCamera(r5Camera)
        stream!!.publish(streamName, RecordType.Live)
        stream!!.setListener {
            Log.e("TAG", "message : ${it.message},    value : ${it.value()}")
            if (it.message == "Connected") {
                updateStatus("Connected")
            }
            if (it.message == "host is unreachable") {
                updateStatus("host is unreachable")
                runOnUiThread { onPublishToggle() }
                stream!!.removeListener()
            }
            if (it.message == "Disconnected") {
                updateStatus("Disconnected")
            }
            if (it.message == "Closed") {
                updateStatus("Closed")
                stream!!.removeListener()
            }
        }

        cam!!.startPreview()
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            status_txt.text = message
        }
    }

    override fun onBackPressed() {
        finish()
    }

    private fun stop() {
        if (stream != null) {
            stream!!.removeListener()
            stream!!.stop()
            cam!!.stopPreview()
            updateStatus("Disconnected!!")
        }
    }

    override fun onResume() {
        super.onResume()
        preview()
    }

    override fun onPause() {
        super.onPause()
        if(isPublishing) {
            onPublishToggle()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cam!!.release()
    }

    private fun preview() {
        cam = openFrontFacingCamera()
        surfaceView.holder.addCallback(this)
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {}
    override fun surfaceDestroyed(p0: SurfaceHolder?) {}
    override fun surfaceCreated(surfaceHolder: SurfaceHolder?) {
        try {
            val parameters = cam!!.parameters
            if (parameters.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            } else {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
            }
            //cam!!.setDisplayOrientation((camOrientation + 180) % 360)         // Used for Front_Face_Camera
            cam!!.setDisplayOrientation(90)                                     // Used for Back_Face_Camera
            cam!!.parameters = parameters
            cam!!.setPreviewDisplay(surfaceHolder)
            cam!!.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openFrontFacingCamera(): Camera? {
        var cameraCount = 0
        var cam: Camera? = null
        val cameraInfo = Camera.CameraInfo()
        cameraCount = Camera.getNumberOfCameras()
        for (camIdx in 0 until cameraCount) {
            Camera.getCameraInfo(camIdx, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    cam = Camera.open(camIdx)
                    camOrientation = cameraInfo.orientation
                    applyDeviceRotation()
                    break
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                }
            }
        }
        return cam
    }

    private fun applyDeviceRotation() {
        val window = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val rotation = window.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 270
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 90
        }

        val screenSize = Rect()
        window.defaultDisplay.getRectSize(screenSize)
        val screenAR = screenSize.width() * 1.0f / (screenSize.height() * 1.0f)
        if (screenAR > 1 && degrees % 180 == 0 || screenAR < 1 && degrees % 180 > 0)
            degrees += 180

        camOrientation += degrees
        camOrientation %= 360
    }

}
