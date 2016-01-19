ifneq ($(BOARD_HAVE_BLUETOOTH_BCM),)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) 
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME := BleClient
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true


include $(BUILD_PACKAGE)

endif
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
