LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := chalk-jni
LOCAL_SRC_FILES := chalk-jni.cpp
LOCAL_CPPFLAGS := -std=c++11
LOCAL_LDLIBS := -llog
LOCAL_C_INCLUDES := $(LOCAL_PATH)/libusb

LIBUSB_ROOT_REL:= libusb
LIBUSB_ROOT_ABS:= $(LOCAL_PATH)/libusb

LOCAL_SRC_FILES += \
  $(LIBUSB_ROOT_REL)/libusb/core.c \
  $(LIBUSB_ROOT_REL)/libusb/descriptor.c \
  $(LIBUSB_ROOT_REL)/libusb/hotplug.c \
  $(LIBUSB_ROOT_REL)/libusb/io.c \
  $(LIBUSB_ROOT_REL)/libusb/sync.c \
  $(LIBUSB_ROOT_REL)/libusb/strerror.c \
  $(LIBUSB_ROOT_REL)/libusb/os/linux_usbfs.c \
  $(LIBUSB_ROOT_REL)/libusb/os/poll_posix.c \
  $(LIBUSB_ROOT_REL)/libusb/os/threads_posix.c \
  $(LIBUSB_ROOT_REL)/libusb/os/linux_netlink.c

LOCAL_C_INCLUDES += \
  $(LOCAL_PATH)/.. \
  $(LIBUSB_ROOT_ABS)/libusb \
  $(LIBUSB_ROOT_ABS)/libusb/os

include $(BUILD_SHARED_LIBRARY)