package net.kismetwireless.android.pcapcapture;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class FilemanagerActivity extends Activity {
	Context mContext;
	SharedPreferences mPreferences;
	String mLogDir;

	@Override
	public void onResume() {
		super.onResume();
	
		// Ghetto update incase we add the ability to change
		mLogDir = mPreferences.getString(MainActivity.PREF_LOGDIR, "/");
	}
	
	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
	
		mContext = this;
		
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		mLogDir = mPreferences.getString(MainActivity.PREF_LOGDIR, "/");
		
		setContentView(R.layout.activity_filemanager);
		
		FilelistFragment list = (FilelistFragment) getFragmentManager().findFragmentById(R.id.fragment_filelist);
		list.registerFiletype("cap", new PcapFileTyper());
		list.setDirectory(new File("/mnt/sdcard/pcap"));
		list.setRefreshTimer(2000); 
		list.setFavorites(true);
		list.Populate();
		
		FileusageFragment usage = 
			(FileusageFragment) getFragmentManager().findFragmentById(R.id.fragment_fileusage);
		usage.setDirectory(new File("/mnt/sdcard/pcap"));
		usage.setRefreshTimer(1000);
		usage.Populate();
	}
}
