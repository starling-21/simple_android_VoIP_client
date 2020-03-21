LOCAL_PATH := $(call my-dir)
OPUS_DIR            := opus-1.2.1

include $(OPUS_DIR)/Android.mk

include $(CLEAR_VARS)

LOCAL_MODULE        := codec


LOCAL_C_INCLUDES    := $(LOCAL_PATH)/$(OPUS_DIR)/include

LOCAL_SRC_FILES     := com_starling_zvonilka_jniwrappers_OpusEncoder.c \
                       com_starling_zvonilka_jniwrappers_OpusDecoder.c

LOCAL_CFLAGS        := -DNULL=0
LOCAL_LDLIBS        := -lm -llog

LOCAL_SHARED_LIBRARIES := opus
include $(BUILD_SHARED_LIBRARY)