package net.kismetwireless.android.pcapcapture;

/* File list fragment w/ optional favorite support
 * 
 * Caller provides multiple implementations of FileTyper functor class via
 * RegisterFileType; FileTyper creates icons and descriptive text of file
 * contents.
 * 
 * Optional favorite support ties into sharedpreferences to store favorited
 * file status
 * 
 * Built-in support for sharing and deleting files from the list
 * 
 */

import java.io.File;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FileusageFragment extends Fragment {
	private static String LOGTAG = "filelist-fragment";
	private File mDirectory;
	private int mTimeout;
	private Handler mTimeHandler = new Handler();
	private SharedPreferences mPreferences;
	
	private TextView mTextUse, mTextMax, mTextPath;
	private View mLayoutMenu;

	private Runnable updateTask = new Runnable() {
		@Override
		public void run() {
			Populate();
		}
	};

	public FileusageFragment() {
		super();
	}

	public FileusageFragment(File directory, int timer) {
		super();

		mDirectory = directory;
		mTimeout = timer;
	}

	public void setDirectory(File directory) {
		mDirectory = directory;
	}

	public void setRefreshTimer(int timer) {
		mTimeout = timer;
	}

	public void Populate() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		View view = inflater.inflate(R.layout.fragment_fileusage, container, false);
		
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

	}

	@Override
	public void onPause() {
		super.onPause();

		mTimeHandler.removeCallbacks(updateTask);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mTimeout > 0) {
			Populate();
		}
	}

}