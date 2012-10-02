package net.kismetwireless.android.pcapcapture;

import java.util.ArrayList;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

class PcapService extends Service {
	private final String LOGTAG = "pcapcapture-service";

	private Context mContext;

	private ArrayList<Messenger> mClientList = new ArrayList<Messenger>();

	private ArrayList<UsbSource> mSourceList = new ArrayList<UsbSource>();

	private boolean mShutdown = false;

	private SharedPreferences mPreferences;

	public static final int MSG_REGISTER_CLIENT = 0;
	public static final int MSG_UNREGISTER_CLIENT = 1;
	public static final int MSG_COMMAND = 2;
	public static final int MSG_DIE = 4;
	public static final int MSG_STATE = 5;
	public static final int MSG_RECONFIGURE_PREFS = 6;

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
		
		updatePreferences();
		
		return START_STICKY;
	}
	
	private void updatePreferences() {
		return;
	}
	
	private Notification makeNotification(String notifytext, String text) {
		Notification notification= new Notification(R.drawable.ic_statusbar,
				text, System.currentTimeMillis());
	}
}