package com.nxp.multicamera.models

import java.util.Collections.emptyList


data class CameraIdInfo(
    val logicalCameraId: String = "",
    val physicalCameraIds: List<String> = emptyList()
)