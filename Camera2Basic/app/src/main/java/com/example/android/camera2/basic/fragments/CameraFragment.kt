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

package com.example.android.camera2.basic.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.example.android.camera.utils.AutoFitSurfaceView
import com.example.android.camera.utils.OrientationLiveData
import com.example.android.camera.utils.computeExifOrientation
import com.example.android.camera.utils.getPreviewOutputSize
import com.example.android.camera2.basic.CameraActivity
import com.example.android.camera2.basic.R
import com.example.android.camera2.basic.databinding.FragmentCameraBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.hardware.camera2.CameraCharacteristics
import android.util.Range
import android.widget.CompoundButton

class CameraFragment : Fragment() {
    private var mHflip = 0
    private var mVflip = 0
    private var mDewarp = 0
    private var scene_mode = CaptureRequest.CONTROL_SCENE_MODE_DISABLED
    private var wb_mode = CameraMetadata.CONTROL_AWB_MODE_AUTO

    /** AndroidX navigation arguments */
    private val args: CameraFragmentArgs by navArgs()

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(args.cameraId)
    }

    /** Readers used as buffers for camera still shots */
    private lateinit var imageReader: ImageReader

    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [cameraThread] */
    private val cameraHandler = Handler(cameraThread.looper)

    /** Performs recording animation of flashing screen */
    private val animationTask: Runnable by lazy {
        Runnable {
            // Flash white animation
            overlay.background = Color.argb(150, 255, 255, 255).toDrawable()
            // Wait for ANIMATION_FAST_MILLIS
            overlay.postDelayed({
                // Remove white flash animation
                overlay.background = null
            }, CameraActivity.ANIMATION_FAST_MILLIS)
        }
    }

    /** [HandlerThread] where all buffer reading operations run */
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }

    /** [Handler] corresponding to [imageReaderThread] */
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    /** Where the camera preview is displayed */
    private lateinit var viewFinder: AutoFitSurfaceView

    /** Overlay on top of the camera preview */
    private lateinit var overlay: View

    /** The [CameraDevice] that will be opened in this fragment */
    private lateinit var camera: CameraDevice

    /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters */
    private lateinit var session: CameraCaptureSession

    /** Live data listener for changes in the device orientation relative to the camera */
    private lateinit var relativeOrientation: OrientationLiveData

    private lateinit var outputDirectory: File

    private var _binding: FragmentCameraBinding? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCameraBinding.inflate(inflater,container,false)
        return _binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        overlay = view.findViewById(R.id.overlay)
        viewFinder = view.findViewById(R.id.view_finder)

        outputDirectory = CameraActivity.getOutputDirectory(requireContext())

        _binding?.captureButton?.setOnApplyWindowInsetsListener { v, insets ->
            v.translationX = (-insets.systemWindowInsetRight).toFloat()
            v.translationY = (-insets.systemWindowInsetBottom).toFloat()
            insets.consumeSystemWindowInsets()
        }

        viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

            override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {

                // Selects appropriate preview size and configures view finder
                val previewSize = getPreviewOutputSize(
                        viewFinder.display, characteristics, SurfaceHolder::class.java)
                Log.d(TAG, "View finder size: ${viewFinder.width} x ${viewFinder.height}")
                Log.d(TAG, "Selected preview size: $previewSize")
                viewFinder.setAspectRatio(previewSize.width, previewSize.height)

                // To ensure that size is set, initialize camera in the view's thread
                view.post { initializeCamera() }
            }
        })

        // Used to rotate the output media to match device orientation
        relativeOrientation = OrientationLiveData(requireContext(), characteristics).apply {
            observe(viewLifecycleOwner, Observer {
                orientation -> Log.d(TAG, "Orientation changed: $orientation")
            })
        }
    }

    /**
     * Begin all camera operations in a coroutine in the main thread. This function:
     * - Opens the camera
     * - Configures the camera session
     * - Starts the preview by dispatching a repeating capture request
     * - Sets up the still image capture listeners
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
        // Open the selected camera
        camera = openCamera(cameraManager, args.cameraId, cameraHandler)

        // Initialize an image reader which will be used to capture still photos
        val size = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                .getOutputSizes(args.pixelFormat).maxByOrNull { it.height * it.width }!!
        imageReader = ImageReader.newInstance(
                size.width, size.height, args.pixelFormat, IMAGE_BUFFER_SIZE)

        val cap_list = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        var raw_cap = false
        if (cap_list != null) {
            if (cap_list.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW))
                raw_cap = true
        }

        // Creates list of Surfaces where the camera will output frames
        val targets = listOf(viewFinder.holder.surface, imageReader.surface)

        // Start a capture session using our open camera and list of Surfaces where frames will go
        session = createCaptureSession(camera, targets, cameraHandler)

        val captureRequest = camera.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(viewFinder.holder.surface) }

        captureRequest.set(CaptureRequest.CONTROL_SCENE_MODE, scene_mode)
        captureRequest.set(CaptureRequest.CONTROL_AWB_MODE, wb_mode)
        Log.i(TAG,"preview, set scene mode ${scene_mode}, wb mode ${wb_mode}")

        // This will keep sending the capture request as frequently as possible until the
        // session is torn down or session.stopRepeating() is called
        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

        // Listen to the capture button
        _binding?.captureButton?.setOnClickListener {

            // Disable click listener to prevent multiple requests simultaneously in flight
            it.isEnabled = false

            // Perform I/O heavy operations in a different scope
            lifecycleScope.launch(Dispatchers.IO) {
                takePhoto().use { result ->
                    Log.d(TAG, "Result received: $result")

                    // Save the result to disk
                    val output = saveResult(result)
                    Log.d(TAG, "Image saved: ${output.absolutePath}")

                    // If the result is a JPEG file, update EXIF metadata with orientation info
                    if (output.extension == "jpg") {
                        val exif = ExifInterface(output.absolutePath)
                        exif.setAttribute(
                                ExifInterface.TAG_ORIENTATION, result.orientation.toString())
                        exif.saveAttributes()
                        Log.d(TAG, "EXIF metadata saved: ${output.absolutePath}")
                    }

                    // Display the photo taken to user
                    lifecycleScope.launch(Dispatchers.Main) {
                        navController.navigate(CameraFragmentDirections
                                .actionCameraToJpegViewer(output.absolutePath)
                                .setOrientation(result.orientation)
                                .setDepth(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                        result.format == ImageFormat.DEPTH_JPEG))
                    }
                }

                // Re-enable click listener after photo is taken
                it.post { it.isEnabled = true }
            }
        }

        if (raw_cap) {
            /* WB process */
            _binding?.AWB?.setOnClickListener {
                SetWB(captureRequest, CameraMetadata.CONTROL_AWB_MODE_AUTO)
            }

            _binding?.INCANDESCENT?.setOnClickListener {
                SetWB(captureRequest, CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT)
            }

            _binding?.FLUORESCENT?.setOnClickListener {
                SetWB(captureRequest, CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT)
            }

            _binding?.WARMFLUORESCENT?.setOnClickListener {
                SetWB(captureRequest, CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT)
            }

            _binding?.DAYLIGHT?.setOnClickListener {
                SetWB(captureRequest, CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT)
            }

            _binding?.CLOUDYDAYLIGHT?.setOnClickListener {
                SetWB(captureRequest, CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT)
            }

            _binding?.TWILIGHT?.setOnClickListener {
                SetWB(captureRequest, CameraMetadata.CONTROL_AWB_MODE_TWILIGHT)
            }

            /* hflip/vflip/dewarp */
            _binding?.hflip?.setOnClickListener {
                val HFLIP_ENABLE = CaptureRequest.Key("vsi.hflip.enable", Int::class.java)
                mHflip = 1 - mHflip
                captureRequest.set(HFLIP_ENABLE, mHflip)
                session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

            }

            _binding?.vflip?.setOnClickListener {
                val VFLIP_ENABLE = CaptureRequest.Key("vsi.vflip.enable", Int::class.java)
                mVflip = 1 - mVflip
                captureRequest.set(VFLIP_ENABLE, mVflip)
                session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

            }

            _binding?.dewarp?.setOnClickListener {
                val DEWARP_ENABLE = CaptureRequest.Key("vsi.dewarp.enable", Int::class.java)
                mDewarp = 1 - mDewarp
                captureRequest.set(DEWARP_ENABLE, mDewarp)
                session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
            }

            /* exposure gain */
            val supportedExposure = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            if (supportedExposure != null) {
                val gainRange: Range<Int> = supportedExposure
                Log.i(TAG, "exposure gain range $gainRange")
                _binding?.exposureGain?.setMin(gainRange.lower)
                _binding?.exposureGain?.setMax(gainRange.upper)

                _binding?.exposureGain?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        val expGain = progress
                        captureRequest.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, expGain)
                        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                    }

                })
            }

            /* exposure time */
            val supportedExposureTime = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            if (supportedExposureTime != null) {
                val timeRange: Range<Long> = supportedExposureTime
                Log.i(TAG, "exposure time range $timeRange")

                _binding?.exposureTime?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        val expTime =
                                timeRange.lower + (timeRange.upper - timeRange.lower) * progress / 100
                        captureRequest.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime)
                        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

                        val left = _binding?.exposureTime?.getLeft()
                        val right = _binding?.exposureTime?.getRight()
                        if ((left != null) && (right != null)) {
                            val pox = left + (right - left) * progress / 100
                            _binding?.currentExposureTime?.setText(expTime.toString())
                            _binding?.currentExposureTime?.setX(java.lang.Float.valueOf(pox.toString()))
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                    }

                })
            }

            /* HDR */
            class HDRCheckListener : CompoundButton.OnCheckedChangeListener {
                override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                    Log.i(TAG, "HDR mode isChecked $isChecked")

                    if (isChecked)
                        scene_mode = CaptureRequest.CONTROL_SCENE_MODE_HDR
                    else
                        scene_mode = CaptureRequest.CONTROL_SCENE_MODE_DISABLED

                    captureRequest.set(CaptureRequest.CONTROL_SCENE_MODE, scene_mode)
                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
                }
            }
            _binding?.HDR?.setOnCheckedChangeListener(HDRCheckListener())

            /* LSC */
            class LSCCheckListener : CompoundButton.OnCheckedChangeListener {
                override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                    Log.i(TAG, "LSC isChecked $isChecked")
                    val LSC_ENABLE = CaptureRequest.Key("vsi.lsc.enable", Int::class.java)
                    var lsc: Int;
                    if (isChecked)
                        lsc = 1
                    else
                        lsc = 0
                    captureRequest.set(LSC_ENABLE, lsc)
                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
                }
            }
            _binding?.LSC?.setOnCheckedChangeListener(LSCCheckListener())

            /* GAMMA */
            _binding?.gamma?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val GammaMin = 1.0
                    val GammaMax = 5.0

                    var gamma: Float = (GammaMin + (GammaMax - GammaMin) * progress / 100).toFloat()
                    captureRequest.set(CaptureRequest.TONEMAP_GAMMA, gamma)
                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

                    val left = _binding?.gamma?.getLeft()
                    val right = _binding?.gamma?.getRight()
                    if ((left != null) && (right != null)) {
                        val pox = left + (right - left) * progress / 100
                        _binding?.currentGamma?.setText(gamma.toString())
                        _binding?.currentGamma?.setX(java.lang.Float.valueOf(pox.toString()))
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

            /* brightness */
            _binding?.brightness?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val BrightnessMin = -127
                    val BrightnessMax = 127
                    var brightness = BrightnessMin + (BrightnessMax - BrightnessMin) * progress / 100

                    val META_BRIGHTNESS = CaptureRequest.Key("vsi.brightness", Int::class.java)
                    captureRequest.set(META_BRIGHTNESS, brightness)
                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

                    val left = _binding?.brightness?.getLeft()
                    val right = _binding?.brightness?.getRight()
                    if ((left != null) && (right != null)) {
                        val pox = left + (right - left) * progress / 100
                        _binding?.currentBrightness?.setText(brightness.toString())
                        _binding?.currentBrightness?.setX(java.lang.Float.valueOf(pox.toString()))
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

            /* contrast */
            _binding?.contrast?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val ContrastMin = 0.0
                    val ContrastMax = 1.99
                    var contrast = (ContrastMin + (ContrastMax - ContrastMin) * progress / 100).toFloat()

                    val META_CONTRAST = CaptureRequest.Key("vsi.contrast", Float::class.java)
                    captureRequest.set(META_CONTRAST, contrast)
                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

                    val left = _binding?.contrast?.getLeft()
                    val right = _binding?.contrast?.getRight()
                    if ((left != null) && (right != null)) {
                        val pox = left + (right - left) * progress / 100
                        _binding?.currentContrast?.setText(contrast.toString())
                        _binding?.currentContrast?.setX(java.lang.Float.valueOf(pox.toString()))
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

            /* saturation */
            _binding?.saturation?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val SaturationMin = 0.0
                    val SaturationMax = 1.99
                    var saturation = (SaturationMin + (SaturationMax - SaturationMin) * progress / 100).toFloat()

                    val META_SATURATION = CaptureRequest.Key("vsi.saturation", Float::class.java)
                    captureRequest.set(META_SATURATION, saturation)
                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

                    val left = _binding?.saturation?.getLeft()
                    val right = _binding?.saturation?.getRight()
                    if ((left != null) && (right != null)) {
                        val pox = left + (right - left) * progress / 100
                        _binding?.currentSaturation?.setText(saturation.toString())
                        _binding?.currentSaturation?.setX(java.lang.Float.valueOf(pox.toString()))
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

            /* hue */
            _binding?.hue?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val HueMin = -127
                    val HueMax = 127
                    var hue = HueMin + (HueMax - HueMin) * progress / 100

                    val META_HUE = CaptureRequest.Key("vsi.hue", Int::class.java)
                    captureRequest.set(META_HUE, hue)
                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

                    val left = _binding?.hue?.getLeft()
                    val right = _binding?.hue?.getRight()
                    if ((left != null) && (right != null)) {
                        val pox = left + (right - left) * progress / 100
                        _binding?.currentHue?.setText(hue.toString())
                        _binding?.currentHue?.setX(java.lang.Float.valueOf(pox.toString()))
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

            /* sharp level */
            _binding?.sharpLevel?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val SharpLevelMin = 1
                    val SharpLevelMax = 10
                    var sharp_level = (SharpLevelMin + (SharpLevelMax - SharpLevelMin) * progress / 100).toByte()

                    val META_SHARP_LEVEL = CaptureRequest.Key("vsi.sharp.level", Byte::class.java)
                    captureRequest.set(META_SHARP_LEVEL, sharp_level)
                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

                    val left = _binding?.sharpLevel?.getLeft()
                    val right = _binding?.sharpLevel?.getRight()
                    if ((left != null) && (right != null)) {
                        val pox = left + (right - left) * progress / 100
                        _binding?.currentSharpLevel?.setText(sharp_level.toString())
                        _binding?.currentSharpLevel?.setX(java.lang.Float.valueOf(pox.toString()))
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
        }
    }

    private fun SetWB(captureRequest: android.hardware.camera2.CaptureRequest.Builder, wbMode: Int) {
        captureRequest.set(CaptureRequest.CONTROL_AWB_MODE, wbMode)
        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
        wb_mode = wbMode
        Log.i(TAG, "SetWB mode ${wb_mode}")
    }

    /** Opens the camera and returns the opened device (as the result of the suspend coroutine) */
    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
            manager: CameraManager,
            cameraId: String,
            handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = cont.resume(device)

            override fun onDisconnected(device: CameraDevice) {
                Log.w(TAG, "Camera $cameraId has been disconnected")
                requireActivity().finish()
            }

            override fun onError(device: CameraDevice, error: Int) {
                val msg = when(error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                Log.e(TAG, exc.message, exc)
                if (cont.isActive) cont.resumeWithException(exc)
            }
        }, handler)
    }

    /**
     * Starts a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine
     */
    private suspend fun createCaptureSession(
            device: CameraDevice,
            targets: List<Surface>,
            handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->

        // Create a capture session using the predefined targets; this also involves defining the
        // session state callback to be notified of when the session is ready
        device.createCaptureSession(targets, object: CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    /**
     * Helper function used to capture a still image using the [CameraDevice.TEMPLATE_STILL_CAPTURE]
     * template. It performs synchronization between the [CaptureResult] and the [Image] resulting
     * from the single capture, and outputs a [CombinedCaptureResult] object.
     */
    private suspend fun takePhoto():
            CombinedCaptureResult = suspendCoroutine { cont ->

        // Flush any images left in the image reader
        @Suppress("ControlFlowWithEmptyBody")
        while (imageReader.acquireNextImage() != null) {}

        // Start a new image queue
        val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage()
            Log.d(TAG, "Image available in queue: ${image.timestamp}")
            imageQueue.add(image)
        }, imageReaderHandler)

        val captureRequest = session.device.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE).apply { addTarget(imageReader.surface) }

        captureRequest.set(CaptureRequest.CONTROL_SCENE_MODE, scene_mode)
        captureRequest.set(CaptureRequest.CONTROL_AWB_MODE, wb_mode)
        Log.i(TAG, "takePhoto, set scene mode ${scene_mode}, wb mode ${wb_mode}")

        session.capture(captureRequest.build(), object : CameraCaptureSession.CaptureCallback() {

            override fun onCaptureStarted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    timestamp: Long,
                    frameNumber: Long) {
                super.onCaptureStarted(session, request, timestamp, frameNumber)
                viewFinder.post(animationTask)
            }

            override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult) {
                super.onCaptureCompleted(session, request, result)
                val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                Log.d(TAG, "Capture result received: $resultTimestamp")

                // Set a timeout in case image captured is dropped from the pipeline
                val exc = TimeoutException("Image dequeuing took too long")
                val timeoutRunnable = Runnable { cont.resumeWithException(exc) }
                imageReaderHandler.postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS)

                // Loop in the coroutine's context until an image with matching timestamp comes
                // We need to launch the coroutine context again because the callback is done in
                //  the handler provided to the `capture` method, not in our coroutine context
                @Suppress("BlockingMethodInNonBlockingContext")
                lifecycleScope.launch(cont.context) {
                    while (true) {

                        // Dequeue images while timestamps don't match
                        val image = imageQueue.take()
                        // TODO(owahltinez): b/142011420
                        // if (image.timestamp != resultTimestamp) continue
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                image.format != ImageFormat.DEPTH_JPEG &&
                                image.timestamp != resultTimestamp) continue
                         Log.d(TAG, "Matching image dequeued: ${image.timestamp}")

                        // Unset the image reader listener
                        imageReaderHandler.removeCallbacks(timeoutRunnable)
                        imageReader.setOnImageAvailableListener(null, null)

                        // Clear the queue of images, if there are left
                        while (imageQueue.size > 0) {
                            imageQueue.take().close()
                        }

                        // Compute EXIF orientation metadata
                        val rotation = relativeOrientation.value ?: 0
                        val mirrored = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                                CameraCharacteristics.LENS_FACING_FRONT
                        val exifOrientation = computeExifOrientation(rotation, mirrored)

                        // Build the result and resume progress
                        cont.resume(CombinedCaptureResult(
                                image, result, exifOrientation, imageReader.imageFormat))

                        // There is no need to break out of the loop, this coroutine will suspend
                    }
                }
            }
        }, cameraHandler)
    }

    /** Helper function used to save a [CombinedCaptureResult] into a [File] */
    private suspend fun saveResult(result: CombinedCaptureResult): File = suspendCoroutine { cont ->
        when (result.format) {

            // When the format is JPEG or DEPTH JPEG we can simply save the bytes as-is
            ImageFormat.JPEG, ImageFormat.DEPTH_JPEG -> {
                val buffer = result.image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                try {
                    val output = createFile(outputDirectory, "jpg")
                    FileOutputStream(output).use { it.write(bytes) }
                    cont.resume(output)
                } catch (exc: IOException) {
                    Log.e(TAG, "Unable to write JPEG image to file", exc)
                    cont.resumeWithException(exc)
                }
            }

            // When the format is RAW we use the DngCreator utility library
            ImageFormat.RAW_SENSOR -> {
                val dngCreator = DngCreator(characteristics, result.metadata)
                try {
                    val output = createFile(outputDirectory, "dng")
                    FileOutputStream(output).use { dngCreator.writeImage(it, result.image) }
                    cont.resume(output)
                } catch (exc: IOException) {
                    Log.e(TAG, "Unable to write DNG image to file", exc)
                    cont.resumeWithException(exc)
                }
            }

            // No other formats are supported by this sample
            else -> {
                val exc = RuntimeException("Unknown image format: ${result.image.format}")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            camera.close()
        } catch (exc: Throwable) {
            Log.e(TAG, "Error closing camera", exc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
        imageReaderThread.quitSafely()
    }

    companion object {
        private val TAG = CameraFragment::class.java.simpleName

        /** Maximum number of images that will be held in the reader's buffer */
        private const val IMAGE_BUFFER_SIZE: Int = 3

        /** Maximum time allowed to wait for the result of an image capture */
        private const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5000

        /** Helper data class used to hold capture metadata with their associated image */
        data class CombinedCaptureResult(
                val image: Image,
                val metadata: CaptureResult,
                val orientation: Int,
                val format: Int
        ) : Closeable {
            override fun close() = image.close()
        }

        /**
         * Create a [File] named a using formatted timestamp with the current date and time.
         *
         * @return [File] created.
         */
        private fun createFile(baseFolder: File, extension: String): File {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
            return File(baseFolder, "IMG_${sdf.format(Date())}.$extension")
        }
    }
}
