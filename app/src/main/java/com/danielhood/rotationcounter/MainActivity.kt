package com.danielhood.rotationcounter

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.danielhood.rotationcounter.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ****************
// Based on https://github.com/android/camera-samples/blob/main/CameraX-MLKit/app/src/main/java/com/example/camerax_mlkit/MainActivity.kt
// ****************

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var bitmapBuffer: Bitmap
    private var imageRotationDegrees: Int = 0

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() = viewBinding.viewFinder.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val previewView: PreviewView = viewBinding.viewFinder

        val rotationCountStats = RotationCountStats()
        val fpsStats = FpsStats()
        val viewTarget = ViewTarget(
            0,
            640,
            480,
            640,
            480
        )

        var resetTarget: Boolean = false

        cameraProviderFuture.addListener({
            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            // Set up the view finder use case to display camera preview
            val preview = Preview.Builder()
                .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build())
                .setTargetRotation(viewBinding.viewFinder.display.rotation)
                //.setTargetRotation(Surface.ROTATION_0)
                .build()

            previewView.setOnTouchListener { v: View, e: MotionEvent ->
                when (e.action) {
                    MotionEvent.ACTION_UP -> {
                        resetTarget = true
                    }
                    MotionEvent.ACTION_DOWN -> {
                        viewTarget.updateCoordinates(e.x.toInt(), e.y.toInt())
                    }
                    MotionEvent.ACTION_MOVE -> {
                        viewTarget.updateCoordinates(e.x.toInt(), e.y.toInt())
                    }
                }

                v.performClick()
                true // return true from the callback to signify the event was handled
            }

            // Set up the image analysis use case which will process frames in real time
            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build())
                .setTargetRotation(viewBinding.viewFinder.display.rotation)
                //.setTargetRotation(Surface.ROTATION_0)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()


            val counterViewModel = CounterViewModel()
            val counterDrawable = CounterDrawable(counterViewModel)

            previewView.overlay.clear()
            previewView.overlay.add(counterDrawable)

            imageAnalysis.setAnalyzer(cameraExecutor) { image ->
                if (!::bitmapBuffer.isInitialized) {
                    // The image rotation and RGB image buffer are initialized only once
                    // the analyzer has started running
                    imageRotationDegrees = image.imageInfo.rotationDegrees

                    bitmapBuffer = Bitmap.createBitmap(
                        image.width, image.height, Bitmap.Config.ARGB_8888
                    )

                    Log.d(TAG, "viewRotation: ${viewBinding.viewFinder.display.rotation}, imageRotation: ${imageRotationDegrees}, image(${image.width},${image.height}), view(${viewBinding.viewFinder.width},${viewBinding.viewFinder.height})")

                    viewTarget.updateImageAndViewSize(
                        imageRotationDegrees,
                        image.width,
                        image.height,
                        viewBinding.viewFinder.width,
                        viewBinding.viewFinder.height
                    )

                    viewTarget.updateCoordinates(
                    viewBinding.viewFinder.width/2,
                    viewBinding.viewFinder.height/2
                    )
                }

                // Copy out RGB bits to our shared buffer
                image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

                if (resetTarget) {
                    // Reset rotation count
                    rotationCountStats.rotationCount = 0

                    Log.d(TAG, "scaledTarget(${viewTarget.xScaled},${viewTarget.yScaled})")
                    //viewTarget.setTargetColor(bitmapBuffer.getColor(viewTarget.xScaled, viewTarget.yScaled))
                    viewTarget.setTargetColor(getAverageColorFromBuffer(bitmapBuffer, viewTarget))

                    Log.d(TAG, "targetColor(${viewTarget.targetColor.red()},${viewTarget.targetColor.green()},${viewTarget.targetColor.blue()}) at (${viewTarget.xScaled},${viewTarget.yScaled})")

                    resetTarget = false
                } else {

                    // TODO: analyze image
                    //Log.d(TAG, "target(${viewTarget.xScaled},${viewTarget.yScaled})")
                    //val color = bitmapBuffer.getColor(viewTarget.xScaled, viewTarget.yScaled)
                    //Log.d(TAG, "color(${color.red()},${color.green()},${color.blue()})")
                }

                // Update counter overlay
                counterViewModel.rotationCount = rotationCountStats.rotationCount++
                counterViewModel.currentFps = rotationCountStats.lastComputedFps
                counterViewModel.targetX = viewTarget.x
                counterViewModel.targetY = viewTarget.y

                previewView.overlay.clear()
                previewView.overlay.add(counterDrawable)

                // Tick frame
                fpsStats.frameTick(rotationCountStats)
            }

            // Create a new camera selector each time, enforcing lens facing
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            // Apply declared configs to CameraX using the same lifecycle owner
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, preview, imageAnalysis)

            // Use the camera object to link our preview use case with the view
            preview.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getAverageColorFromBuffer(bitmapBuffer: Bitmap, viewTarget: ViewTarget): Color {
        var r: Float = 0f
        var g: Float = 0f
        var b: Float = 0f

        for (x in viewTarget.xScaled-20..viewTarget.xScaled+20){
            for (y in viewTarget.yScaled-20..viewTarget.yScaled+20) {
                var color = bitmapBuffer.getColor(viewTarget.xScaled, viewTarget.yScaled)
                r = (r + color.red()) / 2
                g = (g + color.green()) / 2
                b = (b + color.blue()) / 2
            }
        }

        return Color.valueOf(r, g, b)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "CameraX-MLKit"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}