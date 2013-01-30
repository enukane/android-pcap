LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE:= libpcap
LOCAL_SRC_FILES:= \
	libpcap-1.3.0/pcap-linux.c \
	libpcap-1.3.0/fad-null.c \
	libpcap-1.3.0/pcap.c \
	libpcap-1.3.0/inet.c \
	libpcap-1.3.0/gencode.c \
	libpcap-1.3.0/optimize.c \
	libpcap-1.3.0/nametoaddr.c \
	libpcap-1.3.0/etherent.c \
	libpcap-1.3.0/savefile.c \
	libpcap-1.3.0/sf-pcap.c \
	libpcap-1.3.0/sf-pcap-ng.c \
	libpcap-1.3.0/pcap-common.c \
	libpcap-1.3.0/bpf_image.c \
	libpcap-1.3.0/bpf_dump.c \
	libpcap-1.3.0/scanner.c \
	libpcap-1.3.0/grammar.c \
	libpcap-1.3.0/bpf_filter.c \
	libpcap-1.3.0/version.c \
	pcap_jni.c
#include $(BUILD_EXECUTABLE)
LOCAL_CFLAGS	:= -DSYS_ANDROID=1 -Dyylval=pcap_lval -DHAVE_CONFIG_H  -D_U_="__attribute__((unused))" -I$(LOCAL_PATH)/libpcap-1.3.0
LOCAL_LDLIBS	:= -llog
include $(BUILD_SHARED_LIBRARY)

