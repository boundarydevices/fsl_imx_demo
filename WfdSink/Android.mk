ifeq ($(BOARD_HAVE_VPU),true)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := WfdSink
LOCAL_CERTIFICATE := platform

LOCAL_STATIC_JAVA_LIBRARIES := fsl.imx
LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
