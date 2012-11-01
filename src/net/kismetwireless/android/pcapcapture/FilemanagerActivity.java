package net.kismetwireless.android.pcapcapture;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class FilemanagerActivity extends Activity {
	Context mContext;
	SharedPreferences mPreferences;
	String mLogDir;
	FilelistFragment mList;
	FileusageFragment mUsage;
	
	Button mDeleteButton;

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
		
		mList = (FilelistFragment) getFragmentManager().findFragmentById(R.id.fragment_filelist);
		mList.registerFiletype("cap", new PcapFileTyper());
		mList.setDirectory(new File(mLogDir));
		mList.setRefreshTimer(2000); 
		mList.setFavorites(true);
		mList.Populate();
		
		mUsage = 
			(FileusageFragment) getFragmentManager().findFragmentById(R.id.fragment_fileusage);
		mUsage.setDirectory(new File(mLogDir));
		mUsage.setRefreshTimer(1000);
		mUsage.Populate();
	
		mDeleteButton = (Button) findViewById(R.id.buttonDelete);
		
		mDeleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deleteFileDialog();
			}
		});
	}
	
	public void deleteFileDialog() {
		AlertDialog.Builder alertbox = new AlertDialog.Builder(mContext);

		int nf = FileUtils.countFiles(new File(mLogDir), new String[] { "cap" }, 
				false, true, mPreferences);

		alertbox.setTitle("Delete " + nf + " Files?");
		
		alertbox.setMessage("Delete un-starred files?  " + nf + " files will be deleted.  " +
				"This can not be undone!");

		alertbox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				
			}
		});

		alertbox.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				FileUtils.deleteFiles(new File(mLogDir), new String[] { "cap" }, 
						false, true, mPreferences);
				mList.Populate();
				mUsage.Populate();
			}
		});

		alertbox.show();
	}
}
