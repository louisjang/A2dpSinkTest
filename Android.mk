LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PACKAGE_NAME := A2dpSinkTest

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)
