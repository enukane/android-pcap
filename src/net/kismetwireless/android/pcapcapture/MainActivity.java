package net.kismetwireless.android.pcapcapture;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends Activity { 
	String LOGTAG = "PcapCapture";

	PendingIntent mPermissionIntent;
	UsbManager mUsbManager;
	Context mContext;

	SharedPreferences mPreferences;

	Messenger mService = null;
	boolean mIsBound = false;

	public class deferredUsbIntent {
		UsbDevice device;
		String action;
		boolean extrapermission;
	};

	ArrayList<deferredUsbIntent> mDeferredIntents = new ArrayList<deferredUsbIntent>();

	private TextView mTextDashUsb, mTextDashUsbSmall, mTextDashFile, 
	mTextDashFileSmall, mTextDashLogControl, mTextDashChanhop,
	mTextDashChanhopSmall, mTextManageSmall, mTextDashHardware;
	private TableRow mRowLogControl, mRowLogShare, mRowManage, mRowHardware, mRowHop;
	private ImageView mImageControl, mImageShare, mImageHardware;

	private String mLogDir;
	private File mLogPath = new File("");
	private File mOldLogPath;
	private boolean mShareOnStop = false;
	private boolean mLocalLogging = false, mLogging = false, mUsbPresent = false;
	private int mLogCount = 0;
	private long mLogSize = 0;
	private String mUsbType = "", mUsbInfo = "";
	private int mLastChannel = 0;

	public static int PREFS_REQ = 0x1001;

	public static final String PREF_CHANNELHOP = "channel_hop";
	public static final String PREF_CHANNEL = "channel_lock";
	public static final String PREF_CHANPREFIX = "ch_";

	public static final String PREF_LOGDIR = "logdir";

	private boolean mChannelHop;
	private int mChannelLock;
	ArrayList<Integer> mChannelList = new ArrayList<Integer>();

	class IncomingServiceHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Bundle b;
			boolean updateUi = false;

			switch (msg.what) {
			case PcapService.MSG_RADIOSTATE:
				b = msg.getData();

				Log.d(LOGTAG, "Got radio state: " + b);

				if (b == null)
					break;

				if (b.getBoolean(UsbSource.BNDL_RADIOPRESENT_BOOL, false)) {
					mUsbPresent = true;

					mUsbType = b.getString(UsbSource.BNDL_RADIOTYPE_STRING, "Unknown");
					mUsbInfo = b.getString(UsbSource.BNDL_RADIOINFO_STRING, "No info available");
					mLastChannel = b.getInt(UsbSource.BNDL_RADIOCHANNEL_INT, 0);
				} else {
					// Turn off logging
					if (mUsbPresent) 
						doUpdateServiceLogs(mLogPath.toString(), false);

					mUsbPresent = false;
					mUsbType = "";
					mUsbInfo = "";
					mLastChannel = 0;
				}

				updateUi = true;

				break;
			case PcapService.MSG_LOGSTATE:
				b = msg.getData();

				if (b == null)
					break;

				if (b.getBoolean(PcapService.BNDL_STATE_LOGGING_BOOL, false)) {
					mLocalLogging = true;
					mLogging = true;

					mLogPath = new File(b.getString(PcapService.BNDL_CMD_LOGFILE_STRING));
					mLogCount = b.getInt(PcapService.BNDL_STATE_LOGFILE_PACKETS_INT, 0);
					mLogSize = b.getLong(PcapService.BNDL_STATE_LOGFILE_SIZE_LONG, 0);
				} else {
					mLocalLogging = false;
					mLogging = false;

					if (mShareOnStop) {
						Intent i = new Intent(Intent.ACTION_SEND); 
						i.setType("application/cap"); 
						i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + mOldLogPath)); 
						startActivity(Intent.createChooser(i, "Share Pcap file"));
						mShareOnStop = false;
					}
				}

				updateUi = true;

				break;
			default:
				super.handleMessage(msg);
			}

			if (updateUi)
				doUpdateUi();
		}
	}

	final Messenger mMessenger = new Messenger(new IncomingServiceHandler());

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(LOGTAG, "mconnection connected");

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
		Log.d(LOGTAG, "binding service");

		if (mIsBound) {
			Log.d(LOGTAG, "already bound");
			return;
		}

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

	private void doUpdatePrefs() {
		mChannelHop = mPreferences.getBoolean(PREF_CHANNELHOP, true);
		String chpref = mPreferences.getString(PREF_CHANNEL, "11");
		mChannelLock = Integer.parseInt(chpref);

		if (!mPreferences.contains(PREF_LOGDIR)) {
			SharedPreferences.Editor e = mPreferences.edit();
			e.putString(PREF_LOGDIR, "/mnt/sdcard/pcap");
			e.commit();
		}

		mLogDir = mPreferences.getString(PREF_LOGDIR, "/mnt/sdcard/pcap");

		mChannelList.clear();
		for (int c = 1; c <= 11; c++) {
			if (mPreferences.getBoolean(PREF_CHANPREFIX + Integer.toString(c), true)) {
				mChannelList.add(c);
			}
		}
	}

	private void doUpdateServiceprefs() {
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

	private void doUpdateServiceLogs(String path, boolean enable) {
		if (mService == null)
			return;

		Bundle b = new Bundle();

		if (enable) {
			b.putString(PcapService.BNDL_CMD_LOGFILE_STRING, path);
			b.putBoolean(PcapService.BNDL_CMD_LOGFILE_START_BOOL, true);
		} else {
			b.putBoolean(PcapService.BNDL_CMD_LOGFILE_STOP_BOOL, true);
		}

		Message msg = Message.obtain(null, PcapService.MSG_COMMAND);
		msg.replyTo = mMessenger;
		msg.setData(b);

		try {
			mService.send(msg);
		} catch (RemoteException e) {
			Log.e(LOGTAG, "Failed to send command message: " + e);
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

		mService = null;
		mIsBound = false;
	}

	void doSendDeferredIntent(deferredUsbIntent i) {
		Message msg;

		Bundle b = new Bundle();

		// Toast.makeText(mContext, "Sending deferred intent", Toast.LENGTH_SHORT).show();

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

	private void doUpdateUi() {
		if (!mUsbPresent) {
			mTextDashUsb.setText("No USB device present");
			mTextDashUsbSmall.setVisibility(View.GONE);
			mTextDashUsbSmall.setText("");

			mTextDashLogControl.setText("No USB NIC plugged in");
			mImageControl.setImageResource(R.drawable.alert_warning);
			mRowLogControl.setClickable(false);
		} else {
			mTextDashUsb.setText(mUsbType);
			mTextDashUsbSmall.setText(mUsbInfo);
			mTextDashUsbSmall.setVisibility(View.VISIBLE);
			mRowLogControl.setClickable(true);
		}

		if (!mLogging) {
			mTextDashFile.setText("Logging inactive");
			mTextDashFileSmall.setText("");
			mTextDashFileSmall.setVisibility(View.GONE);
			mRowLogShare.setClickable(false);
			mImageShare.setVisibility(View.INVISIBLE);
		} else {
			mTextDashFile.setText(mLogPath.getName());
			mTextDashFileSmall.setVisibility(View.VISIBLE);
			mRowLogShare.setClickable(true);
			mImageShare.setVisibility(View.VISIBLE);
		}

		if (mLogCount > 0 || mLogging) {
			String sz = "0B";

			if (mLogSize < 1024) {
				sz = String.format("%dB", mLogSize);
			} else if (mLogSize < (1024 * 1024)) {
				sz = String.format("%2.2fK", ((float) mLogSize) / 1024);
			} else if (mLogSize < (1024 * 1024 * 1024)) {
				sz = String.format("%5.2fM", ((float) mLogSize) / (1024 * 1024));
			}

			mTextDashFileSmall.setText(sz + ", " + mLogCount + " packets");
		} else {
			mTextDashFileSmall.setText("");
		}

		if (!mLocalLogging && mUsbPresent) {
			mTextDashLogControl.setText("Start logging");
			mImageControl.setImageResource(R.drawable.ic_action_record);
		} else if (mLocalLogging) {
			mTextDashLogControl.setText("Stop logging");
			mImageControl.setImageResource(R.drawable.ic_action_stop);
		}

		if (mChannelHop) {
			mTextDashChanhop.setText("Channel hopping enabled");

			String s = "";
			for (Integer i : mChannelList)  {
				s += i;

				if (mChannelList.indexOf(i) != mChannelList.size() - 1)
					s += ", ";

				if (mLastChannel != 0)
					s += " (" + mLastChannel + ")";
			}

			mTextDashChanhopSmall.setText(s);
		} else {
			mTextDashChanhop.setText("Channel hopping disabled");
			mTextDashChanhopSmall.setText("Locked to channel " + mChannelLock);
		}

		doUpdateFilesizes();
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Don't launch a second copy from the USB intent
		if (!isTaskRoot()) {
			final Intent intent = getIntent();
			final String intentAction = intent.getAction(); 
			if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
				Log.w(LOGTAG, "Main Activity is not the root.  Finishing Main Activity instead of launching.");
				finish();
				return;       
			}
		}

		mContext = this;

		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

		setContentView(R.layout.activity_main);

		mTextDashUsb = (TextView) findViewById(R.id.textDashUsbDevice);
		mTextDashUsbSmall = (TextView) findViewById(R.id.textDashUsbSmall);
		mTextDashFile = (TextView) findViewById(R.id.textDashFile);
		mTextDashFileSmall = (TextView) findViewById(R.id.textDashFileSmall);
		mTextDashLogControl = (TextView) findViewById(R.id.textDashCaptureControl);
		mTextDashChanhop = (TextView) findViewById(R.id.textChannelHop);
		mTextDashChanhopSmall = (TextView) findViewById(R.id.textChannelHopSmall);
		mTextManageSmall = (TextView) findViewById(R.id.textManageSmall);
		mTextDashHardware = (TextView) findViewById(R.id.textDashHardware); 

		mRowLogControl = (TableRow) findViewById(R.id.tableRowLogControl);
		mRowLogShare = (TableRow) findViewById(R.id.tableRowFile);
		mRowManage = (TableRow) findViewById(R.id.tableRowManage);
		mRowHardware = (TableRow) findViewById(R.id.tableRowHardware);
		mRowHop = (TableRow) findViewById(R.id.tableRowHop);

		mImageControl = (ImageView) findViewById(R.id.imageDashLogControl);
		mImageShare = (ImageView) findViewById(R.id.imageShare);
		mImageHardware = (ImageView) findViewById(R.id.imageViewHardware);
		
        if (Build.MANUFACTURER.equals("motorola")) {
        	mTextDashHardware.setText("Motorola hardware has limitations on USB (special power " +
        			"injectors are needed), check the Help window for a link to more info.");
        	mImageHardware.setImageResource(R.drawable.alert_warning);
        	mRowHardware.setVisibility(View.VISIBLE);
        }

		Intent svc = new Intent(this, PcapService.class);
		startService(svc);
		doBindService();

		mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(PcapService.ACTION_USB_PERMISSION), 0);

		IntentFilter filter = new IntentFilter(PcapService.ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		mContext.registerReceiver(mUsbReceiver, filter);

		doUpdatePrefs();

		// make the directory on the sdcard
		File f = new File(mLogDir);

		if (!f.exists()) {
			f.mkdir();
		}

		/*
		FilelistFragment list = (FilelistFragment) getFragmentManager().findFragmentById(R.id.fragment_filelist);
		list.registerFiletype("cap", new PcapFileTyper());
		list.setDirectory(new File(mLogDir));
		list.setRefreshTimer(2000);
		list.setFavorites(true);
		list.Populate();
		 */
		// getFragmentManager().beginTransaction().add(R.id.fragment_filelist, list).commit();

		mRowLogControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mLocalLogging) {
					mLocalLogging = false;
					doUpdateServiceLogs(null, false);
				} else {
					mLocalLogging = true;

					Date now = new Date();
					String snow = now.toString();
					snow = snow.replace(" ", "-");
					snow = snow.replace(":", "-");
					mLogPath = new File(mLogDir + "/android-" + snow + ".cap");

					doUpdateServiceLogs(mLogPath.toString(), true);
				}

				doUpdateUi();
			}
		});

		mRowManage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(mContext, FilemanagerActivity.class);
				startActivity(i);
			}
		});

		mRowLogShare.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doShareCurrent();
			}
		});
		
		mRowHop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(MainActivity.this, ChannelPrefs.class), PREFS_REQ);
			}
		});

		doUpdateUi();
	}

	@Override
	public void onNewIntent(Intent intent) {
		// Replicate USB intents that come in on the single-top task
		mUsbReceiver.onReceive(this, intent);
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mContext.unregisterReceiver(mUsbReceiver);
		doUnbindService();
	}

	@Override 
	public void onPause() {
		super.onPause();

		// Log.d(LOGTAG, "Onpause");
		// doUnbindService();
	}

	@Override
	public void onResume() {
		super.onResume();

		// Log.d(LOGTAG, "Onresume");
		// doBindService();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivityForResult(new Intent(this, ChannelPrefs.class), PREFS_REQ);
			break;

		case R.id.menu_help:
			doShowHelp();
			break;
		}

		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOGTAG, "Got activity req " + Integer.toString(requestCode)
                + " result code " + Integer.toString(resultCode));

		if (requestCode == PREFS_REQ) {
			doUpdatePrefs();
			doUpdateUi();
			doUpdateServiceprefs();
		}

	}

	protected void doUpdateFilesizes() {
		long ds = FileUtils.countFileSizes(new File(mLogDir), new String[] { "cap" }, 
				false, false, null);
		long nf = FileUtils.countFiles(new File(mLogDir), new String[] { "cap" }, 
				false, false, null);

		String textuse = FileUtils.humanSize(ds);

		mTextManageSmall.setText("Using " + textuse + " in " + nf + " logs");
	}

	protected void doShowHelp() {
		AlertDialog.Builder alert = new AlertDialog.Builder(mContext);

		WebView wv = new WebView(this);
		
		wv.loadUrl("file:///android_asset/html_no_copy/PcapCaptureHelp.html");

		wv.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Uri uri = Uri.parse(url);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
				
				return true;
			}
		});

		alert.setView(wv);
		
		alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
			}
		});
		
		alert.show();
	}
	
	protected void doShareCurrent() {
		AlertDialog.Builder alertbox = new AlertDialog.Builder(mContext);

		alertbox.setTitle("Share current pcap?");

		alertbox.setMessage("Sharing the active log can result in truncated log files.  This " +
				"should not cause a problem with most log processors, but for maximum safety, " +
		"you should stop logging first.");

		alertbox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
			}
		});

		alertbox.setPositiveButton("Share", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				Intent i = new Intent(Intent.ACTION_SEND); 
				i.setType("application/cap"); 
				i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + mLogPath)); 
				startActivity(Intent.createChooser(i, "Share Pcap file"));
			}
		});

		alertbox.setNeutralButton("Stop and Share", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				mShareOnStop = true;
				mOldLogPath = mLogPath;
				doUpdateServiceLogs(mLogPath.toString(), false);
			}
		});

		alertbox.show();

		/*
		 */
	}

}
