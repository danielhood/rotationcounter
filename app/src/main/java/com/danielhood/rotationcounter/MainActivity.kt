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
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
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

    @OptIn(ExperimentalCamera2Interop::class) private fun startCamera() = viewBinding.viewFinder.post {
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

        var resetTarget = false

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

                        viewTarget.setTargetColor(getAverageColorFromBuffer(bitmapBuffer, viewTarget))
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

                    rotationCountStats.reset()
                }

                // Copy out RGB bits to our shared buffer
                image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

                if (resetTarget) {
                    rotationCountStats.reset()
                    resetTarget = false
                } else {
                    val color = getAverageColorFromBuffer(bitmapBuffer, viewTarget)
                    rotationCountStats.update(colorMatch(color, viewTarget.targetColor))
                }

                // Update counter overlay
                counterViewModel.rotationCount = rotationCountStats.rotationCount
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
            val camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, preview, imageAnalysis)

            val meteringFactory = SurfaceOrientedMeteringPointFactory(
                viewBinding.viewFinder.width.toFloat(),
                viewBinding.viewFinder.height.toFloat()
            )
            val point = meteringFactory.createPoint(viewBinding.viewFinder.width.toFloat()/2, viewBinding.viewFinder.height.toFloat()/2)
            val autoFocusActon = FocusMeteringAction.Builder(point,
                FocusMeteringAction.FLAG_AF
                        or FocusMeteringAction.FLAG_AE
                        or FocusMeteringAction.FLAG_AWB)
                .apply {
                    disableAutoCancel() // focus once on launch/rotate
                }.build()
            camera.cameraControl.startFocusAndMetering(autoFocusActon)

            // Use the camera object to link our preview use case with the view
            preview.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun colorMatch(color: Color, targetColor: Color): Boolean {
        val colorTolerance = 0.2f

        return (
                color.red() in targetColor.red()-colorTolerance..targetColor.red()+colorTolerance
                        && color.green() in targetColor.green()-colorTolerance..targetColor.green()+colorTolerance
                        && color.blue() in targetColor.blue()-colorTolerance..targetColor.blue()+colorTolerance
                )
    }

    private fun getAverageColorFromBuffer(bitmapBuffer: Bitmap, viewTarget: ViewTarget): Color {
        var r = 0f
        var g = 0f
        var b = 0f

        for (x in viewTarget.xScaled-viewTarget.bufferX ..viewTarget.xScaled+viewTarget.bufferX){
            for (y in viewTarget.yScaled-viewTarget.bufferY..viewTarget.yScaled+viewTarget.bufferY) {
                val color = bitmapBuffer.getColor(viewTarget.xScaled, viewTarget.yScaled)
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