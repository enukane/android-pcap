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
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

public class FileusageFragment extends Fragment {
	private static String LOGTAG = "fileusage-fragment";

	// private static String PREF_MAXFILE_INT = "maxfilestorage";
	
	private File mDirectory;
	private int mTimeout;
	private Handler mTimeHandler = new Handler();
	// private SharedPreferences mPreferences;

	private TextView mTextUse, mTextPath;
	// private View mLayoutMenu;

	// private ImageView mImagePopup;

	// private PopupMenu mSizePopup;
	private long mDirectorySize;

	private View mView;

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
		
		if (mTextPath != null)
			mTextPath.setText(mDirectory.toString());
	}

	public void setDirectory(File directory) {
		mDirectory = directory;
		if (mTextPath != null)
			mTextPath.setText(mDirectory.toString());
	}

	public void setRefreshTimer(int timer) {
		mTimeout = timer;
	}

	public void Populate() {
		if (mDirectory == null)
			return;

		mDirectorySize = FileUtils.countFileSizes(mDirectory, new String[] { "cap" }, 
				false, false, null);

		updateSizeView();
		
		if (mTimeout > 0) 
			mTimeHandler.postDelayed(updateTask, mTimeout);
	}

	/*
	private void updateMaxView() {
		int max = mPreferences.getInt(PREF_MAXFILE_INT, 4);
		
		switch (max) {
		case 0:
			mTextMax.setText("50MB");
			break;
		case 1:
			mTextMax.setText("100MB");
			break;
		case 2:
			mTextMax.setText("250MB");
			break;
		case 3:
			mTextMax.setText("1GB");
			break;
		case 4:
			mTextMax.setText("No Limit");
			break;
		}
	}
	*/
	
	private void updateSizeView() {
		if (mTextUse == null)
			return;
		
		mTextUse.setText(FileUtils.humanSize(mDirectorySize));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		// mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		
		if (mView == null) 
			mView = inflater.inflate(R.layout.fragment_fileusage, container, false);

		mTextUse = (TextView) mView.findViewById(R.id.textFileUse);
		// mTextMax = (TextView) mView.findViewById(R.id.textMaxSize);
		mTextPath = (TextView) mView.findViewById(R.id.textPath);

		// mImagePopup = (ImageView) mView.findViewById(R.id.imagePopup);
		
		if (mDirectory != null)
			mTextPath.setText(mDirectory.toString());

		/* 
		mLayoutMenu = mView.findViewById(R.id.layoutMaxSizeMenu);
		mLayoutMenu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSizePopup = new PopupMenu(getActivity(), mImagePopup);
				mSizePopup.getMenuInflater().inflate(R.menu.popup_size, mSizePopup.getMenu());
				mSizePopup.show();

				mSizePopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						int newpref = -1;
						switch (item.getItemId()) {
						case R.id.popup_max_50:
							newpref = 0;
							break;
						case R.id.popup_max_100:
							newpref = 1;
							break;
						case R.id.popup_max_250:
							newpref = 2;
							break;
						case R.id.popup_max_1g:
							newpref = 3;
							break;
						case R.id.popup_max_unlimited:
							newpref = 4;
							break;
						}
					
						if (newpref >= 0) {
							SharedPreferences.Editor e = mPreferences.edit();
							e.putInt(PREF_MAXFILE_INT, newpref);
							e.commit();
							updateMaxView();
						}

						return true;
					}
				});
			}
		});
		
		updateMaxView();
		*/

		return mView;
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