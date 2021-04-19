package com.nxp.multicamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.RectF
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.*
import android.view.View.INVISIBLE
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.nxp.multicamera.databinding.FragmentCameraBinding
import com.nxp.multicamera.listeners.SurfaceTextureWaiter
import com.nxp.multicamera.models.CameraIdInfo
import com.nxp.multicamera.models.State
import com.nxp.multicamera.services.Camera
import com.nxp.multicamera.ui.ConfirmationDialog
import com.nxp.multicamera.ui.ErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.roundToInt

class CameraFragment : Fragment() {
    private lateinit var container: ConstraintLayout

    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
        private const val REQUEST_CAMERA_PERMISSION = 100
        private val TAG = CameraFragment::class.java::getSimpleName.toString()
        fun newInstance() = CameraFragment()
    }

    private var camera: Camera? = null

    private lateinit var previewSize: Size

    private val zoomTransition = false

    private var _binding: FragmentCameraBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding?.zoomBar?.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            var progressValue = 0

            @RequiresApi(Build.VERSION_CODES.P)
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                this.progressValue = progress
                camera?.maxZoom?.let {
                    if(!_binding?.camera1View?.isAvailable!! || !_binding?.camera2View?.isAvailable!!) return@let
                    val zoomValue = progressValue.toDouble()/seekBar.max * it
                    if(zoomTransition) {
                        // ADHOC
                        // Because max zoom is 7 and zoom level difference of two cameras are 2,
                        // Switch Camera 1 and 2 when zoom value is 100 / 7 * 2 = 2.857
                        if (zoomValue < 2.857) {
                            camera?.setZoom(zoomValue + 1)
                            // Delay view switch to ease the transition
                            Handler().postDelayed(
                                    {
                                        _binding?.camera1ViewLayout?.visibility = View.INVISIBLE
                                        _binding?.camera2ViewLayout?.visibility = View.VISIBLE
                                    }, 200)

                        } else {
                            camera?.setZoom(zoomValue)
                            Handler().postDelayed(
                                    {
                                        _binding?.camera1ViewLayout?.visibility = View.VISIBLE
                                        _binding?.camera2ViewLayout?.visibility = View.INVISIBLE
                                    }, 200)
                        }
                    } else {
                        camera?.setZoom(zoomValue)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Log.d(TAG, "===== manager $manager")
        camera = Camera.initInstance(manager)


        // set Seek bar zoom
        camera?.maxZoom?.let {
            val actualProgress = (100 / it).roundToInt()
            Log.d(TAG, "===== actual $actualProgress")
            _binding?.zoomBar?.progress = actualProgress
        }


    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onResume() {
        super.onResume()

        if (camera?.isLogicalCamera == false) {
            val waiter1 = _binding?.camera1View?.let { SurfaceTextureWaiter(it) }
            GlobalScope.launch {
                val result1 = waiter1?.textureIsReady()
                Log.d(TAG," ======== ready $result1 " )
                // Assuming both textures are ready and they have the same width and height,
                if (result1 != null) {
                    when(result1.state) {
                        State.ON_TEXTURE_AVAILABLE -> {
                            withContext(Dispatchers.Main) {
                                _binding?.camera1View?.let { openCamera(it.width, _binding?.camera1View!!.height) }
                            }
                        }
                        State.ON_TEXTURE_SIZE_CHANGED -> {
                            withContext(Dispatchers.Main) {
                                configureTransform(viewWidth = result1.width, viewHeight = result1.height)
                            }
                        }
                        else -> { }
                    }
                }
            }
        } else {
            val waiter1 = _binding?.let { SurfaceTextureWaiter(it.camera1View) }
            val waiter2 = _binding?.let { SurfaceTextureWaiter(it.camera2View) }

            GlobalScope.launch {
                val result1 = waiter1?.textureIsReady()
                val result2 = waiter2?.textureIsReady()
                Log.d(TAG," ======== ready $result1 $result2" )
                // Assuming both textures are ready and they have the same width and height,
                // Just check the state of 1
                when(result1?.state) {
                    State.ON_TEXTURE_AVAILABLE -> {
                        withContext(Dispatchers.Main) {
                            openDualCamera(width = result1.width, height = result1.height)
                        }
                    }
                    State.ON_TEXTURE_SIZE_CHANGED -> {
                        withContext(Dispatchers.Main) {
                            result2?.let { configureTransform(viewWidth = result1.width, viewHeight = it.height) }
                        }
                    }
                    else -> { }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onPause() {
        super.onPause()
        camera?.close()
    }

    // Permissions
    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                    .show(childFragmentManager, FRAGMENT_DIALOG)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun openCamera(width: Int, height: Int) {

        if (activity == null) {
            Log.e(TAG, "activity is not ready!")
            return
        }
        val permission = ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
            //return
        }

        try {
            camera?.let {
                // Usually preview size has to be calculated based on the sensor rotation using getImageOrientation()
                // so that the sensor rotation and image rotation aspect matches correctly.
                // In this sample app, we know that Pixel series has the 90 degrees of sensor rotation,
                // so we just consider that width/ height < 1, which means portrait.
                val aspectRatio: Float = width / height.toFloat()
                previewSize = it.getPreviewSize(aspectRatio)
                _binding?.camera1View?.setAspectRatio(previewSize.height, previewSize.width)
                configureTransform(width, height)
                it.open()
                val texture1 = _binding?.camera1View?.surfaceTexture
                texture1?.setDefaultBufferSize(previewSize.width, previewSize.height)
                it.start(listOf(Surface(texture1)))
                updateSingleCameraStatus(it.allCameraIds,it.getSingleCameraIds())
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun openDualCamera(width: Int, height: Int) {
        if (activity == null) {
            Log.e(TAG, "activity is not ready!")
            return
        }
        val permission = ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
            //return
        }

        try {
            camera?.let {
                // Usually preview size has to be calculated based on the sensor rotation using getImageOrientation()
                // so that the sensor rotation and image rotation aspect matches correctly.
                // In this sample app, we know that Pixel series has the 90 degrees of sensor rotation,
                // so we just consider that width/ height < 1, which means portrait.
                val aspectRatio: Float = width / height.toFloat()
                previewSize = it.getPreviewSize(aspectRatio)
                // FIXME should write this better
                _binding?.camera1View?.setAspectRatio(previewSize.height, previewSize.width)
                _binding?.camera2View?.setAspectRatio(previewSize.height, previewSize.width)
                val matrix = calculateTransform(width, height)
                _binding?.camera1View?.setTransform(matrix)
                _binding?.camera2View?.setTransform(matrix)
                it.open()
                val texture1 = _binding?.camera1View?.surfaceTexture
                val texture2 = _binding?.camera2View?.surfaceTexture
                texture1?.setDefaultBufferSize(previewSize.width, previewSize.height)
                texture2?.setDefaultBufferSize(previewSize.width, previewSize.height)
                it.start(listOf(Surface(texture1), Surface(texture2)))
                updateCameraStatus(it.allCameraIds,it.getCameraIds(),it.physicalCameraIdUsed)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    private fun updateCameraStatus(allCameraIds: Array<String>, cameraIdInfo: CameraIdInfo, physicalCameraIdUsed: ArrayList<String>) {
        val (logicalCameraId, physicalCameraIds) = cameraIdInfo
        _binding?.totalCamera?.text = Arrays.toString(allCameraIds)
        if(logicalCameraId.isNotEmpty()) {
            _binding?.multiCameraSupportTv?.text = "YES"
            _binding?.logicalCameraTv?.text = "[$logicalCameraId]"
        }
        if(physicalCameraIds.isNotEmpty()) {
            _binding?.physicalCameraTv?.text = physicalCameraIds
                .asSequence()
                .map { s -> "[$s]" }
                .reduce { acc, s -> "$acc,$s" }
            if (_binding?.camera1TextView?.isVisible == true)
                _binding?.camera1TextView?.text = physicalCameraIdUsed.get(0)
            if (_binding?.camera2TextView?.isVisible == true)
                _binding?.camera2TextView?.text = physicalCameraIdUsed.get(1)
        }
    }

    private fun updateSingleCameraStatus(allCameraIds: Array<String>, cameraIdInfo: String) {
        _binding?.totalCamera?.text= Arrays.toString(allCameraIds)
        _binding?.multiCameraSupportTv?.text = "NO"
        _binding?.logicalCameraTv?.text = ""
        _binding?.physicalCameraTv?.text = ""
            if (_binding?.camera1TextView?.isVisible == true)
                _binding?.camera1TextView?.text = cameraIdInfo
            if (_binding?.camera2TextView?.isVisible == true)
                _binding?.camera2TextView?.visibility = INVISIBLE
    }

    private fun calculateTransform(viewWidth: Int, viewHeight: Int) : Matrix {
        val rotation = activity!!.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        return matrix
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        activity ?: return
        val rotation = activity!!.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        _binding?.camera1View?.setTransform(matrix)
    }
}