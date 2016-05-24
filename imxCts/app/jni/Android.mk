LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libimx_cts_app
LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)
LOCAL_C_INCLUDE +=	hardware/imx/mx6/libgralloc_wrapper
LOCAL_SRC_FILES := com_imx_app_AllocBufferService.cpp util_alloc_buf.cpp alloc_buf_test.cpp
LOCAL_SHARED_LIBRARIES := liblog libEGL libhardware
include $(BUILD_SHARED_LIBRARY)
