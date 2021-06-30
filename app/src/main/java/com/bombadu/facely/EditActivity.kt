package com.bombadu.facely

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.android.synthetic.main.activity_edit2.*
import java.io.IOException

class EditActivity : AppCompatActivity() {

    lateinit var image: InputImage


    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit2)

        val imageUri = intent.getParcelableExtra<Uri>("imageuri")

        imageView.setImageURI(imageUri)

        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        // Real-time contour detection
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        magicButton.setOnClickListener {
            processImage(imageUri)
        }



    }

    private fun processImage(imageUri: Uri?) {

        try {
            image = InputImage.fromFilePath(this, imageUri!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val detector = FaceDetection.getClient(options)

        val result = detector.process(image)
            .addOnSuccessListener { faces ->

                for (face in faces) {
                    val bounds = face.boundingBox
                    val roty = face.headEulerAngleY
                    val rotz = face.headEulerAngleZ

                    //If landmark detection was enabled (mouth, ears, eys, cheeks and nose available)

                    val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
                    val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)
                    leftEar?.let {
                        val leftEarPos = leftEar.position
                    }

                    noseBase?.let {
                        val nosePosition = noseBase.position
                    }

                    //if classification was enabled
                    if (face.smilingProbability != null) {
                        val smileProb = face.smilingProbability
                        showDialog(smileProb!!)
                        println("Smile Prob: $smileProb")
                    }

                    if (face.rightEyeOpenProbability != null) {
                        val rightEyeOpenProb = face.rightEyeOpenProbability
                        println("Right Eye Open Prob: $rightEyeOpenProb")
                    }

                    //if face tracking was enabled
                    if (face.trackingId != null) {
                        val id = face.trackingId
                        println("Id: $id")
                    }
                }

            }

            .addOnFailureListener { e ->
                Log.e(TAG, "Face Detection Failed")
                Toast.makeText(this, "No Face Detected", Toast.LENGTH_SHORT).show()

            }

    }

    private fun showDialog(smile: Float) {
        val smileDialog = AlertDialog.Builder(this)

        if (smile > 0.50f) {
            smileDialog.setTitle("You seem happy")
            smileDialog.setMessage("Good for you!")
            smileDialog.setIcon(R.drawable.happyemoji)
        } else {
            smileDialog.setTitle("You seem un-happy")
            smileDialog.setMessage("Cheer up!")
            smileDialog.setIcon(R.drawable.frownemoji128)
        }

        smileDialog.show()
    }

    companion object {
        val TAG = EditActivity::class.java.simpleName
    }
}