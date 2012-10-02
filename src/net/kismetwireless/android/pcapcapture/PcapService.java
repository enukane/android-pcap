package net.kismetwireless.android.pcapcapture;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

class PcapService extends Service {
	private final String LOGTAG = "pcapcapture-service";

	private Context mContext;

	private ArrayList<Messenger> mClientList = new ArrayList<Messenger>();

	private ArrayList<UsbSource> mSourceList = new ArrayList<UsbSource>();
	
	private ArrayList<UsbSource> mProbeList = new ArrayList<UsbSource>();
	
	private HashMap<Integer, UsbSource> mDevMapping = new HashMap<Integer, UsbSource>();

	private boolean mShutdown = false;

	private SharedPreferences mPreferences;

	public static final int MSG_REGISTER_CLIENT = 0;
	public static final int MSG_UNREGISTER_CLIENT = 1;
	public static final int MSG_COMMAND = 2;
	public static final int MSG_USBINTENT = 3;
	public static final int MSG_DIE = 4;
	public static final int MSG_STATE = 5;
	public static final int MSG_RECONFIGURE_PREFS = 6;
	
	public static final String ACTION_USB_PERMISSION =
		PcapService.class + ".USB_PERMISSION";
	
	private static final int NOTIFICATION = 0x1337;
	
	private NotificationManager mNM;

	class IncomingServiceHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Messenger c;

			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				c = msg.replyTo;

				if (!mClientList.contains(c))
					mClientList.add(c);

				// TODO send sync states
				break;
			case MSG_UNREGISTER_CLIENT:
				c = msg.replyTo;

				if (mClientList.contains(c))
					mClientList.remove(c);

				break;

			case MSG_DIE:
				stopSelf();
				break;
				
			case MSG_USBINTENT:
				handleUsbIntent(msg.getData());

			case MSG_RECONFIGURE_PREFS:
				updatePreferences();
				break;

			case MSG_STATE:
				break;

			case MSG_COMMAND:
				break;

			default:
				super.handleMessage(msg);
			}

		}
	}
	
	final Messenger mMessenger = new Messenger(new IncomingServiceHandler());
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mProbeList.add(new Rtl8187Card());
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mContext = this;
		
		if (mNM == null)
			mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		updatePreferences();
		
		return START_STICKY;
	}
	
	private void updatePreferences() {
		return;
	}
	
	private Notification makeNotification(String notifytext, String text) {
		Notification notification= new Notification(R.drawable.ic_statusbar,
				text, System.currentTimeMillis());
		
		Intent i = new Intent();
		i.setAction(Intent.ACTION_MAIN);
		i.addCategory(Intent.CATEGORY_LAUNCHER);
		ComponentName cn = new ComponentName(this, MainActivity.class);
		i.setComponent(cn);
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);
		
		notification.setLatestEventInfo(this, notifytext, text, contentIntent);
		
		return notification;
	}
	
	private void showNotification(String notifytext, String text) {
		mNM.notify(NOTIFICATION, makeNotification(notifytext, text));
	}
	
	private void handleUsbIntent(Bundle b) {
		String action = b.getString("ACTION");
	
		if (ACTION_USB_PERMISSION.equals(action)) {
			synchronized (this) {
				UsbDevice device = (UsbDevice) b.getParcelable("DEVICE");
				
				if (b.getBoolean("EXTRA")) {
					if (device != null) {
						// Find who can handle this, make a new source for it, start doing things
						for (UsbSource s : mProbeList) {
							if (s.scanUsbDevice(device, true)) {
								// UsbSource newSource = s.makeSource(mUsbManager, servicehandler, mContext, packethandler, permission)
								Log.d(LOGTAG, "Source " + s.getClass() + " would handle this");
								
								break;
							}
						}
					}
				} else {
					showNotification("USB permission denied", "");
				}
			}
		} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
			UsbDevice device = (UsbDevice) b.getParcelable("DEVICE");
			
			// Find out who owns it and shut it down
			if (device != null) {
				if (mDevMapping.containsKey(device.getDeviceId())) {
					UsbSource ds = mDevMapping.get(device.getDeviceId());
					
					ds.doShutdown();
					mDevMapping.remove(device.getDeviceId());
					mSourceList.remove(ds);
				}
			}
		} else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			UsbDevice device = (UsbDevice) b.getParcelable("DEVICE");
			
			if (device != null) {
				for (UsbSource s : mProbeList) {
					// Scanning will ask for permission so just stop
					if (s.scanUsbDevice(device, false)) {
						Log.d(LOGTAG, "Source " + s.getClass() + " likes USB device");
						break;
					}
				}
			}
		}
	}
}