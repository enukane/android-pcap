package net.kismetwireless.android.pcapcapture;

import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	String LOGTAG = "PcapCapture";

	PendingIntent mPermissionIntent;
	UsbManager mUsbManager;
	Context mContext;
	
	Messenger mService = null;
	boolean mIsBound = false;
	
	public class deferredUsbIntent {
		UsbDevice device;
		String action;
		boolean extrapermission;
	};
	
	ArrayList<deferredUsbIntent> mDeferredIntents = new ArrayList<deferredUsbIntent>();
	
	class IncomingServiceHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Bundle b;
			
			switch (msg.what) {
			case PcapService.MSG_STATE:
				b = msg.getData();
				
				if (b == null)
					break;
				
				if (b.getBoolean(UsbSource.BNDL_RADIOPRESENT_BOOL, false)) {
					TextView tv = (TextView) findViewById(R.id.textDashUsbDevice);
					TextView tvsmall = (TextView) findViewById(R.id.textDashUsbSmall);
					
					tv.setText(b.getString(UsbSource.BNDL_RADIOTYPE_STRING, "Unknown"));
															
					tvsmall.setText(b.getString(UsbSource.BNDL_RADIOINFO_STRING, 
							"No info available"));
				}
				
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	final Messenger mMessenger = new Messenger(new IncomingServiceHandler());
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			
			try {
				Message msg = Message.obtain(null, PcapService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
				
				for (deferredUsbIntent di : mDeferredIntents) 
					doSendDeferredIntent(di);
				
			} catch (RemoteException e) {
				// Service has crashed before we can do anything, we'll soon be
				// disconnected and reconnected, do nothing
			}
		}
		
		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			mIsBound = false;
		}
	};
	
	void doBindService() {
		if (mIsBound)
			return;
		
		bindService(new Intent(MainActivity.this, PcapService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}
	
	void doKillService() {
		if (mService == null)
			return;
		
		if (!mIsBound)
			return;
		
		Message msg = Message.obtain(null, PcapService.MSG_DIE);
		msg.replyTo = mMessenger;
		
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			Log.d(LOGTAG, "Failed to send die message: " + e);
		}
	}
	
	public void doUpdateServiceprefs() {
		if (mService == null)
			return;
		
		Message msg = Message.obtain(null, PcapService.MSG_RECONFIGURE_PREFS);
		msg.replyTo = mMessenger;
		
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			Log.e(LOGTAG, "Failed to send prefs message: " + e);
		}
	}
	
	void doUnbindService() {
		if (mIsBound) {
			if (mService != null) {
				try {
					Message msg = Message.obtain(null, PcapService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// Do nothing
				}
			}
		}
	}
	
	void doSendDeferredIntent(deferredUsbIntent i) {
		Message msg;
		
		Bundle b = new Bundle();
		
		Toast.makeText(mContext, "Sending deferred intent", Toast.LENGTH_SHORT).show();
		
		msg = Message.obtain(null, PcapService.MSG_USBINTENT);
		
		b.putParcelable("DEVICE", i.device);
		b.putString("ACTION", i.action);
		b.putBoolean("EXTRA", i.extrapermission);
		
		msg.setData(b);
		
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			// nothing
		}
	}
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if (PcapService.ACTION_USB_PERMISSION.equals(action) ||
					UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action) ||
					UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				synchronized (this) {
					doBindService();
					
					deferredUsbIntent di = new deferredUsbIntent();
					di.device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					di.action = action;
					di.extrapermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
					
					if (mService == null)
						mDeferredIntents.add(di);
					else
						doSendDeferredIntent(di);
				}
			}
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mContext = this;
        
        Intent svc = new Intent(this, PcapService.class);
        startService(svc);
        doBindService();
        
        mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(PcapService.ACTION_USB_PERMISSION), 0);
        
        IntentFilter filter = new IntentFilter(PcapService.ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mContext.registerReceiver(mUsbReceiver, filter);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	mContext.unregisterReceiver(mUsbReceiver);

    	doUnbindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
