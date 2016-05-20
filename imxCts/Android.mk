LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-test ctsdeviceutil ctstestrunner ub-uiautomator
LOCAL_PACKAGE_NAME := CtsImxTestCases
LOCAL_CERTIFICATE := platform
LOCAL_SDK_VERSION := current

include $(BUILD_CTS_PACKAGE)
