package com.task.sm.agorademo


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

class CallActivity : AppCompatActivity() {

    private val APP_ID = "YOUR APP ID"
    private val TOKEN: String? =  "YOUR TOKEN"
    private val CHANNEL_NAME = "YOUR CHANNEL NAME"

    private lateinit var rtcEngine: RtcEngine
    private lateinit var btnMute: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnEndCall: ImageButton
    private lateinit var localContainer: FrameLayout
    private lateinit var remoteContainer: FrameLayout

    private var muted = false
    private var callType = "VIDEO"

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET
    )

    private val PERMISSION_REQ_CODE = 22

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@CallActivity, "Joined channel: $uid >> $channel", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d("CallActivity", "Remote user joined: $uid")
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Log.d("CallActivity", "Remote user offline: $uid  ::: $reason")
                removeRemoteVideo()
            }
        }

        override fun onError(err: Int) {
            super.onError(err)
            Log.d("CallActivity", "user error: $err")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        callType = intent.getStringExtra("CALL_TYPE") ?: "VIDEO"

        btnMute = findViewById(R.id.btnMute)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnEndCall = findViewById(R.id.btnEndCall)
        localContainer = findViewById(R.id.local_video_container)
        remoteContainer = findViewById(R.id.remote_video_container)

        if (callType == "AUDIO") {
            btnSwitchCamera.visibility = View.GONE
            localContainer.visibility = View.GONE
        }

        if (hasPermissions()) {
            initAgoraEngine()
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_CODE)
        }

        btnMute.setOnClickListener {
            muted = !muted
            rtcEngine.muteLocalAudioStream(muted)
            btnMute.setImageResource(if (muted) R.drawable.ic_mic_off else R.drawable.ic_mic)
        }

        btnSwitchCamera.setOnClickListener {
            rtcEngine.switchCamera()
        }

        btnEndCall.setOnClickListener {
            finish()
        }
    }

    private fun hasPermissions(): Boolean {
        return PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initAgoraEngine()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun initAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(this, APP_ID, rtcEventHandler)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Check Agora SDK initialization.")
        }

        rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        rtcEngine.enableAudio()

        if (callType == "VIDEO") {
            rtcEngine.enableVideo()
            setupLocalVideo()
            rtcEngine.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
                )
            )
            rtcEngine.startPreview()
        } else {
            rtcEngine.disableVideo()
        }

        // For testing: Use token = null if App Certificate is off on Agora dashboard
        val tokenToUse = TOKEN ?: null

        val localUid = (System.currentTimeMillis() / 1000L % Int.MAX_VALUE).toInt()
        Log.d("CallActivity", "Joining channel with uid = $localUid and token = $tokenToUse")
        rtcEngine.joinChannel(tokenToUse, CHANNEL_NAME, "", localUid)
    }



    private fun setupLocalVideo() {
        val localVideoView = VideoView(this)
        localContainer.removeAllViews()
        localContainer.addView(localVideoView)
        rtcEngine.setupLocalVideo(VideoCanvas(localVideoView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        localVideoView.setZOrderMediaOverlay(true)
    }

    private fun setupRemoteVideo(uid: Int) {
        val remoteVideoView = VideoView(this)
        remoteContainer.removeAllViews()
        remoteContainer.addView(remoteVideoView)
        rtcEngine.setupRemoteVideo(VideoCanvas(remoteVideoView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    private fun removeRemoteVideo() {
        remoteContainer.removeAllViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        rtcEngine.leaveChannel()
        RtcEngine.destroy()
    }
}

