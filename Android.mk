LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files) $(call all-renderscript-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := \
	roottools \
	android-support-v4 \
	annotations \
	commons-io \

LOCAL_PACKAGE_NAME := SimpleExplorer
LOCAL_CERTIFICATE := shared

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
	roottools:libs/RootTools-3.4.jar \
	commons-io:libs/commons-io-2.4.jar \
	annotations:libs/annotations-12.0.jar

include $(BUILD_MULTI_PREBUILT)
