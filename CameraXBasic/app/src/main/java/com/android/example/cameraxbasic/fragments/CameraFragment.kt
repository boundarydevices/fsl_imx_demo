/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.example.cameraxbasic.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import com.android.example.cameraxbasic.KEY_EVENT_ACTION
import com.android.example.cameraxbasic.KEY_EVENT_EXTRA
import com.android.example.cameraxbasic.MainActivity
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.utils.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
//import kotlinx.android.synthetic.main.camera_ui_container.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit

/**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo taking
 * - Image analysis
 */
class CameraFragment : Fragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: PreviewView
    private lateinit var outputDirectory: File
    private lateinit var broadcastManager: LocalBroadcastManager

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var awbState = CameraMetadata.CONTROL_AWB_MODE_AUTO
    private var aecValue = 0
    private var enableVflip = true
    private var enableHflip = true
    private var enableDewarp = true
    private var mVflip = 0
    private var mHflip = 0
    private var mDewarp = 0

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    /** Volume down button receiver used to trigger shutter */
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val shutter = container
                            .findViewById<ImageButton>(R.id.camera_capture_button)
                    shutter.simulateClick()
                }
            }
        }
    }

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    CameraFragmentDirections.actionCameraToPermissions()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()

        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_camera, container, false)

    private fun setGalleryThumbnail(uri: Uri) {
        // Reference of the view that holds the gallery thumbnail
        val thumbnail = container.findViewById<ImageButton>(R.id.photo_view_button)

        // Run the operations in the view's thread
        thumbnail.post {

            // Remove thumbnail padding
            thumbnail.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())

            // Load thumbnail into circular button using Glide
            Glide.with(thumbnail)
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(thumbnail)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.view_finder)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        // Set up the intent filter that will receive events from our main activity
        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        broadcastManager.registerReceiver(volumeDownReceiver, filter)

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

        // Determine the output directory
        outputDirectory = MainActivity.getOutputDirectory(requireContext())

        // Wait for the views to be properly laid out
        viewFinder.post {

            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId

            // Build UI controls
            updateCameraUi()

            // Set up the camera and its use cases
            setUpCamera()
        }

        // This swipe gesture adds a fun gesture to switch between video and photo
        val swipeGestures = SwipeGestureDetector().apply {
            setSwipeCallback(right = {
                Navigation.findNavController(view).navigate(R.id.action_camera_to_video)
            })
        }
        val gestureDetectorCompat = GestureDetector(requireContext(), swipeGestures)
        view.setOnTouchListener { _, motionEvent ->
            if (gestureDetectorCompat.onTouchEvent(motionEvent)) return@setOnTouchListener false
            return@setOnTouchListener true
        }
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Redraw the camera UI controls
        updateCameraUi()

        updateVflipSwitchButton()
        updateHflipSwitchButton()
        updateDewarpSwitchButton()

        updateAecSwitchButton()
        updateAwbSwitchButton()

        // Enable or disable switching between cameras
        updateCameraSwitchButton()
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            updateVflipSwitchButton()
            updateHflipSwitchButton()
            updateDewarpSwitchButton()

            updateAecSwitchButton()
            updateAwbSwitchButton()

            // Enable or disable switching between cameras
            updateCameraSwitchButton()

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /** Declare and bind preview, capture and analysis use cases */
    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError")
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        val DEWARP_ENABLE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CaptureRequest.Key("vsi.dewarp.enable", Int::class.java)
        } else {
            TODO("VERSION.SDK_INT < Q")
        }
        val HFLIP_ENABLE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CaptureRequest.Key("vsi.hflip.enable", Int::class.java)
        } else {
            TODO("VERSION.SDK_INT < Q")
        }
        val VFLIP_ENABLE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CaptureRequest.Key("vsi.vflip.enable", Int::class.java)
        } else {
            TODO("VERSION.SDK_INT < Q")
        }

        val imxCapReqOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(VFLIP_ENABLE, mVflip)
                .setCaptureRequestOption(HFLIP_ENABLE, mHflip)
                .setCaptureRequestOption(DEWARP_ENABLE, mDewarp)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, aecValue)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, awbState)
                .build()

        // Preview
        preview = Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .setCameraSelector(cameraSelector)
                .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

        // ImageAnalysis
        imageAnalyzer = ImageAnalysis.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    if (it != null) {
                        it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                            // Values returned from our analyzer are passed to the attached listener
                            // We log image analysis results here - you should do something useful
                            // instead!
                            // Log.d(TAG, "Average luminosity: $luma")
                        })
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)

            Camera2CameraControl.from(camera!!.cameraControl).setCaptureRequestOptions(imxCapReqOptions)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {

        // Remove previous UI if any
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        // Inflate a new view containing all UI for controlling the camera
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)

        // In the background, load latest photo taken (if any) for gallery thumbnail
        lifecycleScope.launch(Dispatchers.IO) {
            outputDirectory.listFiles { file ->
                EXTENSION_WHITELIST.contains(file.extension.toUpperCase(Locale.ROOT))
            }?.maxOrNull()?.let {
                setGalleryThumbnail(Uri.fromFile(it))
            }
        }

        // Listener for button used to capture photo
        controls.findViewById<ImageButton>(R.id.camera_capture_button).setOnClickListener {

            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->

                // Create output file to hold the image
                val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)

                // Setup image capture metadata
                val metadata = Metadata().apply {

                    // Mirror image when using the front camera
                    //isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }

                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                        .setMetadata(metadata)
                        .build()

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(
                        outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        Log.d(TAG, "Photo capture succeeded: $savedUri")

                        // We can only change the foreground Drawable using API level 23+ API
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Update the gallery thumbnail with latest picture taken
                            setGalleryThumbnail(savedUri)
                        }

                        // Implicit broadcasts will be ignored for devices running API level >= 24
                        // so if you only target API level 24+ you can remove this statement
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            requireActivity().sendBroadcast(
                                    Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                            )
                        }

                        // If the folder selected is an external media directory, this is
                        // unnecessary but otherwise other apps will not be able to access our
                        // images unless we scan them using [MediaScannerConnection]
                        val mimeType = MimeTypeMap.getSingleton()
                                .getMimeTypeFromExtension(savedUri.toFile().extension)
                        MediaScannerConnection.scanFile(
                                context,
                                arrayOf(savedUri.toFile().absolutePath),
                                arrayOf(mimeType)
                        ) { _, uri ->
                            Log.d(TAG, "Image capture scanned into media store: $uri")
                        }
                    }
                })

                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    // Display flash animation to indicate that photo was captured
                    container.postDelayed({
                        container.foreground = ColorDrawable(Color.WHITE)
                        container.postDelayed(
                                { container.foreground = null }, ANIMATION_FAST_MILLIS)
                    }, ANIMATION_SLOW_MILLIS)
                }
            }
        }

        // Setup for button used to switch dewarp
        controls.findViewById<ImageButton>(R.id.dewarp_switch_button).let {

            // Disable the button until the camera is set up
            it.isEnabled = false

            // Listener for button used to switch awb mode. Only called if the button is enabled
            it.setOnClickListener {
                enableDewarp = !enableDewarp
                mDewarp = if (enableDewarp) 1 else 0
                Toast.makeText(requireContext(), "DEWARP  is : " + enableDewarp, Toast.LENGTH_SHORT).show()

                // Re-bind use cases to update selected camera
                bindCameraUseCases()
            }
        }

        // Setup for button used to switch vflip
        controls.findViewById<ImageButton>(R.id.vflip_switch_button).let {

            // Disable the button until the camera is set up
            it.isEnabled = false

            // Listener for button used to switch awb mode. Only called if the button is enabled
            it.setOnClickListener {
                enableVflip = !enableVflip
                mVflip = if (enableVflip) 1 else 0
                Toast.makeText(requireContext(), "VFLIP  is : " + enableVflip, Toast.LENGTH_SHORT).show()

                // Re-bind use cases to update selected camera
                bindCameraUseCases()
            }
        }

        // Setup for button used to switch hflip
        controls.findViewById<ImageButton>(R.id.hflip_switch_button).let {

            // Disable the button until the camera is set up
            it.isEnabled = false

            // Listener for button used to switch awb mode. Only called if the button is enabled
            it.setOnClickListener {
                enableHflip = !enableHflip
                mHflip = if (enableHflip) 1 else 0
                Toast.makeText(requireContext(), "VFLIP  is : " + enableHflip, Toast.LENGTH_SHORT).show()

                // Re-bind use cases to update selected camera
                bindCameraUseCases()
            }
        }

        // Setup for button used to switch aec
        controls.findViewById<ImageButton>(R.id.aec_switch_button).let {

            // Disable the button until the camera is set up
            it.isEnabled = false

            // Listener for button used to switch awb mode. Only called if the button is enabled
            it.setOnClickListener {
                aecValue = (-4..4).random()
                Toast.makeText(requireContext(), "EXPOSURE VALUE is : " + aecValue/2, Toast.LENGTH_SHORT).show()

                // Re-bind use cases to update selected camera
                bindCameraUseCases()
            }
        }

        // Setup for button used to switch awb
        controls.findViewById<ImageButton>(R.id.awb_switch_button).let {

            // Disable the button until the camera is set up
            it.isEnabled = false

            // Listener for button used to switch awb mode. Only called if the button is enabled
            it.setOnClickListener {
                if (awbState == CameraMetadata.CONTROL_AWB_MODE_OFF) {
                    awbState = CameraMetadata.CONTROL_AWB_MODE_AUTO
                    Toast.makeText(requireContext(), "AWB MODE is : AUTO", Toast.LENGTH_SHORT).show()
                } else if (awbState == CameraMetadata.CONTROL_AWB_MODE_AUTO) {
                    awbState = CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT
                    Toast.makeText(requireContext(), "AWB MODE is : INCANDESCENT", Toast.LENGTH_SHORT).show()
                } else if (awbState == CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT) {
                    awbState = CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT
                    Toast.makeText(requireContext(), "AWB MODE is : FLUORESCENT", Toast.LENGTH_SHORT).show()
                } else if (awbState == CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT ) {
                    awbState = CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT
                    Toast.makeText(requireContext(), "AWB MODE is : WARM_FLUORESCENT", Toast.LENGTH_SHORT).show()
                } else if (awbState == CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT ) {
                    awbState = CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT
                    Toast.makeText(requireContext(), "AWB MODE is : DAYLIGHT", Toast.LENGTH_SHORT).show()
                } else if (awbState == CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT ) {
                    awbState = CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                    Toast.makeText(requireContext(), "AWB MODE is : CLOUDY_DAYLIGHT", Toast.LENGTH_SHORT).show()
                } else if (awbState == CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT ) {
                    awbState = CameraMetadata.CONTROL_AWB_MODE_TWILIGHT
                    Toast.makeText(requireContext(), "AWB MODE is : TWILIGHT", Toast.LENGTH_SHORT).show()
                } else if (awbState == CameraMetadata.CONTROL_AWB_MODE_TWILIGHT ) {
                    awbState = CameraMetadata.CONTROL_AWB_MODE_SHADE
                    Toast.makeText(requireContext(), "AWB MODE is : SHADE", Toast.LENGTH_SHORT).show()
                } else if (awbState == CameraMetadata.CONTROL_AWB_MODE_SHADE ) {
                    awbState = CameraMetadata.CONTROL_AWB_MODE_AUTO
                    Toast.makeText(requireContext(), "AWB MODE is : OFF", Toast.LENGTH_SHORT).show()
                }

                // Re-bind use cases to update selected camera
                bindCameraUseCases()
            }
        }

        // Setup for button used to switch cameras
        controls.findViewById<ImageButton>(R.id.camera_switch_button).let {

            // Disable the button until the camera is set up
            it.isEnabled = false

            // Listener for button used to switch cameras. Only called if the button is enabled
            it.setOnClickListener {
                lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                // Re-bind use cases to update selected camera
                bindCameraUseCases()
            }
        }

        // Listener for button used to view the most recent photo
        controls.findViewById<ImageButton>(R.id.photo_view_button).setOnClickListener {
            // Only navigate when the gallery has photos
            if (true == outputDirectory.listFiles()?.isNotEmpty()) {
                Navigation.findNavController(
                        requireActivity(), R.id.fragment_container
                ).navigate(CameraFragmentDirections
                        .actionCameraToGallery(outputDirectory.absolutePath))
            }
        }
    }

    /** Enabled or disabled a button to switch cameras Dewarp feature */
    private fun updateDewarpSwitchButton() {
        val switchDewarpButton = container.findViewById<ImageButton>(R.id.dewarp_switch_button)
        try {
            switchDewarpButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            switchDewarpButton.isEnabled = false
        }
    }

    /** Enabled or disabled a button to switch cameras Hflip feature */
    private fun updateHflipSwitchButton() {
        val switchHflipButton = container.findViewById<ImageButton>(R.id.hflip_switch_button)
        try {
            switchHflipButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            switchHflipButton.isEnabled = false
        }
    }

    /** Enabled or disabled a button to switch cameras Vflip feature */
    private fun updateVflipSwitchButton() {
        val switchVflipButton = container.findViewById<ImageButton>(R.id.vflip_switch_button)
        try {
            switchVflipButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            switchVflipButton.isEnabled = false
        }
    }

    /** Enabled or disabled a button to switch cameras Aec feature */
    private fun updateAecSwitchButton() {
        val switchAecButton = container.findViewById<ImageButton>(R.id.aec_switch_button)
        try {
            switchAecButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            switchAecButton.isEnabled = false
        }
    }

    /** Enabled or disabled a button to switch cameras Awb feature */
    private fun updateAwbSwitchButton() {
        val switchAwbButton = container.findViewById<ImageButton>(R.id.awb_switch_button)
        try {
            switchAwbButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            switchAwbButton.isEnabled = false
        }
    }

    /** Enabled or disabled a button to switch cameras depending on the available cameras */
    private fun updateCameraSwitchButton() {
        val switchCamerasButton = container.findViewById<ImageButton>(R.id.camera_switch_button)
        try {
            switchCamerasButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            switchCamerasButton.isEnabled = false
        }
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /**
     * Our custom image analysis class.
     *
     * <p>All we need to do is override the function `analyze` with our desired operations. Here,
     * we compute the average luminosity of the image by looking at the Y plane of the YUV frame.
     */
    private class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set

        /**
         * Used to add listeners that will be called with each luma computed
         */
        fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

        /**
         * Helper extension function used to extract a byte array from an image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        /**
         * Analyzes an image to produce a result.
         *
         * <p>The caller is responsible for ensuring this analysis method can be executed quickly
         * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
         * images will not be acquired and analyzed.
         *
         * <p>The image passed to this method becomes invalid after this method returns. The caller
         * should not store external references to this image, as these references will become
         * invalid.
         *
         * @param image image being analyzed VERY IMPORTANT: Analyzer method implementation must
         * call image.close() on received images when finished using them. Otherwise, new images
         * may not be received or the camera may stall, depending on back pressure setting.
         *
         */
        override fun analyze(image: ImageProxy) {
            // If there are no listeners attached, we don't need to perform analysis
            if (listeners.isEmpty()) {
                image.close()
                return
            }

            // Keep track of frames analyzed
            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)

            // Compute the FPS using a moving average
            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timestampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            // Analysis could take an arbitrarily long amount of time
            // Since we are running in a different thread, it won't stall other use cases

            lastAnalyzedTimestamp = frameTimestamps.first

            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance plane
            val buffer = image.planes[0].buffer

            // Extract image data from callback object
            val data = buffer.toByteArray()

            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }

            // Compute average luminance for the image
            val luma = pixels.average()

            // Call all listeners with new value
            listeners.forEach { it(luma) }

            image.close()
        }
    }

    companion object {

        private const val TAG = "CameraXBasic"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
                File(baseFolder, SimpleDateFormat(format, Locale.US)
                        .format(System.currentTimeMillis()) + extension)
    }
}
