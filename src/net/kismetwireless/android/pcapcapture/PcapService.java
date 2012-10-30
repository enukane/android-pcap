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
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class PcapService extends Service {
	private final String LOGTAG = "pcapcapture-service";

	private Context mContext;

	private ArrayList<Messenger> mClientList = new ArrayList<Messenger>();
	private ArrayList<UsbSource> mSourceList = new ArrayList<UsbSource>();
	private ArrayList<UsbSource> mProbeList = new ArrayList<UsbSource>();
	private HashMap<Integer, UsbSource> mDevMapping = new HashMap<Integer, UsbSource>();

	private boolean mShutdown = false;
	private SharedPreferences mPreferences;
	PcapLogger mPcapLogger;

	public static final int MSG_REGISTER_CLIENT = 0;
	public static final int MSG_UNREGISTER_CLIENT = 1;
	public static final int MSG_COMMAND = 2;
	public static final int MSG_USBINTENT = 3;
	public static final int MSG_DIE = 4;
	public static final int MSG_RADIOSTATE = 5;
	public static final int MSG_LOGSTATE = 6;
	public static final int MSG_RECONFIGURE_PREFS = 7;
	
	public static final String ACTION_USB_PERMISSION =
		PcapService.class + ".USB_PERMISSION";
	private static final int NOTIFICATION = 0x1337;
	
	public static final String BNDL_CMD_LOGFILE_STRING = "logfile";
	public static final String BNDL_CMD_LOGFILE_STOP_BOOL = "logstop";
	public static final String BNDL_CMD_LOGFILE_START_BOOL = "logstart";
	
	public static final String BNDL_STATE_LOGGING_BOOL = "logging";
	public static final String BNDL_STATE_LOGFILE_PACKETS_INT = "logpackets";
	public static final String BNDL_STATE_LOGFILE_SIZE_LONG = "logsize";
	
	private NotificationManager mNM;

	private PendingIntent mPermissionIntent;
	private UsbManager mUsbManager;
	
	private Bundle mLastUsbState;
	
	private boolean mChannelHop = false;
	private int mChannelLock = 6;
	ArrayList<Integer> mChannelList = new ArrayList<Integer>();
	private int mChannelPos = 0;
	
	private Handler mTimeHandler = new Handler();
	
	private Runnable logStateTask = new Runnable() {
		public void run() {
			if (mShutdown) return;
			
			sendLogStateBundle();
			
			mTimeHandler.postDelayed(this, 1000);
		}
	};
	
	private Runnable chanChangeTask = new Runnable() {
		public void run() {
			if (mShutdown) return;
			
			mTimeHandler.postDelayed(this, 250);
			
			if (!mChannelHop)
				return;
			
			for (UsbSource u : mSourceList) {
				u.setChannel(mChannelList.get(mChannelPos));
			}
			
			mChannelPos++;
			
			if (mChannelPos == mChannelList.size())
				mChannelPos = 0;
		}
	};
	
	class IncomingServiceHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Messenger c;
			Bundle b;

			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				c = msg.replyTo;

				if (!mClientList.contains(c)) {
					Log.d(LOGTAG, PcapService.this + " Adding client " + c);
					PcapService.this.mClientList.add(c);
				}

				if (mLastUsbState != null)
					sendStateBundle(mLastUsbState);
				
				sendLogStateBundle();
				
				break;
			case MSG_UNREGISTER_CLIENT:
				c = msg.replyTo;

				Log.d(LOGTAG, "Removing client " + c);
				if (mClientList.contains(c))
					mClientList.remove(c);

				break;

			case MSG_DIE:
				mShutdown = true;
				stopSelf();
				break;
				
			case MSG_USBINTENT:
				handleUsbIntent(msg.getData());

			case MSG_RECONFIGURE_PREFS:
				updatePreferences();
				break;

			case MSG_RADIOSTATE:
				break;

			case MSG_COMMAND:
				b = msg.getData();
				
				if (b.containsKey(BNDL_CMD_LOGFILE_START_BOOL)) {
					if (b.containsKey(BNDL_CMD_LOGFILE_STRING)) {
						mPcapLogger.startLogging(b.getString(BNDL_CMD_LOGFILE_STRING));
					}
				} else {
					mPcapLogger.stopLogging();
				}
				
				sendLogStateBundle();
				
				break;

			default:
				super.handleMessage(msg);
			}

		}
	}
	
	final Messenger mMessenger = new Messenger(new IncomingServiceHandler());

	private Handler mDeviceHandler;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		if (mUsbManager == null)
	        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);
		
		mProbeList.add(new Rtl8187Card(mUsbManager));
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		mShutdown = true;
		
		mTimeHandler.removeCallbacks(logStateTask);
	}
	
	public void sendStateBundle(Bundle b) {
		Log.d("USBLOG", "Send state " + this + " clientlist size " + mClientList.size() + " bundle " + b);
		
		if (mLastUsbState == null || (mLastUsbState != null && !mLastUsbState.equals(b)))
			mLastUsbState = new Bundle(b);
		
		for (Messenger m : mClientList) {
			try {
				Log.d("USBLOG", "Sending to client " + m);
				Message s = Message.obtain(null, MSG_RADIOSTATE);
				s.setData(new Bundle(b));
		
				m.send(s);
			} catch (RemoteException e) {
				Log.d(LOGTAG, "Lost a client: " + e);
				mClientList.remove(m);
			}
		}
	}
	
	public void sendLogStateBundle() {
		Bundle b = new Bundle();
	
		b.putBoolean(BNDL_STATE_LOGGING_BOOL, mPcapLogger.getLogging());
		b.putInt(BNDL_STATE_LOGFILE_PACKETS_INT, mPcapLogger.getHandledPackets());
		b.putLong(BNDL_STATE_LOGFILE_SIZE_LONG, mPcapLogger.getHandledBytes());

		if (mPcapLogger.getPath() != null)
			b.putString(BNDL_CMD_LOGFILE_STRING, mPcapLogger.getPath());
		
		for (Messenger m : mClientList) {
			try {
				Message s = Message.obtain(null, MSG_LOGSTATE);
				s.setData(new Bundle(b));
				
				m.send(s);
			} catch (RemoteException e) {
				Log.d(LOGTAG, "Lost a client: " + e);
				mClientList.remove(m);
			}
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mContext = this;
		
		if (mNM == null)
			mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		if (mPermissionIntent == null)
	        mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, 
	        		new Intent(ACTION_USB_PERMISSION), 0);

		if (mPcapLogger == null)
			mPcapLogger = new PcapLogger();
		
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		// Devicehandler is a basic replicator up to the main messaging system to the UI
		if (mDeviceHandler == null) {
			mDeviceHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					Log.d("USBLOG", "Replicating service message " + (Bundle) msg.getData());
					// Toast.makeText(mContext, "Replicating service message", Toast.LENGTH_SHORT).show();
					PcapService.this.sendStateBundle(msg.getData());
				}

			};		
		}
		
		updatePreferences();
		
		// Toast.makeText(mContext, "Service starting", Toast.LENGTH_SHORT).show();
		
		// Look for already connected USB devices and initiate permissions requests for them
		for (UsbSource s : mProbeList) {
			ArrayList<UsbDevice> ud = s.scanUsbDevices();
			
			for (UsbDevice d : ud) {
				if (!mDevMapping.containsKey(d.getDeviceId())) {
					mUsbManager.requestPermission(d, mPermissionIntent);
				}
			}
		}
	
		logStateTask.run();
		chanChangeTask.run();
		
		return START_STICKY;
	}
	
	private void updatePreferences() {
		mChannelHop = mPreferences.getBoolean(MainActivity.PREF_CHANNELHOP, true);
		String chpref = mPreferences.getString(MainActivity.PREF_CHANNEL, "11");
		mChannelLock = Integer.parseInt(chpref);
		
		mChannelList.clear();
		for (int c = 1; c <= 11; c++) {
			if (mPreferences.getBoolean(MainActivity.PREF_CHANPREFIX + Integer.toString(c), true)) {
				mChannelList.add(c);
			}
		}
		
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
		// Toast.makeText(mContext, "Handling USB intent in service", Toast.LENGTH_SHORT).show();

		String action = b.getString("ACTION");
	
		if (ACTION_USB_PERMISSION.equals(action)) {
			synchronized (this) {
				UsbDevice device = (UsbDevice) b.getParcelable("DEVICE");
				
				if (b.getBoolean("EXTRA")) {
					if (device != null) {
						// Find who can handle this, make a new source for it, start doing things
						for (UsbSource s : mProbeList) {
							if (s.scanUsbDevice(device)) {
								if (mDevMapping.containsKey(device.getDeviceId()))
									break;
								
								UsbSource newSource = s.makeSource(device, mUsbManager, mDeviceHandler, mContext, mPcapLogger);
								
								mDevMapping.put(device.getDeviceId(), newSource);
								
								// Log.d(LOGTAG, "Source " + s.getClass() + " would handle this");
								// Toast.makeText(mContext, "Source " + s.getClass() + " would claim device", Toast.LENGTH_SHORT).show();

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
					if (s.scanUsbDevice(device)) {
						mUsbManager.requestPermission(device, mPermissionIntent);
						// Log.d(LOGTAG, "Source " + s.getClass() + " likes USB device");
						// Toast.makeText(mContext, "Source " + s.getClass() + " likes USB device", Toast.LENGTH_SHORT).show();
						break;
					}
				}
			}
		}
	}
}