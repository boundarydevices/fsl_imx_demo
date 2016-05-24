LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)
LOCAL_JNI_SHARED_LIBRARIES := libimx_cts_app
LOCAL_STATIC_JAVA_LIBRARIES := android-support-test ctsdeviceutil ctstestrunner ub-uiautomator
LOCAL_PACKAGE_NAME := CtsIMxTestApp
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := $(call all-java-files-under, src)

include $(BUILD_CTS_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))
