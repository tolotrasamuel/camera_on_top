package com.tolotra.cameraontop

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.Preview.Builder
import androidx.camera.lifecycle.ProcessCameraProvider

import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.common.util.concurrent.ListenableFuture
import com.tolotra.cameraontop.R
import java.util.concurrent.ExecutionException

class CameraOverlayService : LifecycleService() {
    private var windowManager: WindowManager? = null
    private var cameraView: View? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        cameraView = LayoutInflater.from(this).inflate(R.layout.overlay_camera_view, null)
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params!!.gravity = Gravity.TOP or Gravity.START
        params!!.x = 0
        params!!.y = 0
        windowManager!!.addView(cameraView, params)
        cameraView!!.setOnTouchListener(object : OnTouchListener {
            private var lastAction = 0
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastAction = MotionEvent.ACTION_DOWN
                        initialX = params!!.x
                        initialY = params!!.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        lastAction = MotionEvent.ACTION_MOVE
                        params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                        params!!.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager!!.updateViewLayout(cameraView, params)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            // Handle click event if needed
                        }
                        return true
                    }
                }
                return false
            }
        })


        // Initialize camera
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                bindCameraPreview(cameraProvider)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Builder().build()
        val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        preview.setSurfaceProvider(cameraView!!.findViewById(R.id.camera_preview))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (cameraView != null) {
            windowManager!!.removeView(cameraView)
        }
    }
}
