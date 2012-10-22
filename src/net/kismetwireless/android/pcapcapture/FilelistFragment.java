package net.kismetwireless.android.pcapcapture;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FilelistFragment extends ListFragment {
	private static String LOGTAG = "filelist-fragment";
	File mDirectory;
	int mTimeout;
	ArrayList<FileEntry> mFileList;
	TreeMap<String, FileTyper> mFileTypeMap = new TreeMap<String, FileTyper>(String.CASE_INSENSITIVE_ORDER);

	public boolean registerFiletype(String ext, FileTyper ft) {
		if (mFileTypeMap.containsKey(ext))
			return false;
		
		mFileTypeMap.put(ext, ft);
	
		return true;
	}
	
	public void unregisterFiletype(String ext) {
		if (mFileTypeMap.containsKey(ext)) 
			mFileTypeMap.remove(ext);
	}
	
	public FilelistFragment() {
		super();
	}
	
	public FilelistFragment(File directory, int timer) {
		super();
	
		mDirectory = directory;
		mTimeout = timer;
	}
	
	public void Populate() {
		ArrayList<FileEntry> al = new ArrayList<FileEntry>();
		
		for (String fn : mDirectory.list()) {
			Log.d(LOGTAG, fn);
			
			int pos = fn.lastIndexOf('.');
			String ext = fn.substring(pos+1);
			
			Log.d(LOGTAG, "Fn " + fn + " ext '" + ext + "'");
		
			if (mFileTypeMap.containsKey(ext)) {
				al.add(mFileTypeMap.get(ext).getEntry(mDirectory, fn));
			}
		}
	
		mFileList = al;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListAdapter(new FileArrayAdapter(getActivity(),
				R.layout.fragment_filelist_row, mFileList));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.i("FragmentList", "Item clicked: " + id);
	}

	public static class FileEntry {
		int mIconId;
		String mText;
		String mSmalltext;

		public FileEntry(int iconid, String text, String small) {
			this.mIconId = iconid;
			this.mText = text;
			this.mSmalltext = small;
		}

		public int getIconId() {
			return mIconId;
		}

		public String getText() {
			return mText;
		}

		public String getSmallText() {
			return mSmalltext;
		}
	}

	public class FileArrayAdapter extends ArrayAdapter<FileEntry> {
		// FileEntry mFiles[];
		ArrayList<FileEntry> mFiles;
		int mLayoutId;
		Context mContext;

		public FileArrayAdapter(Context context, int layoutResourceId, ArrayList<FileEntry> entries) {
			super(context, layoutResourceId, entries);

			mContext = context;
			mLayoutId = layoutResourceId;
			mFiles = entries;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;

			// initialize a view first
			if (view == null) {
				LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
				view = inflater.inflate(mLayoutId, parent, false);
			}

			// FileEntry mitem = mFiles[position];
			FileEntry mitem = mFiles.get(position);

			ImageView icon = (ImageView) view.findViewById(R.id.imageFileListIcon);
			TextView text = (TextView) view.findViewById(R.id.textFileListName);
			TextView smalltext = (TextView) view.findViewById(R.id.textFileListSmall);

			icon.setImageResource(mitem.getIconId());
			text.setText(mitem.getText());
			smalltext.setText(mitem.getSmallText());

			return view;
		}
		
		@Override
		public int getCount() {
			if (mFiles == null)
				return 0;
			
			return mFiles.size();
		}
	}
	
	public static abstract class FileTyper {
		abstract FileEntry getEntry(File directory, String fname);
	}
}