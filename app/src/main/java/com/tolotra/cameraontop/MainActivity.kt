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
import android.util.DisplayMetrics
import android.util.Rational
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.tolotra.cameraontop.databinding.ActivityMainBinding
import java.util.concurrent.ExecutionException


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.fab.setOnClickListener { view ->
//            showOverlay();
            startCamera();
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
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

//    private var mApplication: MyApplication? = null

//
//    private fun saveDpi() {
//        val configuration: Configuration = getResources().getConfiguration()
//        mApplication.orgDensityDpi = configuration.densityDpi
//    }
//
//    private fun setDpi() {
//        val configuration: Configuration = getResources().getConfiguration()
//        val metrics: DisplayMetrics = getResources().getDisplayMetrics()
//        if (mApplication.mode === MyApplication.MODE_PIP) {
//            configuration.densityDpi = mApplication.orgDensityDpi / 3
//        } else {
//            configuration.densityDpi = mApplication.orgDensityDpi
//        }
//        getBaseContext().getResources().updateConfiguration(configuration, metrics)
//    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {


        val cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        val previewView: PreviewView = binding.cameraPreview
        // Create a preview use case instance
        val preview = Preview.Builder().build()

        // Bind the preview use case and other needed user cases to a lifecycle
        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)

//        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE

        // Create a surfaceProvider using the bound camera's cameraInfo
        val surfaceProvider = previewView.surfaceProvider

        // Attach the surfaceProvider to the preview use case to start preview
        preview.setSurfaceProvider(surfaceProvider)
        // start the camera

//        camera
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
//        if (isInPictureInPictureMode)
        } else {
            binding.fab.show()
        }
//            mApplication.mode = MyApplication.MODE_PIP
//        } else {
//            mApplication.mode = MyApplication.MODE_FULL
//        }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
