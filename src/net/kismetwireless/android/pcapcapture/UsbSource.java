package net.kismetwireless.android.pcapcapture;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

abstract public class UsbSource {
	public static final String BNDL_RADIOPRESENT_BOOL = "radiopresent";
	public static final String BNDL_RADIOMAC_STRING = "radiomac";
	public static final String BNDL_RADIOINFO_STRING = "radioinfo";
	public static final String BNDL_RADIOTYPE_STRING = "radiotype";
	public static final String BNDL_RADIOPHY_INT = "radiophy";
	
	public static final String BNDL_RADIOERROR_STRING = "errorstring";
	public static final String BNDL_RADIOSTATUS_STRING = "statusstring";
	public static final String BNDL_RADIOPACKET_BYTES = "packet";
	
	public static final String BNDL_RADIOCHANNEL_INT = "channelset";
	
	protected boolean mRadioActive;
	protected String mRadioType;
	protected String mRadioMac;
	protected String mRadioInfo;
	
	// DLT
	protected int mDlt;
	
	protected int mLastChannel = 0;
	
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
	public UsbSource(UsbManager manager) {
		// Does nothing but give us a way to add to a list for searching for cards
		mUsbManager = manager;
	}
	
	public UsbSource(UsbManager manager, Handler servicehandler, Context context, 
			PacketHandler packethandler) {
		
		mUsbManager = manager;
		mServiceHandler = servicehandler;
		mContext = context;
		
		mPacketHandler = packethandler;
		mPacketHandler.addUsbSource(this);
	}
	
	abstract public UsbSource makeSource(UsbDevice device, UsbManager manager, Handler servicehandler, 
			Context context, PacketHandler packethandler);

	// Grab a specific device we have permission for
	abstract public int attachUsbDevice(UsbDevice device);
	
	// Return devices we want to look at
	abstract public ArrayList<UsbDevice> scanUsbDevices();
	
	// Examine a device & ask for permission if we want to use it and don't have it already
	abstract public boolean scanUsbDevice(UsbDevice device);

	public void doShutdown() {
		mRadioActive = false;
		sendRadioState();
	}
	
	public void setChannel(int ch) {
		mLastChannel = ch;
		sendRadioState();
	}
		
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
	
	public int getLastChannel() {
		return mLastChannel;
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
	
	protected void sendRadioState() {
		// Log.d("USBLOG", "Sending radio state");
		if (mServiceHandler == null) {
			// Log.d("USBLOG", "Can't send state, servicehandler null");
			return;
		}
		
		Message msg = new Message();
		Bundle bundle = new Bundle();

		bundle.putBoolean(BNDL_RADIOPRESENT_BOOL, getRadioActive());
		bundle.putString(BNDL_RADIOTYPE_STRING, getRadioType());
		bundle.putString(BNDL_RADIOMAC_STRING, getRadioMac());
		bundle.putString(BNDL_RADIOINFO_STRING, getRadioInfo());
		bundle.putInt(BNDL_RADIOCHANNEL_INT, getLastChannel());
		
		msg.setData(bundle);

		mServiceHandler.sendMessage(msg);
		
	}
	
}