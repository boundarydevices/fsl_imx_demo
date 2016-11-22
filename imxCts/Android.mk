LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-test ctsdeviceutil ctstestrunner ub-uiautomator android-support-v4
LOCAL_PACKAGE_NAME := CtsImxTestCases
LOCAL_COMPATIBILITY_SUITE := cts
LOCAL_SDK_VERSION := current
include $(BUILD_CTS_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))
