package net.kismetwireless.android.pcapcapture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

/*
 * Converted to Java/Android by Mike Kershaw / Dragorn <dragorn@kismetwireless.net>
 * 
 * Derived from MacUserspaceWifi project and current (3.x) linux kernel drivers
 * 
 * Additional copyright/attributions:
 * 
 * Definitions for RTL8187 hardware
 *
 * Adapted by pr0gg3d from linux-kernel source
 *
 * Original copyrights:
 *
 * Copyright 2007 Michael Wu <flamingice@sourmilk.net>
 * Copyright 2007 Andrea Merello <andreamrl@tiscali.it>
 *
 * Based on the r8187 driver, which is:
 * Copyright 2005 Andrea Merello <andreamrl@tiscali.it>, et al.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 */


public class Rtl8187Card extends UsbSource {
	String TAG = "rtl8187";
	
	private final int USB_ENDPOINT_OUT = 0x00;
	private final int USB_ENDPOINT_IN = 0x80;
	private final int USB_TYPE_CLASS = (0x01 << 5);
	private final int USB_RECIP_INTERFACE = 0x01;
	
	private final int RTL8187_EEPROM_TXPWR_BASE = 0x05;
	private final int RTL8187_EEPROM_MAC_ADDR = 0x07;
	private final int RTL8187_EEPROM_TXPWR_CHAN_1 = 0x16;    /* 3 channels */
	private final int RTL8187_EEPROM_TXPWR_CHAN_6 = 0x1B;    /* 2 channels */
	private final int RTL8187_EEPROM_TXPWR_CHAN_4 = 0x3D;    /* 2 channels */

	private final int RTL8187_REQT_READ = 0xC0;
	private final int RTL8187_REQT_WRITE = 0x40;
	
	private final int RTL8187_REQ_GET_REG = 0x05;
	private final int RTL8187_REQ_SET_REG = 0x05;

	private final int RTL8187_MAX_RX = 0x9C4;
	
	private final int RTL818X_ADDR_RX_CONF = 0xff44;
	
	private Object control_lock = new Object();
	
	private final int rtl8225bcd_rxgain[] = {
	    0x0400, 0x0401, 0x0402, 0x0403, 0x0404, 0x0405, 0x0408, 0x0409,
	    0x040a, 0x040b, 0x0502, 0x0503, 0x0504, 0x0505, 0x0540, 0x0541,
	    0x0542, 0x0543, 0x0544, 0x0545, 0x0580, 0x0581, 0x0582, 0x0583,
	    0x0584, 0x0585, 0x0588, 0x0589, 0x058a, 0x058b, 0x0643, 0x0644,
	    0x0645, 0x0680, 0x0681, 0x0682, 0x0683, 0x0684, 0x0685, 0x0688,
	    0x0689, 0x068a, 0x068b, 0x068c, 0x0742, 0x0743, 0x0744, 0x0745,
	    0x0780, 0x0781, 0x0782, 0x0783, 0x0784, 0x0785, 0x0788, 0x0789,
	    0x078a, 0x078b, 0x078c, 0x078d, 0x0790, 0x0791, 0x0792, 0x0793,
	    0x0794, 0x0795, 0x0798, 0x0799, 0x079a, 0x079b, 0x079c, 0x079d,
	    0x07a0, 0x07a1, 0x07a2, 0x07a3, 0x07a4, 0x07a5, 0x07a8, 0x07a9,
	    0x07aa, 0x07ab, 0x07ac, 0x07ad, 0x07b0, 0x07b1, 0x07b2, 0x07b3,
	    0x07b4, 0x07b5, 0x07b8, 0x07b9, 0x07ba, 0x07bb, 0x07bb
	};

	private final int rtl8225_agc[] = {
	    0x9e, 0x9e, 0x9e, 0x9e, 0x9e, 0x9e, 0x9e, 0x9e,
	    0x9d, 0x9c, 0x9b, 0x9a, 0x99, 0x98, 0x97, 0x96,
	    0x95, 0x94, 0x93, 0x92, 0x91, 0x90, 0x8f, 0x8e,
	    0x8d, 0x8c, 0x8b, 0x8a, 0x89, 0x88, 0x87, 0x86,
	    0x85, 0x84, 0x83, 0x82, 0x81, 0x80, 0x3f, 0x3e,
	    0x3d, 0x3c, 0x3b, 0x3a, 0x39, 0x38, 0x37, 0x36,
	    0x35, 0x34, 0x33, 0x32, 0x31, 0x30, 0x2f, 0x2e,
	    0x2d, 0x2c, 0x2b, 0x2a, 0x29, 0x28, 0x27, 0x26,
	    0x25, 0x24, 0x23, 0x22, 0x21, 0x20, 0x1f, 0x1e,
	    0x1d, 0x1c, 0x1b, 0x1a, 0x19, 0x18, 0x17, 0x16,
	    0x15, 0x14, 0x13, 0x12, 0x11, 0x10, 0x0f, 0x0e,
	    0x0d, 0x0c, 0x0b, 0x0a, 0x09, 0x08, 0x07, 0x06,
	    0x05, 0x04, 0x03, 0x02, 0x01, 0x01, 0x01, 0x01,
	    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
	    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
	    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01
	};
	
	private final int rtl8225_gain[] = {
	    0x23, 0x88, 0x7c, 0xa5, /* -82dBm */
	    0x23, 0x88, 0x7c, 0xb5, /* -82dBm */
	    0x23, 0x88, 0x7c, 0xc5, /* -82dBm */
	    0x33, 0x80, 0x79, 0xc5, /* -78dBm */
	    0x43, 0x78, 0x76, 0xc5, /* -74dBm */
	    0x53, 0x60, 0x73, 0xc5, /* -70dBm */
	    0x63, 0x58, 0x70, 0xc5, /* -66dBm */
	};
	
	private final int rtl8225_threshold[] = {
	    0x8d, 0x8d, 0x8d, 0x8d, 0x9d, 0xad, 0xbd
	};
	
	private final int rtl8225_tx_gain_cck_ofdm[] = {
	    0x02, 0x06, 0x0e, 0x1e, 0x3e, 0x7e
	};
	
	private final int rtl8225_tx_power_cck[] = {
	    0x18, 0x17, 0x15, 0x11, 0x0c, 0x08, 0x04, 0x02,
	    0x1b, 0x1a, 0x17, 0x13, 0x0e, 0x09, 0x04, 0x02,
	    0x1f, 0x1e, 0x1a, 0x15, 0x10, 0x0a, 0x05, 0x02,
	    0x22, 0x21, 0x1d, 0x18, 0x11, 0x0b, 0x06, 0x02,
	    0x26, 0x25, 0x21, 0x1b, 0x14, 0x0d, 0x06, 0x03,
	    0x2b, 0x2a, 0x25, 0x1e, 0x16, 0x0e, 0x07, 0x03
	};
	
	private final int rtl8225_tx_power_cck_ch14[] = {
	    0x18, 0x17, 0x15, 0x0c, 0x00, 0x00, 0x00, 0x00,
	    0x1b, 0x1a, 0x17, 0x0e, 0x00, 0x00, 0x00, 0x00,
	    0x1f, 0x1e, 0x1a, 0x0f, 0x00, 0x00, 0x00, 0x00,
	    0x22, 0x21, 0x1d, 0x11, 0x00, 0x00, 0x00, 0x00,
	    0x26, 0x25, 0x21, 0x13, 0x00, 0x00, 0x00, 0x00,
	    0x2b, 0x2a, 0x25, 0x15, 0x00, 0x00, 0x00, 0x00
	};
	
	private final char rtl8225_tx_power_ofdm[] = {
	    0x80, 0x90, 0xa2, 0xb5, 0xcb, 0xe4
	};

	private final int rtl8225_chan[] = {
	    0x085c, 0x08dc, 0x095c, 0x09dc, 0x0a5c, 0x0adc, 0x0b5c,
	    0x0bdc, 0x0c5c, 0x0cdc, 0x0d5c, 0x0ddc, 0x0e5c, 0x0f72
	};

	private final int rtl8225z2_agc[] = {
	    0x5e, 0x5e, 0x5e, 0x5e, 0x5d, 0x5b, 0x59, 0x57, 0x55, 0x53, 0x51, 0x4f,
	    0x4d, 0x4b, 0x49, 0x47, 0x45, 0x43, 0x41, 0x3f, 0x3d, 0x3b, 0x39, 0x37,
	    0x35, 0x33, 0x31, 0x2f, 0x2d, 0x2b, 0x29, 0x27, 0x25, 0x23, 0x21, 0x1f,
	    0x1d, 0x1b, 0x19, 0x17, 0x15, 0x13, 0x11, 0x0f, 0x0d, 0x0b, 0x09, 0x07,
	    0x05, 0x03, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
	    0x01, 0x01, 0x01, 0x01, 0x19, 0x19, 0x19, 0x19, 0x19, 0x19, 0x19, 0x19,
	    0x19, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x26, 0x27, 0x27, 0x28,
	    0x28, 0x29, 0x2a, 0x2a, 0x2a, 0x2b, 0x2b, 0x2b, 0x2c, 0x2c, 0x2c, 0x2d,
	    0x2d, 0x2d, 0x2d, 0x2e, 0x2e, 0x2e, 0x2e, 0x2f, 0x2f, 0x2f, 0x30, 0x30,
	    0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31,
	    0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31
	};

	
	private final int rtl8225z2_tx_power_cck_ch14[] = {
	    0x36, 0x35, 0x2e, 0x1b, 0x00, 0x00, 0x00, 0x00
	};
	
	private final int rtl8225z2_tx_power_cck[] = {
	    0x36, 0x35, 0x2e, 0x25, 0x1c, 0x12, 0x09, 0x04
	};
	
	private final int rtl8225z2_tx_power_ofdm[] = {
	    0x42, 0x00, 0x40, 0x00, 0x40
	};
	
	private final int rtl8225z2_tx_gain_cck_ofdm[] = {
	    0x00, 0x01, 0x02, 0x03, 0x04, 0x05,
	    0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b,
	    0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11,
	    0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
	    0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d,
	    0x1e, 0x1f, 0x20, 0x21, 0x22, 0x23
	};
	
	private final int rtl8225z2_rxgain[] = {
	    0x0400, 0x0401, 0x0402, 0x0403, 0x0404, 0x0405, 0x0408, 0x0409,
	    0x040a, 0x040b, 0x0502, 0x0503, 0x0504, 0x0505, 0x0540, 0x0541,
	    0x0542, 0x0543, 0x0544, 0x0545, 0x0580, 0x0581, 0x0582, 0x0583,
	    0x0584, 0x0585, 0x0588, 0x0589, 0x058a, 0x058b, 0x0643, 0x0644,
	    0x0645, 0x0680, 0x0681, 0x0682, 0x0683, 0x0684, 0x0685, 0x0688,
	    0x0689, 0x068a, 0x068b, 0x068c, 0x0742, 0x0743, 0x0744, 0x0745,
	    0x0780, 0x0781, 0x0782, 0x0783, 0x0784, 0x0785, 0x0788, 0x0789,
	    0x078a, 0x078b, 0x078c, 0x078d, 0x0790, 0x0791, 0x0792, 0x0793,
	    0x0794, 0x0795, 0x0798, 0x0799, 0x079a, 0x079b, 0x079c, 0x079d,
	    0x07a0, 0x07a1, 0x07a2, 0x07a3, 0x07a4, 0x07a5, 0x07a8, 0x07a9,
	    0x03aa, 0x03ab, 0x03ac, 0x03ad, 0x03b0, 0x03b1, 0x03b2, 0x03b3,
	    0x03b4, 0x03b5, 0x03b8, 0x03b9, 0x03ba, 0x03bb, 0x03bb
	};
	
	private final int rtl8225z2_ofdm[] = {
	    0x10, 0x0d, 0x01, 0x00, 0x14, 0xfb, 0xfb, 0x60,
	    0x00, 0x60, 0x00, 0x00, 0x00, 0x5c, 0x00, 0x00,
	    0x40, 0x00, 0x40, 0x00, 0x00, 0x00, 0xa8, 0x26,
	    0x32, 0x33, 0x07, 0xa5, 0x6f, 0x55, 0xc8, 0xb3,
	    0x0a, 0xe1, 0x2C, 0x8a, 0x86, 0x83, 0x34, 0x0f,
	    0x4f, 0x24, 0x6f, 0xc2, 0x6b, 0x40, 0x80, 0x00,
	    0xc0, 0xc1, 0x58, 0xf1, 0x00, 0xe4, 0x90, 0x3e,
	    0x6d, 0x3c, 0xfb, 0x07
	};


	private final int rtl8225z2_gain_bg[] = {
	    0x23, 0x15, 0xa5, /* -82-1dBm */
	    0x23, 0x15, 0xb5, /* -82-2dBm */
	    0x23, 0x15, 0xc5, /* -82-3dBm */
	    0x33, 0x15, 0xc5, /* -78dBm */
	    0x43, 0x15, 0xc5, /* -74dBm */
	    0x53, 0x15, 0xc5, /* -70dBm */
	    0x63, 0x15, 0xc5  /* -66dBm */
	};
	
	private final int PCI_EEPROM_WIDTH_93C66 = 8;
	private final int PCI_EEPROM_WIDTH_93C46 = 6;
	
	private final int RTL818X_ADDR_EEPROM_CMD = 0xff50;
	private final int RTL818X_EEPROM_CMD_CONFIG = (3 << 6);
	
	private final int RTL818X_EEPROM_CMD_WRITE = (1 << 1);
	private final int RTL818X_EEPROM_CMD_READ = (1 << 0);
	private final int RTL818X_EEPROM_CMD_CK = (1 << 2);
	private final int RTL818X_EEPROM_CMD_CS = (1 << 3);
	
	private final int RTL818X_EEPROM_CMD_PROGRAM = (2 << 6);
	
	private final int PCI_EEPROM_READ_OPCODE = 0x06;
	private final int PCI_EEPROM_WIDTH_OPCODE = 0x03;
	
	private final int RTL818X_ADDR_PGSELECT = 0xff5e;
	private final int RTL818X_EEPROM_CMD_NORMAL = (0 << 6);
	
	private final int RTL818X_ADDR_RFPinsOutput = 0xff80;
	private final int RTL818X_ADDR_RFPinsEnable = 0xff82;
	private final int RTL818X_ADDR_RFPinsSelect = 0xff84;
	private final int RTL818X_ADDR_RFPinsInput = 0xff86;
	
	private final int RTL818X_ADDR_CONFIG3 = 0xff59;
	private final int RTL818X_CONFIG3_ANAPARAM_WRITE = (1 << 6);
	private final int RTL818X_ADDR_ANAPARAM2 = 0xff60;
	private final int RTL818X_ADDR_ANAPARAM = 0xff54;
	private final int RTL8225_ANAPARAM_ON = 0xa0000a59;
	private final int RTL8225_ANAPARAM2_ON = 0x860c7312;
	private final int RTL818X_ADDR_INT_MASK = 0xff3c;
	
    private final int RTL818X_ADDR_CMD = 0xff37;
    private final int RTL818X_CMD_RESET = (1 << 4);
    private final int RTL818X_EEPROM_CMD_LOAD = (1 << 6);
	
    private final int RTL818X_ADDR_GPIO = 0xff91;
    private final int RTL818X_ADDR_GP_ENABLE = 0xff90;
    private final int RTL818X_ADDR_CONFIG1 = 0xff52;
    private final int RTL818X_ADDR_INT_TIMEOUT = 0xff48;
    private final int RTL818X_ADDR_WPA_CONF = 0xffb0;
    private final int RTL818X_ADDR_RATE_FALLBACK = 0xffbe;
    private final int RTL818X_ADDR_RESP_RATE = 0xff34;
    private final int RTL818X_ADDR_BRSR = 0xff2c;
    
    private final int RTL818X_ADDR_RF_TIMING = 0xff8c;
    private final int RTL818X_ADDR_RF_PARA = 0xff88;
    private final int RTL818X_ADDR_TALLY_SEL = 0xfffc;
    
    private final int RTL818X_ADDR_PHY3 = 0xff7f;
    private final int RTL818X_ADDR_PHY2 = 0xff7e;
    private final int RTL818X_ADDR_PHY1 = 0xff7d;
    private final int RTL818X_ADDR_PHY0 = 0xff7c;

    private final int RTL818X_ADDR_TESTR = 0xff5b;   
    private final int RTL818X_ADDR_TX_ANTENNA = 0xff9f;    
    private final int RTL818X_ADDR_TX_GAIN_CCK = 0xff9d;
    private final int RTL818X_ADDR_TX_GAIN_OFDM = 0xff9e;
    
    private static int RTL818X_ADDR_TX_CONF = 0xff40;
    private static int RTL818X_TX_CONF_LOOPBACK_MAC = (1 << 17);

    private final int RTL818X_ADDR_MAR0 =  0xff08;
    private final int RTL818X_ADDR_MAR1 = 0xff0c;
    private final int RTL818X_ADDR_CW_CONF = 0xffbc;
    private final int RTL818X_ADDR_TX_AGC_CTL = 0xff9c;
    private final int RTL818X_ADDR_CONFIG4 = 0xff5a;

    private final int RTL818X_TX_CONF_NO_ICV = (1 << 19);
    private final int RTL818X_TX_CONF_DISCW = (1 << 20);
    private final int RTL818X_TX_CONF_R8180_ABCD = (2 << 25);
    private final int RTL818X_TX_CONF_R8180_F = (3 << 25);
    private final int RTL818X_TX_CONF_R8185_ABC = (4 << 25);
    private final int RTL818X_TX_CONF_R8185_D = (5 << 25);
    private final int RTL818X_TX_CONF_HWVER_MASK = (7 << 25);
    private final int RTL818X_TX_CONF_CW_MIN = (1 << 31);
    private final int RTL818X_RX_CONF_MONITOR = (1 << 0);
    private final int RTL818X_RX_CONF_NICMAC = (1 << 1);
    private final int RTL818X_RX_CONF_MULTICAST = (1 << 2);
    private final int RTL818X_RX_CONF_BROADCAST = (1 << 3);
    private final int RTL818X_RX_CONF_FCS = (1 << 5);
    private final int RTL818X_RX_CONF_DATA = (1 << 18);
    private final int RTL818X_RX_CONF_CTRL = (1 << 19);
    private final int RTL818X_RX_CONF_MGMT = (1 << 20);
    private final int RTL818X_RX_CONF_BSSID = (1 << 23);
    private final int RTL818X_RX_CONF_RX_AUTORESETPHY = (1 << 28);
    private final int RTL818X_RX_CONF_ONLYERLPKT = (1 << 31);
    
    private final int RTL818X_TX_AGC_CTL_PERPACKET_GAIN_SHIFT = (1 << 0);
    private final int RTL818X_TX_AGC_CTL_PERPACKET_ANTSEL_SHIFT = (1 << 1);
    private final int RTL818X_TX_AGC_CTL_FEEDBACK_ANT = (1 << 2);
    private final int RTL818X_CW_CONF_PERPACKET_CW_SHIFT = (1 << 0);
    private final int RTL818X_CW_CONF_PERPACKET_RETRY_SHIFT = (1 << 1);

    private final int RTL818X_CMD_TX_ENABLE = (1 << 2);
    private final int RTL818X_CMD_RX_ENABLE = (1 << 3);
    
    private final int RTL818X_TX_CONF_R8187vD = (5 << 25);
    private final int RTL818X_TX_CONF_R8187vD_B = (6 << 25);

    private final int RTL818X_R8187B_B = 0;
    private final int RTL818X_R8187B_D = 1;
    private final int RTL818X_R8187B_E = 2;

    private final int HW_RTL8187BvB = 0;
    private final int HW_RTL8187BvD = 1;
    private final int HW_RTL8187BvE = 2;
    
    private final int RTL8187_RTL8225_ANAPARAM_ON = 0xa0000a59;
    private final int RTL8187B_RTL8225_ANAPARAM_ON = 0x45090658;
    private final int RTL8187_RTL8225_ANAPARAM_OFF = 0xa00beb59;
    private final int RTL8187B_RTL8225_ANAPARAM_OFF = 0x55480658;
    
    private final int RTL8187_RTL8225_ANAPARAM2_ON = 0x860c7312;
    private final int RTL8187B_RTL8225_ANAPARAM2_ON = 0x727f3f52;
    private final int RTL8187_RTL8225_ANAPARAM2_OFF = 0x840dec11;
    private final int RTL8187B_RTL8225_ANAPARAM2_OFF = 0x72003f50;
    
    private final int RTL8187B_RTL8225_ANAPARAM3_ON = 0x00;
    private final int RTL8187B_RTL8225_ANAPARAM3_OFF = 0x00;
    
    private final int RTL818X_ADDR_ANAPARAM3 = 0xffee;
    private final int RTL818X_ADDR_TID_AC_MAP = 0xffe8;
    private final int RTL818X_ADDR_INT_MIG = 0xffe2;
    private final int RTL818X_ADDR_HSSI_PARA = 0xff94;
    private final int RTL818X_ADDR_ACM_CONTROL = 0xffbf;
    private final int RTL818X_ADDR_MSR = 0xff58;
    
    private final int RTL818X_MSR_ENEDCA = (4 << 2);
    
	private int eeprom_width = 0;
    private byte eeprom_reg_data_in = 0;
    private byte eeprom_reg_data_out = 0;
    private byte eeprom_reg_data_clock = 0;
    private byte eeprom_reg_chip_select = 0;
    
    private String chipset_name = "Unknown";
    private int hw_rev; 
	
    private int rx_conf;
    
    private int is_rtl8187b = 0;
    
    private int rf_type = 0;
    private final int rf_rtl8225 = 1;
    private final int rf_rtl8225z2 = 2;
    private final int rf_rtl8225z2b = 3;
    
    private int[] macaddr = new int[6];

    private final int rtl8187b_reg_table[][] = {
        {0xF0, 0x32, 0}, {0xF1, 0x32, 0}, {0xF2, 0x00, 0}, {0xF3, 0x00, 0},
        {0xF4, 0x32, 0}, {0xF5, 0x43, 0}, {0xF6, 0x00, 0}, {0xF7, 0x00, 0},
        {0xF8, 0x46, 0}, {0xF9, 0xA4, 0}, {0xFA, 0x00, 0}, {0xFB, 0x00, 0},
        {0xFC, 0x96, 0}, {0xFD, 0xA4, 0}, {0xFE, 0x00, 0}, {0xFF, 0x00, 0},

        {0x58, 0x4B, 1}, {0x59, 0x00, 1}, {0x5A, 0x4B, 1}, {0x5B, 0x00, 1},
        {0x60, 0x4B, 1}, {0x61, 0x09, 1}, {0x62, 0x4B, 1}, {0x63, 0x09, 1},
        {0xCE, 0x0F, 1}, {0xCF, 0x00, 1}, {0xF0, 0x4E, 1}, {0xF1, 0x01, 1},
        {0xF2, 0x02, 1}, {0xF3, 0x03, 1}, {0xF4, 0x04, 1}, {0xF5, 0x05, 1},
        {0xF6, 0x06, 1}, {0xF7, 0x07, 1}, {0xF8, 0x08, 1},

        {0x4E, 0x00, 2}, {0x0C, 0x04, 2}, {0x21, 0x61, 2}, {0x22, 0x68, 2},
        {0x23, 0x6F, 2}, {0x24, 0x76, 2}, {0x25, 0x7D, 2}, {0x26, 0x84, 2},
        {0x27, 0x8D, 2}, {0x4D, 0x08, 2}, {0x50, 0x05, 2}, {0x51, 0xF5, 2},
        {0x52, 0x04, 2}, {0x53, 0xA0, 2}, {0x54, 0x1F, 2}, {0x55, 0x23, 2},
        {0x56, 0x45, 2}, {0x57, 0x67, 2}, {0x58, 0x08, 2}, {0x59, 0x08, 2},
        {0x5A, 0x08, 2}, {0x5B, 0x08, 2}, {0x60, 0x08, 2}, {0x61, 0x08, 2},
        {0x62, 0x08, 2}, {0x63, 0x08, 2}, {0x64, 0xCF, 2},

        {0x5B, 0x40, 0}, {0x84, 0x88, 0}, {0x85, 0x24, 0}, {0x88, 0x54, 0},
        {0x8B, 0xB8, 0}, {0x8C, 0x07, 0}, {0x8D, 0x00, 0}, {0x94, 0x1B, 0},
        {0x95, 0x12, 0}, {0x96, 0x00, 0}, {0x97, 0x06, 0}, {0x9D, 0x1A, 0},
        {0x9F, 0x10, 0}, {0xB4, 0x22, 0}, {0xBE, 0x80, 0}, {0xDB, 0x00, 0},
        {0xEE, 0x00, 0}, {0x4C, 0x00, 2},

        {0x9F, 0x00, 3}, {0x8C, 0x01, 0}, {0x8D, 0x10, 0}, {0x8E, 0x08, 0},
        {0x8F, 0x00, 0}
    };

    
    private final int rtl818x_channels[][] = {
        { 1, 2412, 0, 0, 0, 0}, 
        { 2, 2417, 0, 0, 0, 0},
        { 3, 2422, 0, 0, 0, 0},
        { 4, 2427, 0, 0, 0, 0},
        { 5, 2432, 0, 0, 0, 0},
        { 6, 2437, 0, 0, 0, 0},
        { 7, 2442, 0, 0, 0, 0},
        { 8, 2447, 0, 0, 0, 0},
        { 9, 2452, 0, 0, 0, 0},
        { 10, 2457, 0, 0, 0, 0},
        { 11, 2462, 0, 0, 0, 0},
        { 12, 2467, 0, 0, 0, 0},
        { 13, 2472, 0, 0, 0, 0},
        { 14, 2484, 0, 0, 0, 0}
    };
    
    private int channels[][] = rtl818x_channels;
    private int txpwr_base;
    
    private int asic_rev;
    
    private int RTL8225_RF_INIT = 0;
    private int RTL8225Z2_RF_INIT = 1;
    private int rf_init_type;
    
    private int slot_time = 0;
    private int aifsn[] = new int[4];
    	
	private int rtl818x_ioread8(int addr) {
		byte[] val = {0};

		mConnection.controlTransfer(RTL8187_REQT_READ, RTL8187_REQ_GET_REG, addr, 0, val, 1, 1);
        
		// Dalvik is LE so just shift it in
        return val[0];
	}
	
	private int rtl818x_ioread16(int addr) {
		byte[] val = {0, 0};
		
        mConnection.controlTransfer(RTL8187_REQT_READ, RTL8187_REQ_GET_REG, addr, 0, val, 2, 1);
                
        return (val[1] << 8) + val[0];
	}
	
	private int rtl818x_ioread32(int addr) {
		byte[] val = {0, 0, 0, 0};
		
        mConnection.controlTransfer(RTL8187_REQT_READ, RTL8187_REQ_GET_REG, addr, 0, val, 4, 1);

        // Dalvik is LE so just shift it in
        return (val[3] << 24) + (val[2] << 16) + (val[1] << 8) + val[0];
	}
	
	public static final byte[] intToByteArray(int value) {
		return new byte[] {
				(byte) value,
				(byte)(value >>> 8),
				(byte)(value >>> 16),
				(byte)(value >>> 24)};
	}
	
	private void rtl818x_iowrite32(int addr, int value) {
		byte[] val = intToByteArray(value);
				
        mConnection.controlTransfer(RTL8187_REQT_WRITE, RTL8187_REQ_GET_REG, addr, 0, val, 4, 1);     
	}
	
	private void rtl818x_iowrite32_idx(int addr, int value, int idx) {
		byte[] val = intToByteArray(value);
				
        mConnection.controlTransfer(RTL8187_REQT_WRITE, RTL8187_REQ_GET_REG, addr, idx & 0x03, val, 4, 1);     
	}
	
	private void rtl818x_iowrite16(int addr, int value) {
		byte[] val = intToByteArray(value);
		
		
		// Log.d(TAG, "iowrite16 controlxfer to " + addr);
		
		// only take first 2 bytes
        mConnection.controlTransfer(RTL8187_REQT_WRITE, RTL8187_REQ_GET_REG, addr, 0, val, 2, 1);     
	}
	
	private void rtl818x_iowrite16_idx(int addr, int value, int idx) {
		byte[] val = intToByteArray(value);

		mConnection.controlTransfer(RTL8187_REQT_WRITE, RTL8187_REQ_GET_REG, addr, idx & 0x03, val, 2, 1);     
	}

	private void rtl818x_iowrite8(int addr, int value) {
		byte[] val = {(byte) value};
				
        mConnection.controlTransfer(RTL8187_REQT_WRITE, RTL8187_REQ_GET_REG, addr, 0, val, 1, 1);     
	}
	
	private void rtl818x_iowrite8_idx(int addr, int value, int idx) {
		byte[] val = {(byte) value};
				
        mConnection.controlTransfer(RTL8187_REQT_WRITE, RTL8187_REQ_GET_REG, addr, idx & 0x03, val, 1, 1);     
	}
	
	void rtl8187_eeprom_register_read() {
	    int reg = rtl818x_ioread8(RTL818X_ADDR_EEPROM_CMD);

	    eeprom_reg_data_in = (byte) (reg & RTL818X_EEPROM_CMD_WRITE);
	    eeprom_reg_data_out = (byte) (reg & RTL818X_EEPROM_CMD_READ);
	    eeprom_reg_data_clock = (byte) (reg & RTL818X_EEPROM_CMD_CK);
	    eeprom_reg_chip_select = (byte) (reg & RTL818X_EEPROM_CMD_CS);
	}

	void rtl8187_eeprom_register_write() {
	    int reg = RTL818X_EEPROM_CMD_PROGRAM;

	    if (eeprom_reg_data_in != 0)
	        reg |= RTL818X_EEPROM_CMD_WRITE;
	    if (eeprom_reg_data_out != 0)
	        reg |= RTL818X_EEPROM_CMD_READ;
	    if (eeprom_reg_data_clock != 0)
	        reg |= RTL818X_EEPROM_CMD_CK;
	    if (eeprom_reg_chip_select != 0)
	        reg |= RTL818X_EEPROM_CMD_CS;

	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, reg);
	    usleep(1);
	}
	
	private void eeprom_93cx6_pulse_high() {
	    eeprom_reg_data_clock = 1;
	    rtl8187_eeprom_register_write();
	    usleep(1);
	}
	
	private void eeprom_93cx6_pulse_low() {
	    eeprom_reg_data_clock = 0;
	    rtl8187_eeprom_register_write();
	    usleep(1);
	}
	
	private void eeprom_93cx6_startup() {
	    /*
	     * Clear all flags, and enable chip select.
	     */
	    rtl8187_eeprom_register_read();
	    eeprom_reg_data_in = 0;
	    eeprom_reg_data_out = 0;
	    eeprom_reg_data_clock = 0;
	    eeprom_reg_chip_select = 1;
	    rtl8187_eeprom_register_write();

	    /*
	     * kick a pulse.
	     */
	    eeprom_93cx6_pulse_high();
	    eeprom_93cx6_pulse_low();
	}

	private void eeprom_93cx6_cleanup() {
	    /*
	     * Clear chip_select and data_in flags.
	     */
	    rtl8187_eeprom_register_read();
	    eeprom_reg_data_in = 0;
	    eeprom_reg_chip_select = 0;
	    rtl8187_eeprom_register_write();

	    /*
	     * kick a pulse.
	     */
	    eeprom_93cx6_pulse_high();
	    eeprom_93cx6_pulse_low();

	}
	
	private void eeprom_93cx6_write_bits(int data, int count) {
		int i;

		rtl8187_eeprom_register_read();

		/*
		 * Clear data flags.
		 */
		eeprom_reg_data_in = 0;
		eeprom_reg_data_out = 0;

		/*
		 * Start writing all bits.
		 */
		for (i = count; i > 0; i--) {
			/*
			 * Check if this bit needs to be set.
			 */
			if ((data & (1 << ((i - 1)))) != 0)
				eeprom_reg_data_in = 1;
			else
				eeprom_reg_data_in = 0;
			
			// eeprom_reg_data_in = !!((boolean) ((data & (1 << ((i - 1))) != 0));

			/*
			 * Write the bit to the eeprom register.
			 */
			rtl8187_eeprom_register_write();

			/*
			 * Kick a pulse.
			 */
			eeprom_93cx6_pulse_high();
			eeprom_93cx6_pulse_low();
		}

		eeprom_reg_data_in = 0;
		rtl8187_eeprom_register_write();
	}
	
	int eeprom_93cx6_read_bits(int count) {
		int i;
		int buf = 0;

		rtl8187_eeprom_register_read();

		/*
		 * Clear data flags.
		 */
		eeprom_reg_data_in = 0;
		eeprom_reg_data_out = 0;

		/*
		 * Start reading all bits.
		 */
		for (i = count; i > 0; i--) {
			eeprom_93cx6_pulse_high();

			rtl8187_eeprom_register_read();

			/*
			 * Clear data_in flag.
			 */
			eeprom_reg_data_in = 0;

			/*
			 * Read if the bit has been set.
			 */
			if (eeprom_reg_data_out != 0)
				buf |= (1 << (i - 1));

			eeprom_93cx6_pulse_low();
		}

		return buf;
	}

	
	private int eeprom_93cx6_read(int word) {
		int command;
		int ret;

		/*
		 * Initialize the eeprom register
		 */
		eeprom_93cx6_startup();

		/*
		 * Select the read opcode and the word to be read.
		 */
		command = (PCI_EEPROM_READ_OPCODE << eeprom_width) | word;
		eeprom_93cx6_write_bits(command,
				PCI_EEPROM_WIDTH_OPCODE + eeprom_width);

		/*
		 * Read the requested 16 bits.
		 */
		ret = eeprom_93cx6_read_bits(16);

		/*
		 * Cleanup eeprom register.
		 */
		eeprom_93cx6_cleanup();
		
		return ret;
	}
	
	private int[] eeprom_93cx6_multiread(int start_word, int words) {
		int i;
		int[] ret = new int[words];

		for (i = 0; i < words; i++) {			
			int tmp;
			
			tmp = eeprom_93cx6_read(start_word + i);
			ret[i] = tmp;
			
			// Log.d(TAG, "Multiread " + i + " " + tmp);
		}
		
		return ret;
	}


	@Override
	public ArrayList<UsbDevice> scanUsbDevices() {  
		ArrayList<UsbDevice> rl = new ArrayList<UsbDevice>();
		
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            
            if (scanUsbDevice(device)) {
            	rl.add(device);
            }
        }
    
        return rl;
	}

	@Override
	public boolean scanUsbDevice(UsbDevice device) {
		if (
			// Alfa AWUS036H, LevelOne WNC-0301USB v5, LevelOne WNC-0305USB
			(device.getVendorId() == 0x0bda && device.getProductId() == 0x8187) ||
			
			// AirLive WL-1600USB
			(device.getVendorId() == 0x1b75 && device.getProductId() == 0x8187) ||
			
			// NETGEAR WG111v2
			(device.getVendorId() == 0x0846 && device.getProductId() == 0x6a00) ||
			
			// NETGEAR WG111v3
			(device.getVendorId() == 0x0846 && device.getProductId() == 0x4260)
		) {
			return true;
		}
		
		return false;
	}
	
	public void doShutdown() {
		super.doShutdown();
		
		mUsbThread.stopUsb();
	}
	
	private void usleep(Integer n) {
		
        try {
			Thread.sleep(n / 1000);
		} catch (InterruptedException e) {

		}
		
	}

	
	private void rtl8225_write_8051(int addr, int data) {
	    int reg80, reg82, reg84;
	    reg80 = rtl818x_ioread16(RTL818X_ADDR_RFPinsOutput);
	    reg82 = rtl818x_ioread16(RTL818X_ADDR_RFPinsEnable);
	    reg84 = rtl818x_ioread16(RTL818X_ADDR_RFPinsSelect);

//	    NSLog(@"%s:%d %x %x %x", __func__, __LINE__, reg80, reg82, reg84);

	    reg80 &= ~(0x3 << 2);
	    reg84 &= ~0xF;

	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsEnable, reg82 | 0x0007);
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, reg84 | 0x0007);
	    usleep(10);

	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 2));
	    usleep(2);

	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80);
	    usleep(10);
	    
	    byte[] val = intToByteArray(data);
	    
        mConnection.controlTransfer(RTL8187_REQT_WRITE, RTL8187_REQ_GET_REG, addr, 0x8225, val, 2, 1);

	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 2));
	    usleep(10);

	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 2));
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, reg84);
//	    NSLog(@"%s:%d %x %x %x", __func__, __LINE__, reg80, reg82, reg84);
	    usleep(2000);
	}

	private void rtl8225_write_bitbang(int addr, int data) {
	    int reg80, reg84, reg82;
	    int bangdata;
	    int i;

	    bangdata = (data << 4) | (addr & 0xf);

	    reg80 = rtl818x_ioread16(RTL818X_ADDR_RFPinsOutput) & 0xfff3;
	    reg82 = rtl818x_ioread16(RTL818X_ADDR_RFPinsEnable);

	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsEnable, reg82 | 0x7);

	    reg84 = rtl818x_ioread16(RTL818X_ADDR_RFPinsSelect);
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, reg84 | 0x7);
	    usleep(10);

	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 2));
	    usleep(2);
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80);
	    usleep(10);

	    for (i = 15; i >= 0; i--) {
	        int reg = reg80 | (bangdata & (1 << i)) >> i;

	        if ((i & 1) != 0)
	            rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg);

	        rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg | (1 << 1));
	        rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg | (1 << 1));

	        if ((i & 1) == 0)
	            rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg);
	    }

	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 2));
	    usleep(10);

	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 2));
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, reg84);
	    usleep(2);
	}

	private void rtl8225_write(int addr, int data) {
	    if (asic_rev != 0)
	        rtl8225_write_8051(addr, data);
	    else
	        rtl8225_write_bitbang(addr, data);
	}

	private int rtl8225_read(int addr) {
		int reg80, reg82, reg84, out;
		int i;

		reg80 = rtl818x_ioread16(RTL818X_ADDR_RFPinsOutput);
		reg82 = rtl818x_ioread16(RTL818X_ADDR_RFPinsEnable);
		reg84 = rtl818x_ioread16(RTL818X_ADDR_RFPinsSelect);

		//	    NSLog(@"%s:%d %x %x %x", __func__, __LINE__, reg80, reg82, reg84);

		reg80 &= ~0xF;

		rtl818x_iowrite16(RTL818X_ADDR_RFPinsEnable, reg82 | 0x000F);
		rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, reg84 | 0x000F);

		rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 2));
		usleep(4);
		rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80);
		usleep(5);

		for (i = 4; i >= 0; i--) {
			int reg = reg80 | ((addr >> i) & 1);

			if ((i & 1) == 0) {
				rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg);
				usleep(1);
			}

			rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg | (1 << 1));
			usleep(2);
			rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg | (1 << 1));
			usleep(2);

			if ((i & 1) != 0) {
				rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg);
				usleep(1);
			}
		}

		rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 3) | (1 << 1));
		usleep(2);
		rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 3));
		usleep(2);
		rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 3));
		usleep(2);

		out = 0;
		for (i = 11; i >= 0; i--) {
			rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 3));
			usleep(1);
			rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 3) | (1 << 1));
			usleep(2);
			rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 3) | (1 << 1));
			usleep(2);
			rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 3) | (1 << 1));
			usleep(2);

			if ((rtl818x_ioread16(RTL818X_ADDR_RFPinsInput) & (1 << 1)) != 0)
				out |= 1 << i;

			rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 3));
			usleep(2);
		}

		rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, reg80 | (1 << 3) | (1 << 2));
		usleep(2);

		rtl818x_iowrite16(RTL818X_ADDR_RFPinsEnable, reg82);
		rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, reg84);
		rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, 0x03A0);
		//NSLog(@"out %u", out);
		return out;
	}
	
	private void rtl8187_write_phy(int addr, int data) {
	    data <<= 8;
	    data |= addr | 0x80;

	    rtl818x_iowrite8(RTL818X_ADDR_PHY3, (data >> 24) & 0xFF);
	    rtl818x_iowrite8(RTL818X_ADDR_PHY2, (data >> 16) & 0xFF);
	    rtl818x_iowrite8(RTL818X_ADDR_PHY1, (data >> 8) & 0xFF);
	    rtl818x_iowrite8(RTL818X_ADDR_PHY0, data & 0xFF);

	    usleep(1000);
	}

	
	private void rtl8225_write_phy_ofdm(int addr, int data) {
	    rtl8187_write_phy(addr, data);
	}
	
	private void rtl8225_write_phy_cck(int addr, int data) {
	    rtl8187_write_phy(addr, data | 0x10000);
	}

	private void rtl8225_rf_set_tx_power(int channel) {
	    int cck_power, ofdm_power;
	    int tmp;
	    int reg;
	    int i;

	    cck_power = channels[channel - 1][2] & 0xF;
	    ofdm_power = channels[channel - 1][2] >> 4;

		if (cck_power > 11)
			cck_power = 11;
		if (ofdm_power > 35)
			ofdm_power = 35;
		
	    rtl818x_iowrite8(RTL818X_ADDR_TX_GAIN_CCK, rtl8225_tx_gain_cck_ofdm[cck_power / 6] >> 1);

	    for (i = 0; i < 8; i++) {
		    if (channel == 14)
		        tmp = rtl8225_tx_power_cck_ch14[((cck_power % 6) * 8) + i];
		    else
		        tmp = rtl8225_tx_power_cck[((cck_power % 6) * 8) + 1];

	        rtl8225_write_phy_cck(0x44 + i, tmp);
	    }

	    usleep(1000); // FIXME: optional?

	    /* anaparam2 on */
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_CONFIG);
	    reg = rtl818x_ioread8(RTL818X_ADDR_CONFIG3);
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG3, reg | RTL818X_CONFIG3_ANAPARAM_WRITE);
	    rtl818x_iowrite32(RTL818X_ADDR_ANAPARAM2, RTL8225_ANAPARAM2_ON);
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG3, reg & ~RTL818X_CONFIG3_ANAPARAM_WRITE);
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_NORMAL);

	    rtl8225_write_phy_ofdm(2, 0x42);
	    rtl8225_write_phy_ofdm(6, 0x00);
	    rtl8225_write_phy_ofdm(8, 0x00);

	    rtl818x_iowrite8(RTL818X_ADDR_TX_GAIN_OFDM, rtl8225_tx_gain_cck_ofdm[ofdm_power / 6] >> 1);

	    tmp = rtl8225_tx_power_ofdm[ofdm_power % 6];

	    rtl8225_write_phy_ofdm(5, tmp);
	    rtl8225_write_phy_ofdm(7, tmp);

	    usleep(1000);
	}

	private void rtl8225z2_rf_set_tx_power(int channel) {
		int cck_power, ofdm_power;
		int reg;
		int i;
		int numch = 0;

		Log.d(TAG, "rtl8225z2 set tx power");
		
		cck_power = channels[channel - 1][2] & 0xF;
		ofdm_power = channels[channel - 1][2] >> 4;

		if (cck_power > 15)
			cck_power = 15;
		
		cck_power += txpwr_base & 0xf;
		
		if (cck_power > 35)
			cck_power = 35;
			
	    if (ofdm_power > 15)
	        ofdm_power = 25;
	    else
	        ofdm_power += 10;
	    
	    ofdm_power += txpwr_base >> 4;
		
		if (ofdm_power > 35)
			ofdm_power = 35;

		numch = 0;
	    if (channel == 14) {	        
		    for (i = 0; i < 8; i++) {
		        rtl8225_write_phy_cck(0x44 + i, rtl8225z2_tx_power_cck_ch14[numch++]);
		    }
	    } else {
		    for (i = 0; i < 8; i++) {
		        rtl8225_write_phy_cck(0x44 + i, rtl8225z2_tx_power_cck[numch++]);
		    }

	    }

	    rtl818x_iowrite8(RTL818X_ADDR_TX_GAIN_CCK,
	             rtl8225z2_tx_gain_cck_ofdm[cck_power]);
	    usleep(1);

	    /* anaparam2 on */
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_CONFIG);
	    reg = rtl818x_ioread8(RTL818X_ADDR_CONFIG3);
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG3,
	            reg | RTL818X_CONFIG3_ANAPARAM_WRITE);
	    rtl818x_iowrite32(RTL818X_ADDR_ANAPARAM2,
	              RTL8187_RTL8225_ANAPARAM2_ON);
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG3,
	            reg & ~RTL818X_CONFIG3_ANAPARAM_WRITE);
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_NORMAL);
	    rtl8225_write_phy_ofdm(2, 0x42);
	    rtl8225_write_phy_ofdm(5, 0x00);
	    rtl8225_write_phy_ofdm(6, 0x40);
	    rtl8225_write_phy_ofdm(7, 0x00);
	    rtl8225_write_phy_ofdm(8, 0x40);

	    rtl818x_iowrite8(RTL818X_ADDR_TX_GAIN_OFDM,
	             rtl8225z2_tx_gain_cck_ofdm[ofdm_power]);
	    usleep(1);
	}

	
	private void rtl8225_rf_init() {
	    int i;
	    
	    rtl8225_write(0x0, 0x067);
	    rtl8225_write(0x1, 0xFE0);
	    rtl8225_write(0x2, 0x44D);
	    rtl8225_write(0x3, 0x441);
	    rtl8225_write(0x4, 0x486);
	    rtl8225_write(0x5, 0xBC0);
	    rtl8225_write(0x6, 0xAE6);
	    rtl8225_write(0x7, 0x82A);
	    rtl8225_write(0x8, 0x01F);
	    rtl8225_write(0x9, 0x334);
	    rtl8225_write(0xA, 0xFD4);
	    rtl8225_write(0xB, 0x391);
	    rtl8225_write(0xC, 0x050);
	    rtl8225_write(0xD, 0x6DB);
	    rtl8225_write(0xE, 0x029);
	    rtl8225_write(0xF, 0x914); usleep(100);

	    rtl8225_write(0x2, 0xC4D); usleep(200);
	    rtl8225_write(0x2, 0x44D); usleep(200);

	    if ((rtl8225_read(6) & (1 << 7)) == 0) {
	        rtl8225_write(0x02, 0x0c4d);
	        usleep(200);
	        rtl8225_write(0x02, 0x044d);
	        usleep(100);
	        if ((rtl8225_read(6) & (1 << 7)) == 0)
	            Log.e(TAG, "RF Calibration Failed! " + Integer.toHexString(rtl8225_read(6)));
	    }

	    rtl8225_write(0x0, 0x127);

	    for (i = 0; i < rtl8225bcd_rxgain.length; i++) {
	        rtl8225_write(0x1, i + 1);
	        rtl8225_write(0x2, rtl8225bcd_rxgain[i]);
	    }

	    rtl8225_write(0x0, 0x027);
	    rtl8225_write(0x0, 0x22F);

	    for (i = 0; i < rtl8225_agc.length; i++) {
	        rtl8225_write_phy_ofdm(0xB, rtl8225_agc[i]);
	        usleep(1000);
	        rtl8225_write_phy_ofdm(0xA, 0x80 + i);
	        usleep(1000);
	    }

	    usleep(1000);

	    rtl8225_write_phy_ofdm(0x00, 0x01); usleep(1000);
	    rtl8225_write_phy_ofdm(0x01, 0x02); usleep(1000);
	    rtl8225_write_phy_ofdm(0x02, 0x42); usleep(1000);
	    rtl8225_write_phy_ofdm(0x03, 0x00); usleep(1000);
	    rtl8225_write_phy_ofdm(0x04, 0x00); usleep(1000);
	    rtl8225_write_phy_ofdm(0x05, 0x00); usleep(1000);
	    rtl8225_write_phy_ofdm(0x06, 0x40); usleep(1000);
	    rtl8225_write_phy_ofdm(0x07, 0x00); usleep(1000);
	    rtl8225_write_phy_ofdm(0x08, 0x40); usleep(1000);
	    rtl8225_write_phy_ofdm(0x09, 0xfe); usleep(1000);
	    rtl8225_write_phy_ofdm(0x0a, 0x09); usleep(1000);
	    rtl8225_write_phy_ofdm(0x0b, 0x80); usleep(1000);
	    rtl8225_write_phy_ofdm(0x0c, 0x01); usleep(1000);
	    rtl8225_write_phy_ofdm(0x0e, 0xd3); usleep(1000);
	    rtl8225_write_phy_ofdm(0x0f, 0x38); usleep(1000);
	    rtl8225_write_phy_ofdm(0x10, 0x84); usleep(1000);
	    rtl8225_write_phy_ofdm(0x11, 0x06); usleep(1000);
	    rtl8225_write_phy_ofdm(0x12, 0x20); usleep(1000);
	    rtl8225_write_phy_ofdm(0x13, 0x20); usleep(1000);
	    rtl8225_write_phy_ofdm(0x14, 0x00); usleep(1000);
	    rtl8225_write_phy_ofdm(0x15, 0x40); usleep(1000);
	    rtl8225_write_phy_ofdm(0x16, 0x00); usleep(1000);
	    rtl8225_write_phy_ofdm(0x17, 0x40); usleep(1000);
	    rtl8225_write_phy_ofdm(0x18, 0xef); usleep(1000);
	    rtl8225_write_phy_ofdm(0x19, 0x19); usleep(1000);
	    rtl8225_write_phy_ofdm(0x1a, 0x20); usleep(1000);
	    rtl8225_write_phy_ofdm(0x1b, 0x76); usleep(1000);
	    rtl8225_write_phy_ofdm(0x1c, 0x04); usleep(1000);
	    rtl8225_write_phy_ofdm(0x1e, 0x95); usleep(1000);
	    rtl8225_write_phy_ofdm(0x1f, 0x75); usleep(1000);
	    rtl8225_write_phy_ofdm(0x20, 0x1f); usleep(1000);
	    rtl8225_write_phy_ofdm(0x21, 0x27); usleep(1000);
	    rtl8225_write_phy_ofdm(0x22, 0x16); usleep(1000);
	    rtl8225_write_phy_ofdm(0x24, 0x46); usleep(1000);
	    rtl8225_write_phy_ofdm(0x25, 0x20); usleep(1000);
	    rtl8225_write_phy_ofdm(0x26, 0x90); usleep(1000);
	    rtl8225_write_phy_ofdm(0x27, 0x88); usleep(1000);

	    rtl8225_write_phy_ofdm(0x0d, rtl8225_gain[2 * 4]);
	    rtl8225_write_phy_ofdm(0x1b, rtl8225_gain[2 * 4 + 2]);
	    rtl8225_write_phy_ofdm(0x1d, rtl8225_gain[2 * 4 + 3]);
	    rtl8225_write_phy_ofdm(0x23, rtl8225_gain[2 * 4 + 1]);

	    rtl8225_write_phy_cck(0x00, 0x98); usleep(1000);
	    rtl8225_write_phy_cck(0x03, 0x20); usleep(1000);
	    rtl8225_write_phy_cck(0x04, 0x7e); usleep(1000);
	    rtl8225_write_phy_cck(0x05, 0x12); usleep(1000);
	    rtl8225_write_phy_cck(0x06, 0xfc); usleep(1000);
	    rtl8225_write_phy_cck(0x07, 0x78); usleep(1000);
	    rtl8225_write_phy_cck(0x08, 0x2e); usleep(1000);
	    rtl8225_write_phy_cck(0x10, 0x9b); usleep(1000);
	    rtl8225_write_phy_cck(0x11, 0x88); usleep(1000);
	    rtl8225_write_phy_cck(0x12, 0x47); usleep(1000);
	    rtl8225_write_phy_cck(0x13, 0xd0);
	    rtl8225_write_phy_cck(0x19, 0x00);
	    rtl8225_write_phy_cck(0x1a, 0xa0);
	    rtl8225_write_phy_cck(0x1b, 0x08);
	    rtl8225_write_phy_cck(0x40, 0x86);
	    rtl8225_write_phy_cck(0x41, 0x8d); usleep(1000);
	    rtl8225_write_phy_cck(0x42, 0x15); usleep(1000);
	    rtl8225_write_phy_cck(0x43, 0x18); usleep(1000);
	    rtl8225_write_phy_cck(0x44, 0x1f); usleep(1000);
	    rtl8225_write_phy_cck(0x45, 0x1e); usleep(1000);
	    rtl8225_write_phy_cck(0x46, 0x1a); usleep(1000);
	    rtl8225_write_phy_cck(0x47, 0x15); usleep(1000);
	    rtl8225_write_phy_cck(0x48, 0x10); usleep(1000);
	    rtl8225_write_phy_cck(0x49, 0x0a); usleep(1000);
	    rtl8225_write_phy_cck(0x4a, 0x05); usleep(1000);
	    rtl8225_write_phy_cck(0x4b, 0x02); usleep(1000);
	    rtl8225_write_phy_cck(0x4c, 0x05); usleep(1000);

	    rtl818x_iowrite8(RTL818X_ADDR_TESTR, 0x0D);

	    rtl8225_rf_set_tx_power(1);

	    /* RX antenna default to A */
	    rtl8225_write_phy_cck(0x10, 0x9b); usleep(1000);  /* B: 0xDB */
	    rtl8225_write_phy_ofdm(0x26, 0x90); usleep(1000); /* B: 0x10 */

	    rtl818x_iowrite8(RTL818X_ADDR_TX_ANTENNA, 0x03);  /* B: 0x00 */
	    usleep(1000);
	    rtl818x_iowrite32(0xFF94, 0x3dc00002);
	    /* set sensitivity */
	    rtl8225_write(0x0c, 0x50);
	    rtl8225_write_phy_ofdm(0x0d, rtl8225_gain[2 * 4]);
	    rtl8225_write_phy_ofdm(0x1b, rtl8225_gain[2 * 4 + 2]);
	    rtl8225_write_phy_ofdm(0x1d, rtl8225_gain[2 * 4 + 3]);
	    rtl8225_write_phy_ofdm(0x23, rtl8225_gain[2 * 4 + 1]);
	    rtl8225_write_phy_cck(0x41, rtl8225_threshold[2]);
	}

	private void rtl8225z2_b_rf_init() {
	    int i;

	    Log.d(TAG, "8225z2b rf_init");
	    
	    rtl8225_write(0x0, 0x0B7);
	    rtl8225_write(0x1, 0xEE0);
	    rtl8225_write(0x2, 0x44D);
	    rtl8225_write(0x3, 0x441);
	    rtl8225_write(0x4, 0x8C3);
	    rtl8225_write(0x5, 0xC72);
	    rtl8225_write(0x6, 0x0E6);
	    rtl8225_write(0x7, 0x82A);
	    rtl8225_write(0x8, 0x03F);
	    rtl8225_write(0x9, 0x335);
	    rtl8225_write(0xa, 0x9D4);
	    rtl8225_write(0xb, 0x7BB);
	    rtl8225_write(0xc, 0x850);
	    rtl8225_write(0xd, 0xCDF);
	    rtl8225_write(0xe, 0x02B);
	    rtl8225_write(0xf, 0x114);

	    rtl8225_write(0x0, 0x1B7);

	    for (i = 0; i < rtl8225z2_rxgain.length; i++) {
	        rtl8225_write(0x1, i + 1);
	        rtl8225_write(0x2, rtl8225z2_rxgain[i]);
	    }

	    rtl8225_write(0x3, 0x080);
	    rtl8225_write(0x5, 0x004);
	    rtl8225_write(0x0, 0x0B7);

	    rtl8225_write(0x2, 0xC4D);

	    rtl8225_write(0x2, 0x44D);
	    rtl8225_write(0x0, 0x2BF);
	    rtl818x_iowrite8(RTL818X_ADDR_TX_GAIN_CCK, 0x03);
	    rtl818x_iowrite8(RTL818X_ADDR_TX_GAIN_OFDM, 0x07);
	    rtl818x_iowrite8(RTL818X_ADDR_TX_ANTENNA, 0x03);

	    rtl8225_write_phy_ofdm(0x80, 0x12);
	    for (i = 0; i < rtl8225z2_agc.length; i++) {
	        rtl8225_write_phy_ofdm(0xF, rtl8225z2_agc[i]);
	        rtl8225_write_phy_ofdm(0xE, 0x80 + i);
	        rtl8225_write_phy_ofdm(0xE, 0);
	    }
	    rtl8225_write_phy_ofdm(0x80, 0x10);

	    for (i = 0; i < rtl8225z2_ofdm.length; i++)
	        rtl8225_write_phy_ofdm(i, rtl8225z2_ofdm[i]);

	    rtl8225_write_phy_ofdm(0x97, 0x46);
	    rtl8225_write_phy_ofdm(0xa4, 0xb6);
	    rtl8225_write_phy_ofdm(0x85, 0xfc);
	    rtl8225_write_phy_cck(0xc1, 0x88);
	}

	private void rtl8225z2_rf_init() {
	    int i;

	    rtl8225_write(0x0, 0x2BF);
	    rtl8225_write(0x1, 0xEE0);
	    rtl8225_write(0x2, 0x44D);
	    rtl8225_write(0x3, 0x441);
	    rtl8225_write(0x4, 0x8C3);
	    rtl8225_write(0x5, 0xC72);
	    rtl8225_write(0x6, 0x0E6);
	    rtl8225_write(0x7, 0x82A);
	    rtl8225_write(0x8, 0x03F);
	    rtl8225_write(0x9, 0x335);
	    rtl8225_write(0xa, 0x9D4);
	    rtl8225_write(0xb, 0x7BB);
	    rtl8225_write(0xc, 0x850);
	    rtl8225_write(0xd, 0xCDF);
	    rtl8225_write(0xe, 0x02B);
	    rtl8225_write(0xf, 0x114);
	    usleep(100);

	    rtl8225_write(0x0, 0x1B7);

	    for (i = 0; i < rtl8225z2_rxgain.length; i++) {
	        rtl8225_write(0x1, i + 1);
	        rtl8225_write(0x2, rtl8225z2_rxgain[i]);
	    }

	    rtl8225_write(0x3, 0x080);
	    rtl8225_write(0x5, 0x004);
	    rtl8225_write(0x0, 0x0B7);
	    rtl8225_write(0x2, 0xc4D);

	    usleep(200);
	    rtl8225_write(0x2, 0x44D);
	    usleep(100);

	    if ((rtl8225_read(6) & (1 << 7)) == 0) {
	        rtl8225_write(0x02, 0x0C4D);
	        usleep(200);
	        rtl8225_write(0x02, 0x044D);
	        usleep(100);
	        if ((rtl8225_read(6) & (1 << 7)) == 0)
	        	Log.e(TAG, "8187z2 rf calibration failed: " + Integer.toHexString(rtl8225_read(6)));
	    }

	    usleep(200);

	    rtl8225_write(0x0, 0x2BF);

	    for (i = 0; i < rtl8225_agc.length; i++) {
	        rtl8225_write_phy_ofdm(0xB, rtl8225_agc[i]);
	        rtl8225_write_phy_ofdm(0xA, 0x80 + i);
	    }

	    usleep(1);

	    rtl8225_write_phy_ofdm(0x00, 0x01);
	    rtl8225_write_phy_ofdm(0x01, 0x02);
	    rtl8225_write_phy_ofdm(0x02, 0x42);
	    rtl8225_write_phy_ofdm(0x03, 0x00);
	    rtl8225_write_phy_ofdm(0x04, 0x00);
	    rtl8225_write_phy_ofdm(0x05, 0x00);
	    rtl8225_write_phy_ofdm(0x06, 0x40);
	    rtl8225_write_phy_ofdm(0x07, 0x00);
	    rtl8225_write_phy_ofdm(0x08, 0x40);
	    rtl8225_write_phy_ofdm(0x09, 0xfe);
	    rtl8225_write_phy_ofdm(0x0a, 0x08);
	    rtl8225_write_phy_ofdm(0x0b, 0x80);
	    rtl8225_write_phy_ofdm(0x0c, 0x01);
	    rtl8225_write_phy_ofdm(0x0d, 0x43);
	    rtl8225_write_phy_ofdm(0x0e, 0xd3);
	    rtl8225_write_phy_ofdm(0x0f, 0x38);
	    rtl8225_write_phy_ofdm(0x10, 0x84);
	    rtl8225_write_phy_ofdm(0x11, 0x07);
	    rtl8225_write_phy_ofdm(0x12, 0x20);
	    rtl8225_write_phy_ofdm(0x13, 0x20);
	    rtl8225_write_phy_ofdm(0x14, 0x00);
	    rtl8225_write_phy_ofdm(0x15, 0x40);
	    rtl8225_write_phy_ofdm(0x16, 0x00);
	    rtl8225_write_phy_ofdm(0x17, 0x40);
	    rtl8225_write_phy_ofdm(0x18, 0xef);
	    rtl8225_write_phy_ofdm(0x19, 0x19);
	    rtl8225_write_phy_ofdm(0x1a, 0x20);
	    rtl8225_write_phy_ofdm(0x1b, 0x15);
	    rtl8225_write_phy_ofdm(0x1c, 0x04);
	    rtl8225_write_phy_ofdm(0x1d, 0xc5);
	    rtl8225_write_phy_ofdm(0x1e, 0x95);
	    rtl8225_write_phy_ofdm(0x1f, 0x75);
	    rtl8225_write_phy_ofdm(0x20, 0x1f);
	    rtl8225_write_phy_ofdm(0x21, 0x17);
	    rtl8225_write_phy_ofdm(0x22, 0x16);
	    rtl8225_write_phy_ofdm(0x23, 0x80);
	    rtl8225_write_phy_ofdm(0x24, 0x46);
	    rtl8225_write_phy_ofdm(0x25, 0x00);
	    rtl8225_write_phy_ofdm(0x26, 0x90);
	    rtl8225_write_phy_ofdm(0x27, 0x88);

	    rtl8225_write_phy_ofdm(0x0b, rtl8225z2_gain_bg[4 * 3]);
	    rtl8225_write_phy_ofdm(0x1b, rtl8225z2_gain_bg[4 * 3 + 1]);
	    rtl8225_write_phy_ofdm(0x1d, rtl8225z2_gain_bg[4 * 3 + 2]);
	    rtl8225_write_phy_ofdm(0x21, 0x37);

	    rtl8225_write_phy_cck(0x00, 0x98);
	    rtl8225_write_phy_cck(0x03, 0x20);
	    rtl8225_write_phy_cck(0x04, 0x7e);
	    rtl8225_write_phy_cck(0x05, 0x12);
	    rtl8225_write_phy_cck(0x06, 0xfc);
	    rtl8225_write_phy_cck(0x07, 0x78);
	    rtl8225_write_phy_cck(0x08, 0x2e);
	    rtl8225_write_phy_cck(0x10, 0x9b);
	    rtl8225_write_phy_cck(0x11, 0x88);
	    rtl8225_write_phy_cck(0x12, 0x47);
	    rtl8225_write_phy_cck(0x13, 0xd0);
	    rtl8225_write_phy_cck(0x19, 0x00);
	    rtl8225_write_phy_cck(0x1a, 0xa0);
	    rtl8225_write_phy_cck(0x1b, 0x08);
	    rtl8225_write_phy_cck(0x40, 0x86);
	    rtl8225_write_phy_cck(0x41, 0x8d);
	    rtl8225_write_phy_cck(0x42, 0x15);
	    rtl8225_write_phy_cck(0x43, 0x18);
	    rtl8225_write_phy_cck(0x44, 0x36);
	    rtl8225_write_phy_cck(0x45, 0x35);
	    rtl8225_write_phy_cck(0x46, 0x2e);
	    rtl8225_write_phy_cck(0x47, 0x25);
	    rtl8225_write_phy_cck(0x48, 0x1c);
	    rtl8225_write_phy_cck(0x49, 0x12);
	    rtl8225_write_phy_cck(0x4a, 0x09);
	    rtl8225_write_phy_cck(0x4b, 0x04);
	    rtl8225_write_phy_cck(0x4c, 0x05);

	    rtl818x_iowrite8(0xFF5B, 0x0D); usleep(1);

	    rtl8225z2_rf_set_tx_power(1);

	    /* RX antenna default to A */
	    rtl8225_write_phy_cck(0x10, 0x9b);         /* B: 0xDB */
	    rtl8225_write_phy_ofdm(0x26, 0x90);        /* B: 0x10 */

	    rtl818x_iowrite8(RTL818X_ADDR_TX_ANTENNA, 0x03);   /* B: 0x00 */
	    usleep(1);
	    rtl818x_iowrite32(0xFF94, 0x3dc00002);
	}

	
	private void rtl8225_rf_init_common() {
		if (rf_type == rf_rtl8225)
			rtl8225_rf_init();
		else if (rf_type == rf_rtl8225z2)
			rtl8225z2_rf_init();
		else if (rf_type == rf_rtl8225z2b)
			rtl8225z2_b_rf_init();
		else
			Log.e(TAG, "Can't init rf, couldn't handle type " + rf_type);
	}
	
	private void rtl8225_rf_set_channel(int channel) {
		if (rf_init_type == RTL8225_RF_INIT) 
	        rtl8225_rf_set_tx_power(channel);
	   // else
	        // rtl8225z2_rf_set_tx_power(channel);

	    rtl8225_write(0x7, rtl8225_chan[channel - 1]);
	    usleep(10000);
	}

	
	private void rtl8187_set_channel(int channel) {
		synchronized (control_lock) {
			int reg;

			reg = rtl818x_ioread32(RTL818X_ADDR_TX_CONF);
			/* Enable TX loopback on MAC level to avoid TX during channel
			 * changes, as this has be seen to causes problems and the
			 * card will stop work until next reset
			 */
			rtl818x_iowrite32(RTL818X_ADDR_TX_CONF, reg | RTL818X_TX_CONF_LOOPBACK_MAC);
			usleep(10000);
			rtl8225_rf_set_channel(channel);
			usleep(10000);
			rtl818x_iowrite32(RTL818X_ADDR_TX_CONF, reg);

			Log.d(TAG, "Set channel " + channel);
		}
	}
	
	@Override
	public void setChannel(int c) {
		super.setChannel(c);
		rtl8187_set_channel(c);
	}

	private void rtl8187_detect_rf() {
	    int reg8, reg9;

	    if (is_rtl8187b == 0) {
	        rtl8225_write(0, 0x1B7);

	        reg8 = rtl8225_read(8);
	        reg9 = rtl8225_read(9);

	        rtl8225_write(0, 0x0B7);

	        if (reg8 != 0x588 || reg9 != 0x700) {
	        	rf_type = rf_rtl8225;
	        	return;
	        }

	        rf_type = rf_rtl8225z2;
	        return;
	    } else
	    	rf_type = rf_rtl8225z2b;
	}
	
	private int rtl8187_init_hw() {
	    int reg;
	    int i;

	    /* reset */
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_CONFIG);
	    reg = rtl818x_ioread8(RTL818X_ADDR_CONFIG3);
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG3, reg | RTL818X_CONFIG3_ANAPARAM_WRITE);
	    rtl818x_iowrite32(RTL818X_ADDR_ANAPARAM, RTL8225_ANAPARAM_ON);
	    rtl818x_iowrite32(RTL818X_ADDR_ANAPARAM2, RTL8225_ANAPARAM2_ON);
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG3, reg & ~RTL818X_CONFIG3_ANAPARAM_WRITE);
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_NORMAL);

	    rtl818x_iowrite16(RTL818X_ADDR_INT_MASK, 0);

	    usleep(200000);
	    rtl818x_iowrite8(0xFE18, 0x10);
	    rtl818x_iowrite8(0xFE18, 0x11);
	    rtl818x_iowrite8(0xFE18, 0x00);
	    usleep(200000);
	    
	    reg = rtl818x_ioread8(RTL818X_ADDR_CMD);
	    reg &= (1 << 1);
	    reg |= RTL818X_CMD_RESET;
	    rtl818x_iowrite8(RTL818X_ADDR_CMD, reg);

	    i = 10;
	    do {
	        usleep(2000);
	        if ((rtl818x_ioread8(RTL818X_ADDR_CMD) &
	              RTL818X_CMD_RESET) == 0)
	            break;
	    } while (--i != 0);
	    if (i == 0) {
	    	Log.d(TAG, "Reset timeout!");
	        return -1;
	    }

	    /* reload registers from eeprom */
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_LOAD);

	    i = 10;
	    do {
	        usleep(4000);
	        if ((rtl818x_ioread8(RTL818X_ADDR_EEPROM_CMD) &
	              RTL818X_EEPROM_CMD_CONFIG) == 0)
	            break;
	    } while (--i != 0);

	    if (i == 0) {
	    	Log.d(TAG, "eeprom reset timeout");
	        return -1;
	    }

	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_CONFIG);
	    reg = rtl818x_ioread8(RTL818X_ADDR_CONFIG3);
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG3, reg | RTL818X_CONFIG3_ANAPARAM_WRITE);
	    rtl818x_iowrite32(RTL818X_ADDR_ANAPARAM, RTL8225_ANAPARAM_ON);
	    rtl818x_iowrite32(RTL818X_ADDR_ANAPARAM2, RTL8225_ANAPARAM2_ON);
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG3, reg & ~RTL818X_CONFIG3_ANAPARAM_WRITE);
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_NORMAL);

	    /* setup card */
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, 0);
	    rtl818x_iowrite8(RTL818X_ADDR_GPIO, 0);

	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, (4 << 8));
	    rtl818x_iowrite8(RTL818X_ADDR_GPIO, 1);
	    rtl818x_iowrite8(RTL818X_ADDR_GP_ENABLE, 0);
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_CONFIG);

	    rtl818x_iowrite16(0xFFF4, 0xFFFF);
	    reg = rtl818x_ioread8(RTL818X_ADDR_CONFIG1);
	    reg &= 0x3F;
	    reg |= 0x80;
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG1, reg);

	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_NORMAL);

	    rtl818x_iowrite32(RTL818X_ADDR_INT_TIMEOUT, 0);
	    rtl818x_iowrite8(RTL818X_ADDR_WPA_CONF, 0);
	    rtl818x_iowrite8(RTL818X_ADDR_RATE_FALLBACK, 0x81);

	    // TODO: set RESP_RATE and BRSR properly
	    rtl818x_iowrite8(RTL818X_ADDR_RESP_RATE, (8 << 4) | 0);
	    rtl818x_iowrite16(RTL818X_ADDR_BRSR, 0x01F3);

	    /* host_usb_init */
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, 0);
	    rtl818x_iowrite8(RTL818X_ADDR_GPIO, 0);
	    reg = rtl818x_ioread8(0xFE53);
	    rtl818x_iowrite8(0xFE53, reg | (1 << 7));
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, (4 << 8));
	    rtl818x_iowrite8(RTL818X_ADDR_GPIO, 0x20);
	    rtl818x_iowrite8(RTL818X_ADDR_GP_ENABLE, 0);
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, 0x80);
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, 0x80);
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsEnable, 0x80);

	    usleep(100000);
	    
	    rtl818x_iowrite32(RTL818X_ADDR_RF_TIMING, 0x000a8008);
	    rtl818x_iowrite16(RTL818X_ADDR_BRSR, 0xFFFF);
	    rtl818x_iowrite32(RTL818X_ADDR_RF_PARA, 0x00100044);
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_CONFIG);
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG3, 0x44);
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_NORMAL);
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsEnable, 0x1FF7);
	    usleep(100000);

	   //  priv->rf_init(priv);
	    rtl8225_rf_init_common();

	    rtl818x_iowrite16(RTL818X_ADDR_BRSR, 0x01F3);
	    reg = rtl818x_ioread8(RTL818X_ADDR_PGSELECT) & ~1;
	    rtl818x_iowrite8(RTL818X_ADDR_PGSELECT, reg | 1);
	    rtl818x_iowrite16(0xFFFE, 0x10);
	    rtl818x_iowrite8(RTL818X_ADDR_TALLY_SEL, 0x80);
	    rtl818x_iowrite8(0xFFFF, 0x60);
	    rtl818x_iowrite8(RTL818X_ADDR_PGSELECT, reg);

	    Log.d(TAG, "Initialized card hardware");
	    
	    return 0;
	}
	
	private void rtl8187_set_anaparam(boolean rfon) {
	    int anaparam, anaparam2;
	    int anaparam3 = 0, reg;

	    if (is_rtl8187b == 0) {
	        if (rfon) {
	            anaparam = RTL8187_RTL8225_ANAPARAM_ON;
	            anaparam2 = RTL8187_RTL8225_ANAPARAM2_ON;
	        } else {
	            anaparam = RTL8187_RTL8225_ANAPARAM_OFF;
	            anaparam2 = RTL8187_RTL8225_ANAPARAM2_OFF;
	        }
	    } else {
	        if (rfon) {
	            anaparam = RTL8187B_RTL8225_ANAPARAM_ON;
	            anaparam2 = RTL8187B_RTL8225_ANAPARAM2_ON;
	            anaparam3 = RTL8187B_RTL8225_ANAPARAM3_ON;
	        } else {
	            anaparam = RTL8187B_RTL8225_ANAPARAM_OFF;
	            anaparam2 = RTL8187B_RTL8225_ANAPARAM2_OFF;
	            anaparam3 = RTL8187B_RTL8225_ANAPARAM3_OFF;
	        }
	    }

	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_CONFIG);
	    reg = rtl818x_ioread8(RTL818X_ADDR_CONFIG3);
	    reg |= RTL818X_CONFIG3_ANAPARAM_WRITE;
	   
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG3, reg);
	    rtl818x_iowrite32(RTL818X_ADDR_ANAPARAM, anaparam);
	    rtl818x_iowrite32(RTL818X_ADDR_ANAPARAM2, anaparam2);
	    if (is_rtl8187b != 0)
	        rtl818x_iowrite8(RTL818X_ADDR_ANAPARAM3, anaparam3);
	    reg &= ~RTL818X_CONFIG3_ANAPARAM_WRITE;
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG3, reg);
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_NORMAL);
	}
	
	private int rtl8187_cmd_reset() {
	    int reg;
	    int i;

	    reg = rtl818x_ioread8(RTL818X_ADDR_CMD);
	    reg &= (1 << 1); 
	    reg |= RTL818X_CMD_RESET;
	    rtl818x_iowrite8(RTL818X_ADDR_CMD, reg);

	    i = 10;
	    do {
	        usleep(2);
	        if ((rtl818x_ioread8(RTL818X_ADDR_CMD) & RTL818X_CMD_RESET) == 0)
	            break;
	    } while (--i != 0);
	    
	    if (i == 0) {
	    	Log.e(TAG, "Reset timed out");
	    	return -1;
	    }

	    /* reload registers from eeprom */
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_LOAD);
	    
	    i = 10;
	    do {
	        usleep(4);
	        if ((rtl818x_ioread8(RTL818X_ADDR_EEPROM_CMD) & RTL818X_EEPROM_CMD_CONFIG) == 0)
	            break;
	    } while (--i != 0);

	    if (i == 0) {
	    	Log.e(TAG, "eeprom reset timed out");
	    	return -1;
	    }

	    return 0;
	}   

	private int rtl8187b_init_hw() {
	    int res, i;
	    int reg;

	    rtl8187_set_anaparam(true);

	    /* Reset PLL sequence on 8187B. Realtek note: reduces power
	     * consumption about 30 mA */
	    rtl818x_iowrite8(0xFF61, 0x10);
	    reg = rtl818x_ioread8(0xFF62);
	    rtl818x_iowrite8(0xFF62, reg & ~(1 << 5));
	    rtl818x_iowrite8(0xFF62, reg | (1 << 5));

	    res = rtl8187_cmd_reset();
	    if (res != 0)
	        return res;

	    rtl8187_set_anaparam(true);

	    /* BRSR (Basic Rate Set Register) on 8187B looks to be the same as
	     * RESP_RATE on 8187L in Realtek sources: each bit should be each
	     * one of the 12 rates, all are enabled */
	    rtl818x_iowrite16(0xFF34, 0x0FFF);

	    reg = rtl818x_ioread8(RTL818X_ADDR_CW_CONF);
	    reg |= RTL818X_CW_CONF_PERPACKET_RETRY_SHIFT;
	    rtl818x_iowrite8(RTL818X_ADDR_CW_CONF, reg);

	    /* Auto Rate Fallback Register (ARFR): 1M-54M setting */
	    rtl818x_iowrite16_idx(0xFFE0, 0x0FFF, 1);
	    rtl818x_iowrite8_idx(0xFFE2, 0x00, 1);

	    rtl818x_iowrite16_idx(0xFFD4, 0xFFFF, 1);

	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_CONFIG);
	    reg = rtl818x_ioread8(RTL818X_ADDR_CONFIG1);
	    rtl818x_iowrite8(RTL818X_ADDR_CONFIG1, (reg & 0x3F) | 0x80);
	    rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_NORMAL);

	    rtl818x_iowrite8(RTL818X_ADDR_WPA_CONF, 0);
	    for (i = 0; i < rtl8187b_reg_table.length; i++) {
	        rtl818x_iowrite8_idx((rtl8187b_reg_table[i][0] | 0xFF00),
	                     rtl8187b_reg_table[i][1],
	                     rtl8187b_reg_table[i][2]);
	    }

	    rtl818x_iowrite16(RTL818X_ADDR_TID_AC_MAP, 0xFA50);
	    rtl818x_iowrite16(RTL818X_ADDR_INT_MIG, 0);

	    rtl818x_iowrite32_idx(0xFFF0, 0, 1);
	    rtl818x_iowrite32_idx(0xFFF4, 0, 1);
	    rtl818x_iowrite8_idx(0xFFF8, 0, 1);

	    rtl818x_iowrite32(RTL818X_ADDR_RF_TIMING, 0x00004001);

	    /* RFSW_CTRL register */
	    rtl818x_iowrite16_idx(0xFF72, 0x569A, 2);

	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsOutput, 0x0480);
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsSelect, 0x2488);
	    rtl818x_iowrite16(RTL818X_ADDR_RFPinsEnable, 0x1FFF);
	    usleep(100);

	    // TODO support 8255Z2
	    rtl8225_rf_init_common();

	    //priv->rf->init(dev);

	    reg = RTL818X_CMD_TX_ENABLE | RTL818X_CMD_RX_ENABLE;
	    rtl818x_iowrite8(RTL818X_ADDR_CMD, reg);
	    rtl818x_iowrite16(RTL818X_ADDR_INT_MASK, 0xFFFF);
	    rtl818x_iowrite8(0xFE41, 0xF4);
	    rtl818x_iowrite8(0xFE40, 0x00);
	    rtl818x_iowrite8(0xFE42, 0x00);
	    rtl818x_iowrite8(0xFE42, 0x01);
	    rtl818x_iowrite8(0xFE40, 0x0F);
	    rtl818x_iowrite8(0xFE42, 0x00);
	    rtl818x_iowrite8(0xFE42, 0x01);

	    reg = rtl818x_ioread8(0xFFDB);
	    rtl818x_iowrite8(0xFFDB, reg | (1 << 2));
	    rtl818x_iowrite16_idx(0xFF72, 0x59FA, 3);
	    rtl818x_iowrite16_idx(0xFF74, 0x59D2, 3);
	    rtl818x_iowrite16_idx(0xFF76, 0x59D2, 3);
	    rtl818x_iowrite16_idx(0xFF78, 0x19FA, 3);
	    rtl818x_iowrite16_idx(0xFF7A, 0x19FA, 3);
	    rtl818x_iowrite16_idx(0xFF7C, 0x00D0, 3);
	    rtl818x_iowrite8(0xFF61, 0);
	    rtl818x_iowrite8_idx(0xFF80, 0x0F, 1);
	    rtl818x_iowrite8_idx(0xFF83, 0x03, 1);
	    rtl818x_iowrite8(0xFFDA, 0x10);
	    rtl818x_iowrite8_idx(0xFF4D, 0x08, 2);

	    rtl818x_iowrite32(RTL818X_ADDR_HSSI_PARA, 0x0600321B);

	    rtl818x_iowrite16_idx(0xFFEC, 0x0800, 1);

	    slot_time = 0x9;
	    aifsn[0] = 2; /* AIFSN[AC_VO] */
	    aifsn[1] = 2; /* AIFSN[AC_VI] */
	    aifsn[2] = 7; /* AIFSN[AC_BK] */
	    aifsn[3] = 3; /* AIFSN[AC_BE] */
	    rtl818x_iowrite8(RTL818X_ADDR_ACM_CONTROL, 0);

	    /* ENEDCA flag must always be set, transmit issues? */
	    rtl818x_iowrite8(RTL818X_ADDR_MSR, RTL818X_MSR_ENEDCA);

	    return 0;
	}

	private int rtl8187_start() {
	    int reg;
	    int ret;

	    if (is_rtl8187b != 0)
	    	ret = rtl8187b_init_hw();
	    else
	    	ret = rtl8187_init_hw();
	    if (ret != 0)
	        return ret;

	    rtl818x_iowrite16(RTL818X_ADDR_INT_MASK, 0xFFFF);

	    rtl818x_iowrite32(RTL818X_ADDR_MAR0, ~0);
	    rtl818x_iowrite32(RTL818X_ADDR_MAR1, ~0);

	//  rtl8187_init_urbs(dev);

	    reg = RTL818X_RX_CONF_ONLYERLPKT |
	    RTL818X_RX_CONF_RX_AUTORESETPHY |
	    RTL818X_RX_CONF_BSSID |
	    RTL818X_RX_CONF_MGMT |
	    RTL818X_RX_CONF_DATA |
	    RTL818X_RX_CONF_CTRL |
	    (7 << 13 /* RX FIFO threshold NONE */) |
	    (7 << 10 /* MAX RX DMA */) |
	    RTL818X_RX_CONF_BROADCAST |
	    RTL818X_RX_CONF_NICMAC |
	    RTL818X_RX_CONF_MONITOR;
	    rx_conf = reg;
	    rtl818x_iowrite32(RTL818X_ADDR_RX_CONF, reg);

	    reg = rtl818x_ioread8(RTL818X_ADDR_CW_CONF);
	    reg &= ~RTL818X_CW_CONF_PERPACKET_CW_SHIFT;
	    reg |= RTL818X_CW_CONF_PERPACKET_RETRY_SHIFT;
	    rtl818x_iowrite8(RTL818X_ADDR_CW_CONF, reg);
	    reg = rtl818x_ioread8(RTL818X_ADDR_TX_AGC_CTL);
	    reg &= ~RTL818X_TX_AGC_CTL_PERPACKET_GAIN_SHIFT;
	    reg &= ~RTL818X_TX_AGC_CTL_PERPACKET_ANTSEL_SHIFT;
	    reg &= ~RTL818X_TX_AGC_CTL_FEEDBACK_ANT;
	    rtl818x_iowrite8(RTL818X_ADDR_TX_AGC_CTL, reg);

	    reg  = RTL818X_TX_CONF_CW_MIN |
	    (7 << 21 /* MAX TX DMA */) |
	    RTL818X_TX_CONF_NO_ICV;
	    rtl818x_iowrite32(RTL818X_ADDR_TX_CONF, reg);

	    reg = rtl818x_ioread8(RTL818X_ADDR_CMD);
	    reg |= RTL818X_CMD_TX_ENABLE;
	    reg |= RTL818X_CMD_RX_ENABLE;
	    rtl818x_iowrite8(RTL818X_ADDR_CMD, reg);

	    Log.d(TAG, "Started rtl8187");
	    return 0;
	}
	
	void dumpOneFrame() {
		int max = mBulkEndpoint.getMaxPacketSize();
		
		byte[] buf = new byte[max];
		
		int r = mConnection.bulkTransfer(mBulkEndpoint, buf, max, 1000);
		
		Log.d(TAG, "dumpOneFrame, bulkxfer: " + r);
		
		String s = "";
		
		for (int i = 0; i < r; i++) {
			s = s + " " + Integer.toHexString(buf[i]);
		}
		
		Log.d(TAG, "got frame: " + s);
	}
	
    private class usbThread extends Thread {
    	private volatile boolean stopped = false;
    	private volatile UsbSource usbsource;
    	
    	public usbThread(UsbSource s) {
    		super();
    		
    		usbsource = s;
    	}
    	
    	public void stopUsb() {
    		stopped = true;
    	}
    	
    	@Override
    	public void run() {
    		// int sz = mBulkEndpoint.getMaxPacketSize();
    		int sz = 2500;
			byte[] buffer = new byte[sz];
    		
			while (!stopped) {
				int l = mConnection.bulkTransfer(mBulkEndpoint, buffer, sz, 1000);
				int fcsofft = 0;
				
				if (l > 0) {
					if (is_rtl8187b == 0 && l > 16)
						l = l - 16;
					else if (l > 20)
						l = l - 20;
				
					boolean fcs = false;
					if (l > 4) {
						fcs = true;
						fcsofft = l - 4;
						l = l - 4;
					}
					
					if (mPacketHandler != null) {
						Packet p = new Packet(Arrays.copyOfRange(buffer, 0, l));
						p.setDlt(PcapLogger.DLT_IEEE80211);
					
						/*
						if (fcs)
							p.setFcs(Arrays.copyOfRange(buffer, fcsofft - 1, 4));
							*/
						
						mPacketHandler.handlePacket(usbsource, p);
					}
					
				} else if (l < 0) {
					// Log.e(TAG, "Failed to do bulkio");
				}
			}

    	}
    };
    usbThread mUsbThread = new usbThread(this);

    @Override
    public int attachUsbDevice(UsbDevice device) {	 
    	mRadioType = "RTL8187 USB";
    	mRadioInfo = "Initializing USB device";
    	mRadioMac = "";
    	mRadioActive = true;
    	sendRadioState();
    	
    	if (
    			// LevelOne WNC-0301USB v5
				(device.getVendorId() == 0x0dba && device.getProductId() == 0x8187) ||
				
				// NetGear WG111v3
				(device.getVendorId() == 0x0846 && device.getProductId() == 0x4260)
		) {
    		// It's got a chance of being an 8187b
    		is_rtl8187b = 1;
    	}

    	if (device.getInterfaceCount() != 1) {
    		mRadioInfo = "Could not find USB interface, getInterfaceCount != 1";

    		sendRadioState();
    		return -1;
		}
		
		UsbInterface intf = device.getInterface(0);
		if (intf.getEndpointCount() == 0) {
			mRadioInfo = "Could not find USB endpoints";
			sendRadioState();
			return -1;
		}
		
        UsbDeviceConnection connection = mUsbManager.openDevice(device);
        if (connection != null && connection.claimInterface(intf, true)) {
        	mConnection = connection;
        	mDevice = device;
        } else {            
            mRadioInfo = "Could not claim and open USB device";
            mRadioActive = false;
            sendRadioState();
    		
            return -1;
        }
        
		
		UsbEndpoint ep = null;
		/*
		for (int e = 0; e < intf.getEndpointCount(); e++) {
			ep = intf.getEndpoint(e);
			if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK &&
					ep.getDirection() == UsbConstants.USB_DIR_OUT) {
				break;
			} 
			ep = null;
		}*/
		ep = intf.getEndpoint(0);
		if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK ||
				ep.getDirection() != UsbConstants.USB_DIR_IN)
			ep = null;
		
		if (ep == null) {
			mRadioInfo = "Unable to find bulk IO USB endpoint";
			sendRadioState();
			return -1;
		}
		
		mBulkEndpoint = ep;
        
        if ((rtl818x_ioread32(RTL818X_ADDR_RX_CONF) & (1 << 6)) != 0)
            eeprom_width = PCI_EEPROM_WIDTH_93C66;
        else
            eeprom_width = PCI_EEPROM_WIDTH_93C46;

        rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_CONFIG);
        
       // usleep(1);
        
        int[] rawaddr = new int[3];
        rawaddr = eeprom_93cx6_multiread(RTL8187_EEPROM_MAC_ADDR, 3);

        // Unpack 16bit into mac addr
        macaddr[0] = rawaddr[0] & 0xFF;
        macaddr[1] = (rawaddr[0] >> 8) & 0xFF;
        macaddr[2] = rawaddr[1] & 0xFF;
        macaddr[3] = (rawaddr[1] >> 8) & 0xFF;
		macaddr[4] = rawaddr[2] & 0xFF;
		macaddr[5] = (rawaddr[2] >> 8) & 0xFF;
        
        int txpwr;
        int chnum = 0;
        
        for (int i = 0; i < 3; i++) {
            txpwr = eeprom_93cx6_read(RTL8187_EEPROM_TXPWR_CHAN_1 + i);
            channels[chnum++][2] = txpwr & 0xFF;
            channels[chnum++][2] = txpwr >> 8;
        }
        for (int i = 0; i < 2; i++) {
            txpwr = eeprom_93cx6_read(RTL8187_EEPROM_TXPWR_CHAN_4 + i);
            channels[chnum++][2] = txpwr & 0xFF;
            channels[chnum++][2] = txpwr >> 8;
        }
        
        /*
        for (int i = 0; i < 2; i++) {
            txpwr = eeprom_93cx6_read(RTL8187_EEPROM_TXPWR_CHAN_6 + i);
            channels[chnum++][2] = txpwr & 0xFF;
            channels[chnum++][2] = txpwr >> 8;
        }
        */

        txpwr_base = eeprom_93cx6_read(RTL8187_EEPROM_TXPWR_BASE);
        
        int reg;
        reg = rtl818x_ioread8(RTL818X_ADDR_PGSELECT) & ~1;
        rtl818x_iowrite8(RTL818X_ADDR_PGSELECT, reg | 1);
        /* 0 means asic B-cut, we should use SW 3 wire
         * bit-by-bit banging for radio. 1 means we can use
         * USB specific request to write radio registers */
        asic_rev = rtl818x_ioread8(0xFFFE) & 0x3;
        rtl818x_iowrite8(RTL818X_ADDR_PGSELECT, reg);
        rtl818x_iowrite8(RTL818X_ADDR_EEPROM_CMD, RTL818X_EEPROM_CMD_NORMAL);

        if (is_rtl8187b != 0) {
            int reg32;
            reg32 = rtl818x_ioread32(RTL818X_ADDR_TX_CONF);
            reg32 &= RTL818X_TX_CONF_HWVER_MASK;
            switch (reg32) {
            case RTL818X_TX_CONF_R8187vD_B:
                /* Some RTL8187B devices have a USB ID of 0x8187
                 * detect them here */
                chipset_name = "RTL8187BvB(early)";
                is_rtl8187b = 1;
                hw_rev = HW_RTL8187BvB;
                break;
            case RTL818X_TX_CONF_R8187vD:
                chipset_name = "RTL8187vD";
                break;
            default:
                chipset_name = "RTL8187vB (default)";
            }
           } else {
            /*
             * Force USB request to write radio registers for 8187B, Realtek
             * only uses it in their sources
             */
            /*if (priv->asic_rev == 0) {
                printk(KERN_WARNING "rtl8187: Forcing use of USB "
                       "requests to write to radio registers\n");
                priv->asic_rev = 1;
            }*/
            switch (rtl818x_ioread8(0xFFE1)) {
            case RTL818X_R8187B_B:
                chipset_name = "RTL8187BvB";
                hw_rev = HW_RTL8187BvB;
                break;
            case RTL818X_R8187B_D:
                chipset_name = "RTL8187BvD";
                hw_rev = HW_RTL8187BvD;
                break;
            case RTL818X_R8187B_E:
                chipset_name = "RTL8187BvE";
                hw_rev = HW_RTL8187BvE;
                break;
            default:
                chipset_name = "RTL8187BvB (default)";
                hw_rev = HW_RTL8187BvB;
            }
        }

        if (is_rtl8187b == 0) {
            for (int i = 0; i < 2; i++) {
                txpwr = eeprom_93cx6_read(
                          RTL8187_EEPROM_TXPWR_CHAN_6 + i);
                channels[chnum++][2] = txpwr & 0xFF;
                channels[chnum++][2] = txpwr >> 8;
            }
        } else {
           txpwr = eeprom_93cx6_read(RTL8187_EEPROM_TXPWR_CHAN_6);
           channels[chnum++][2] = txpwr & 0xFF;

            txpwr = eeprom_93cx6_read(0x0A);
            channels[chnum++][2] = txpwr & 0xFF;

           txpwr = eeprom_93cx6_read(0x1C);
           channels[chnum++][2] = txpwr & 0xFF;
           channels[chnum++][2] = txpwr >> 8;
        }
        
        rtl8225_write(0, 0x1B7);

        rtl8225_read(8);
        
        if (rtl8225_read(8) != 0x588 || rtl8225_read(8) != 0x700)
            rf_init_type = RTL8225_RF_INIT;
        else
            rf_init_type = RTL8225Z2_RF_INIT;

        rtl8225_write(0, 0x0B7);

        String rfit = "RTL8225z2";
        if (rf_init_type == RTL8225_RF_INIT)
        	rfit = "RTL8225";
        	
        mRadioMac =
        	String.format("%02x:%02x:%02x:%02x:%02x:%02x",
        			macaddr[0], macaddr[1], macaddr[2],
        			macaddr[3], macaddr[4], macaddr[5]);
                
        mRadioInfo = chipset_name + " V" + asic_rev + "+" + rfit;
		
        // int hwr = rtl8187_init_hw();
        
        // Log.d(TAG, "hw init: " + hwr);
        
        rtl8187_detect_rf();
        
        int hws = rtl8187_start();
        
        if (hws != 0) {
        	sendText("Failed to start hw: " + hws, true);
        	mRadioInfo = "Failed to calibrate and start hardware";
        } else {
        	Log.d(TAG, "Successfully calibrated & started hw");
        }
        
        mRadioActive = true;
            
		sendRadioState();	
        
        rtl8187_set_channel(6);
    	// dumpOneFrame();
        
        mUsbThread.start();
        
        return 1;
        		
	}

    // Stub constructor
    public Rtl8187Card(UsbManager manager) {
    	super(manager);
    }
    
	public Rtl8187Card(UsbManager manager, Handler usbhandler, 
			Context context, PacketHandler packethandler) {
		super(manager, usbhandler, context, packethandler);
	}

	@Override
	public UsbSource makeSource(UsbDevice device, UsbManager manager, Handler servicehandler, 
			Context context, PacketHandler packethandler) {
		UsbSource s = (UsbSource) new Rtl8187Card(manager, servicehandler, context, packethandler);
		s.attachUsbDevice(device);
		return s;
	}

};
