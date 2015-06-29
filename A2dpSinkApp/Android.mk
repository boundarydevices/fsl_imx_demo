ifeq (true,$(filter true,$(BOARD_USE_AR3K_BLUETOOTH) $(BOARD_HAVE_BLUETOOTH_BCM)))
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, java)

LOCAL_PACKAGE_NAME := A2dpSinkApp

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
endif

