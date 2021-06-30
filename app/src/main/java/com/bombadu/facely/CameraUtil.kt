package com.bombadu.facely

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraUtil: AppCompatActivity(){

    private var imageCapture: ImageCapture? = null
    private lateinit var myOutputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var isBackCamera = true


    fun takePhoto(context: Context) {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            myOutputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CAMERA UTIL", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                   val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    val intent = Intent(baseContext, EditActivity::class.java)
                    intent.putExtra("imageuri", savedUri)
                    startActivity(intent)
                }


            })
    }

    fun startCamera(context: Context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector: CameraSelector = when {
                isBackCamera -> {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                else -> {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
            }


            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    fun checkPermissions(context: Context) {

        //Request Camera Permissions
        if (allPermissionsGranted()) {
            startCamera(context)
        } else {
            Toast.makeText(context, "Permissions not granted by user", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this, MainActivity.REQUIRED_PERMISSIONS, MainActivity.REQUEST_CODE_PERMISSIONS
            )
        }

        myOutputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }



    fun allPermissionsGranted() = MainActivity.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it) == PackageManager.PERMISSION_GRANTED
    }

    fun onRequestPermissionsResult(context: Context,
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MainActivity.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera(context)
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        const val TAG = "CameraXUtil"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val REQUEST_CODE_PERMISSIONS = 10
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}