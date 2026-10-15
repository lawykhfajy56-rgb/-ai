package com.cloudy.ai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CapturePermissionActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE = 12345
        const val ACTION_RESULT = "com.cloudy.ai.CAPTURE_RESULT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = getSystemService(ACTIVITY_SERVICE) as android.media.projection.MediaProjectionManager
        startActivityForResult(pm.createScreenCaptureIntent(), REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val b = Intent(ACTION_RESULT)
        b.putExtra("resultCode", resultCode)
        b.putExtra("resultData", data)
        sendBroadcast(b)
        finish()
    }
}
