package com.tolotra.cameraontop

import android.app.Activity
import android.app.PictureInPictureParams
import android.app.PictureInPictureParams.Builder
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.tolotra.cameraontop.databinding.ActivityMainBinding
import java.util.concurrent.ExecutionException


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.fab.setOnClickListener { view ->
            startCamera();
        }

        startCamera()
    }


    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {

        if (!checkCameraPermission()) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 0)
            return
        }

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


        val cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        val previewView: PreviewView = binding.cameraPreview
        // Create a preview use case instance
        val preview = Preview.Builder().build()

        // Bind the preview use case and other needed user cases to a lifecycle
        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)

        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE

        // Create a surfaceProvider using the bound camera's cameraInfo
        val surfaceProvider = previewView.surfaceProvider

        // Attach the surfaceProvider to the preview use case to start preview
        preview.setSurfaceProvider(surfaceProvider)
    }

    protected override fun onUserLeaveHint() {
        val params: PictureInPictureParams = Builder().setAspectRatio(Rational(2, 3)).build()
        enterPictureInPictureMode(params)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            binding.fab.hide()
        } else {
            binding.fab.show()
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        resultLauncher.launch(intent)
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                showOverlay()
            }

        }

    private fun showOverlay() {

        if (checkOverlayPermission()) {
            val intent = Intent(this, CameraOverlayService::class.java)
            startService(intent)
        } else {
            requestOverlayPermission()
        }
    }

}
