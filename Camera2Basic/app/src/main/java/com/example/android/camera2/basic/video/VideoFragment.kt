/*
 * Copyright 2020 The Android Open Source Project
 * Copyright 2023 NXP
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

package com.example.android.camera2.basic.video

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.hardware.camera2.*
import android.media.MediaCodec
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.*
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.example.android.camera.utils.AutoFitSurfaceView
import com.example.android.camera.utils.OrientationLiveData
import com.example.android.camera.utils.getPreviewOutputSize
import com.example.android.camera2.basic.CameraActivity
import com.example.android.camera2.basic.CameraActivity.Companion.getOutputDirectory
import com.example.android.camera2.basic.R
import com.example.android.camera2.basic.databinding.FragmentVideoBinding
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

import android.widget.CompoundButton
import android.view.ScaleGestureDetector

class VideoFragment : Fragment() {
    private var mHflip = 0
    private var mVflip = 0
    private var mDewarp = 0
    private var scene_mode = CaptureRequest.CONTROL_SCENE_MODE_DISABLED
    private var wb_mode = CameraMetadata.CONTROL_AWB_MODE_AUTO
    private var m_expGain = -1;
    private var m_expTime: Long = -1;
    private var isRecording =false

    /** AndroidX navigation arguments */
    private val args: VideoFragmentArgs by navArgs()

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

    /** File where the recording will be saved */
    private val outputFile: File by lazy { createFile(requireContext(), "mp4") }

    /**
     * Setup a persistent [Surface] for the recorder so we can use it as an output target for the
     * camera session without preparing the recorder
     */
    private val recorderSurface: Surface by lazy {

        // Get a persistent Surface from MediaCodec, don't forget to release when done
        val surface = MediaCodec.createPersistentInputSurface()

        // Prepare and release a dummy MediaRecorder with our new surface
        // Required to allocate an appropriately sized buffer before passing the Surface as the
        //  output target to the capture session
        createRecorder(surface).apply {
            prepare()
            release()
        }

        surface
    }

    /** Saves the video recording */
    private val recorder: MediaRecorder by lazy { createRecorder(recorderSurface) }

    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [cameraThread] */
    private val cameraHandler = Handler(cameraThread.looper)

    /** Performs recording animation of flashing screen */
    private val animationTask: Runnable by lazy {
        Runnable {
            // Flash white animation
            overlay.foreground = Color.argb(150, 255, 255, 255).toDrawable()
            // Wait for ANIMATION_FAST_MILLIS
            overlay.postDelayed({
                // Remove white flash animation
                overlay.foreground = null
                // Restart animation recursively
                overlay.postDelayed(animationTask, CameraActivity.ANIMATION_FAST_MILLIS)
            }, CameraActivity.ANIMATION_FAST_MILLIS)
        }
    }

    /** Where the camera preview is displayed */
    private lateinit var viewFinder: AutoFitSurfaceView

    /** Overlay on top of the camera preview */
    private lateinit var overlay: View

    /** Captures frames from a [CameraDevice] for our video recording */
    private lateinit var session: CameraCaptureSession

    /** The [CameraDevice] that will be opened in this fragment */
    private lateinit var camera: CameraDevice

    private var recordingStartMillis: Long = 0L

    /** Live data listener for changes in the device orientation relative to the camera */
    private lateinit var relativeOrientation: OrientationLiveData

    private var _binding: FragmentVideoBinding? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater,container,false)
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
                if (args.width < previewSize.width && args.height < previewSize.height ) {
                    viewFinder.setAspectRatio(args.width, args.height)
                    Log.d(TAG, "Selected preview size: $args")

                } else {
                    viewFinder.setAspectRatio(previewSize.width, previewSize.height)
                    Log.d(TAG, "Selected preview size: $previewSize")
                }

                // To ensure that size is set, initialize camera in the view's thread
                viewFinder.post { initializeCamera() }
            }
        })

        // Used to rotate the output media to match device orientation
        relativeOrientation = OrientationLiveData(requireContext(), characteristics).apply {
            observe(viewLifecycleOwner, Observer {
                orientation -> Log.d(TAG, "Orientation changed: $orientation")
            })
        }

        // scale gesture detector
        var scaleFactor = 1f
        var detectScaleFactor = 1f
        var mListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detectScaleFactor
                scaleFactor = scaleFactor.coerceIn(1.0f, 4.0f)
                viewFinder.scaleX = scaleFactor
                viewFinder.scaleY = scaleFactor

                return super.onScale(detector)
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                return true
            }
            override fun onScaleEnd(detector: ScaleGestureDetector): Unit {
            }

        }

        val scaleGestureDetector = ScaleGestureDetector(
            requireContext(), mListener)

        var mCurrSpan : Float
        var mPrevSpan = 0f
        var mInProgress = false
        fun onTouchEvent(event: MotionEvent): Boolean {
            val mMinSpan = 100
            val mSpanSlop = 24
            var mInitialSpan = 0f
            val action = event.actionMasked
            val count = event.pointerCount

            val streamComplete =
                action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
            if (action == MotionEvent.ACTION_DOWN || streamComplete) {
                // Reset any scale in progress with the listener.
                // If it's an ACTION_DOWN we're beginning a new event stream.
                // This means the app probably didn't give us all the events. Shame on it.
                if (mInProgress) {
                    mListener.onScaleEnd(scaleGestureDetector)
                    mInProgress = false
                    mInitialSpan = 0f
                } else if (streamComplete) {
                    mInProgress = false
                    mInitialSpan = 0f
                }
                if (streamComplete) {
                    return true
                }
            }

            val configChanged =
                action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_POINTER_DOWN

            val pointerUp = action == MotionEvent.ACTION_POINTER_UP
            val skipIndex = if (pointerUp) event.actionIndex else -1
            // Determine focal point
            var sumX = 0f
            var sumY = 0f
            val div = if (pointerUp) count - 1 else count
            val focusX: Float
            val focusY: Float

            for (i in 0 until count) {
                if (skipIndex == i) continue
                sumX += event.getX(i)
                sumY += event.getY(i)
            }
            focusX = sumX / div
            focusY = sumY / div

            // Determine average deviation from focal point
            var devSumX = 0f
            var devSumY = 0f
            for (i in 0 until count) {
                if (skipIndex == i) continue
                // Convert the resulting diameter into a radius.
                devSumX += Math.abs(event.getX(i) - focusX)
                devSumY += Math.abs(event.getY(i) - focusY)
            }
            val devX = devSumX / div
            val devY = devSumY / div

            // Span is the average distance between touch points through the focal point;
            // i.e. the diameter of the circle with a radius of the average deviation from
            // the focal point.
            val spanX = devX * 2
            val spanY = devY * 2
            val span: Float
            span = Math.hypot(spanX.toDouble(), spanY.toDouble()).toFloat()

            // Dispatch begin/end events as needed.
            // If the configuration changes, notify the app to reset its current state by beginning
            // a fresh scale event stream.
            val wasInProgress: Boolean = mInProgress
            if (mInProgress && (span < mMinSpan || configChanged)) {
                mListener.onScaleEnd(scaleGestureDetector)
                mInProgress = false
                mInitialSpan = span
            }
            if (configChanged) {
                mCurrSpan = span
                mPrevSpan = mCurrSpan
                mInitialSpan = mPrevSpan
            }

            val minSpan: Int = mMinSpan
            if (!mInProgress && span >= minSpan && (wasInProgress || Math.abs(span - mInitialSpan) > mSpanSlop)
            ) {
                mCurrSpan = span
                mPrevSpan = mCurrSpan
                mInProgress = mListener.onScaleBegin(scaleGestureDetector)
            }

            // Handle motion; focal point and span/scale factor are changing.
            if (action == MotionEvent.ACTION_MOVE) {
                mCurrSpan = span
                detectScaleFactor = mCurrSpan / mPrevSpan

                var updatePrev = true
                if (mInProgress) {
                    updatePrev = mListener.onScale(scaleGestureDetector)
                }
                if (updatePrev) {
                    mPrevSpan = mCurrSpan
                }
            }
            return true
        }

        viewFinder.setOnTouchListener { _, event ->
            onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    /** Creates a [MediaRecorder] instance using the provided [Surface] as input */
    private fun createRecorder(surface: Surface) = MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setOutputFile(outputFile.absolutePath)
        setVideoEncodingBitRate(RECORDER_VIDEO_BITRATE)
        if (args.fps > 0) setVideoFrameRate(args.fps)
        setVideoSize(args.width, args.height)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setInputSurface(surface)
    }

    /**
     * Begin all camera operations in a coroutine in the main thread. This function:
     * - Opens the camera
     * - Configures the camera session
     * - Starts the preview by dispatching a repeating request
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {

        // Open the selected camera
        camera = openCamera(cameraManager, args.cameraId, cameraHandler)

        // Creates list of Surfaces where the camera will output frames
        val targets = listOf(viewFinder.holder.surface, recorderSurface)

        // Start a capture session using our open camera and list of Surfaces where frames will go
        session = createCaptureSession(camera, targets, cameraHandler)

        val cap_list = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        var raw_cap = false
        if (cap_list != null) {
            if (cap_list.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW))
                raw_cap = true
        }
        /** Requests used for preview only in the [CameraCaptureSession] */
        // Capture request holds references to target surfaces
        val previewRequest = session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                // Add the preview surface target
                addTarget(viewFinder.holder.surface)
                set(CaptureRequest.CONTROL_SCENE_MODE, scene_mode)
                set(CaptureRequest.CONTROL_AWB_MODE, wb_mode)
        }

        /** Requests used for preview and recording in the [CameraCaptureSession] */
        // Capture request holds references to target surfaces
        val recordRequest = session.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                // Add the preview and recording surface targets
                addTarget(viewFinder.holder.surface)
                addTarget(recorderSurface)
                // Sets user requested FPS for all targets
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(args.fps, args.fps))
                set(CaptureRequest.CONTROL_SCENE_MODE, scene_mode)
                set(CaptureRequest.CONTROL_AWB_MODE, wb_mode)
        }

        // Sends the capture request as frequently as possible until the session is torn down or
        //  session.stopRepeating() is called
        session.setRepeatingRequest(previewRequest.build(), null, cameraHandler)

        // React to user touching the capture button
        _binding?.captureButton?.setOnClickListener {
            if (!isRecording) {
                    // Prevents screen rotation during the video recording
                    requireActivity().requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_LOCKED

                    // Start recording repeating requests, which will stop the ongoing preview
                    //  repeating requests without having to explicitly call `session.stopRepeating`
                    session.setRepeatingRequest(recordRequest.build(), null, cameraHandler)

                    // Finalizes recorder setup and starts recording
                    recorder.apply {
                        // Sets output orientation based on current sensor value at start time
                        relativeOrientation.value?.let { setOrientationHint(it) }
                        prepare()
                        start()
                    }
                    recordingStartMillis = System.currentTimeMillis()
                    Log.d(TAG, "Recording started")

                    // Starts recording animation
                    overlay.post(animationTask)
            } else {
                    // Unlocks screen rotation after recording finished
                    requireActivity().requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                    Log.d(TAG, "Recording stopped. Output file: $outputFile")
                    recorder.stop()

                    // Removes recording animation
                    overlay.removeCallbacks(animationTask)

                    // Broadcasts the media file to the rest of the system
                    MediaScannerConnection.scanFile(
                            view?.context, arrayOf(outputFile.absolutePath), null, null)
                    navController.popBackStack()
            }
            isRecording = !isRecording
        }

        // the request for ISP settings in record mode should be recordRequest
        val captureRequest : CaptureRequest.Builder = recordRequest
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
            _binding?.exposureGain?.setMin(1)
            _binding?.exposureGain?.setMax(10)

            _binding?.exposureGain?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    captureRequest.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)

                    m_expGain = progress
                    val EXPOSURE_GAIN = CaptureRequest.Key("vsi.exposure.gain", Int::class.java)
                    captureRequest.set(EXPOSURE_GAIN, m_expGain)
                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

            /* exposure time */
            val supportedExposureTime = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            if (supportedExposureTime != null) {
                val timeRange: Range<Long> = supportedExposureTime
                Log.i(TAG, "exposure time range $timeRange")

                _binding?.exposureTime?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        captureRequest.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)

                        m_expTime =
                                timeRange.lower + (timeRange.upper - timeRange.lower) * progress / 100
                        captureRequest.set(CaptureRequest.SENSOR_EXPOSURE_TIME, m_expTime)
                        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

                        val left = _binding?.exposureTime?.getLeft()
                        val right = _binding?.exposureTime?.getRight()
                        if ((left != null) && (right != null)) {
                            val pox = left + (right - left) * progress / 100
                            _binding?.currentExposureTime?.setText(m_expTime.toString())
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
     * Creates a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine)
     */
    private suspend fun createCaptureSession(
            device: CameraDevice,
            targets: List<Surface>,
            handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->

        // Creates a capture session using the predefined targets, and defines a session state
        // callback which resumes the coroutine once the session is configured
        device.createCaptureSession(targets, object: CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    override fun onStop() {
        super.onStop()
        if (outputFile.exists() && (outputFile.length() == 0L) ){
            outputFile.delete()
        }
        try {
            camera.close()
        } catch (exc: Throwable) {
            Log.e(TAG, "Error closing camera", exc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
        recorder.release()
        recorderSurface.release()
    }

    companion object {
        private val TAG = VideoFragment::class.java.simpleName

        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000

        /** Creates a [File] named with the current date and time */
        private fun createFile(context: Context, extension: String): File {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
            return File(getOutputDirectory(context), "VID_${sdf.format(Date())}.$extension")
        }
    }
}
