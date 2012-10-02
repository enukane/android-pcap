package net.kismetwireless.android.pcapcapture;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

abstract class UsbSource {
	public static final String BNDL_RADIOPRESENT_BOOL = "radiopresent";
	public static final String BNDL_RADIOMAC_STRING = "radiomac";
	public static final String BNDL_RADIOINFO_STRING = "radioinfo";
	public static final String BNDL_RADIOTYPE_STRING = "radiotype";
	public static final String BNDL_RADIOPHY_INT = "radiophy";
	
	public static final String BNDL_RADIOERROR_STRING = "errorstring";
	public static final String BNDL_RADIOSTATUS_STRING = "statusstring";
	public static final String BNDL_RADIOPACKET_BYTES = "packet";
	
	public static final String BNDL_CHANNEL_INT = "channelset";
	
	protected String mPermission;

	protected boolean mRadioActive;
	protected String mRadioType;
	protected String mRadioMac;
	protected String mRadioInfo;
	
	// DLT
	protected int mDlt;
	
	// USB manager
	protected UsbManager mUsbManager;
	// Link to service handler
	protected Handler mServiceHandler;
	protected Context mContext;
	
	protected UsbDevice mDevice;
	protected UsbDeviceConnection mConnection;
	protected UsbManager mManager;
	protected UsbEndpoint mBulkEndpoint;
	
	protected PendingIntent mPermissionIntent;
	
	// Thing that does things with packets
	protected PacketHandler mPacketHandler;

	// Weak constructor
	public UsbSource() {
		// Does nothing but give us a way to add to a list for searching for cards
	}
	
	public UsbSource(UsbManager manager, Handler servicehandler, Context context, 
			PacketHandler packethandler, String permission) {
		mUsbManager = manager;
		mServiceHandler = servicehandler;
		mContext = context;
		
		mPermission = permission;
		
		mPacketHandler = packethandler;
		mPacketHandler.addUsbSource(this);
		
        mPermissionIntent = 
        	PendingIntent.getBroadcast(mContext, 0, new Intent(mPermission), 0);
	}
	
	abstract public UsbSource makeSource(UsbManager manager, Handler servicehandler, Context context,
			PacketHandler packethandler, String permission);

	// Grab a specific device we have permission for
	abstract public int attachUsbDevice(UsbDevice device);
	
	// Return devices we want to look at
	abstract public boolean scanUsbDevices();
	
	// Examine a device & ask for permission if we want to use it and don't have it already
	abstract public boolean scanUsbDevice(UsbDevice device, boolean permission);

	abstract public void doShutdown();
	
	abstract public void setChannel(int ch);
		
	public int getDlt() {
		return mDlt;
	}
	
	public String getRadioType() {
		return mRadioType;
	}
	
	public String getRadioMac() {
		return mRadioMac;
	}
	
	public String getRadioInfo() {
		return mRadioInfo;
	}
	
	public boolean getRadioActive() {
		return mRadioActive;
	}
	
	protected void sendText(String text, boolean error) {
		Message msg = new Message();
		Bundle bundle = new Bundle();
		
		if (error)
			bundle.putString(BNDL_RADIOERROR_STRING, text);
		else
			bundle.putString(BNDL_RADIOSTATUS_STRING, text);

		msg.setData(bundle);
		mServiceHandler.sendMessage(msg);
	}
	
	protected void sendRadioStatus() {
		Message msg = new Message();
		Bundle bundle = new Bundle();
		
		bundle.putString(BNDL_RADIOTYPE_STRING, getRadioType());
		bundle.putString(BNDL_RADIOMAC_STRING, getRadioMac());
		bundle.putString(BNDL_RADIOINFO_STRING, getRadioInfo());
	}
	
}