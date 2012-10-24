package net.kismetwireless.android.pcapcapture;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;

public class FilelistFragment extends ListFragment {
	private static String LOGTAG = "filelist-fragment";
	private File mDirectory;
	private int mTimeout;
	private ArrayList<FileEntry> mFileList;
	private TreeMap<String, FileTyper> mFileTypeMap = new TreeMap<String, FileTyper>(String.CASE_INSENSITIVE_ORDER);
	private Handler mTimeHandler = new Handler();
	private FileArrayAdapter mFileAdapter;
	private boolean mFavorites = false;
	SharedPreferences mPreferences;
	
	private Runnable updateTask = new Runnable() {
		@Override
		public void run() {
			Populate();
		}
	};

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
	
	public void setFavorites(boolean fav) {
		mFavorites = fav;
	}

	public FilelistFragment() {
		super();
	}

	public FilelistFragment(File directory, int timer) {
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
		ArrayList<FileEntry> al = new ArrayList<FileEntry>();

		for (String fn : mDirectory.list()) {
			Log.d(LOGTAG, fn);

			int pos = fn.lastIndexOf('.');
			String ext = fn.substring(pos+1);

			Log.d(LOGTAG, "Fn " + fn + " ext '" + ext + "'");

			if (mFileTypeMap.containsKey(ext)) {
				FileEntry fe = mFileTypeMap.get(ext).getEntry(mDirectory, fn);
				
				if (fe != null)
					al.add(fe);
			}
		}

		mFileList = al;
		
		if (mFileAdapter != null)
			mFileAdapter.updateEntries(mFileList);
		
		if (mTimeout > 0)
			mTimeHandler.postDelayed(updateTask, mTimeout);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mFileAdapter = new FileArrayAdapter(getActivity(), R.layout.fragment_filelist_row, mFileList);
		setListAdapter(mFileAdapter);
		
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
	}

	public static class FileEntry {
		int mIconId;
		String mText;
		String mSmalltext;
		File mDirectory;
		String mFname;

		public FileEntry(File directory, String fname, int iconid, String text, String small) {
			this.mDirectory = directory;
			this.mFname = fname;
			this.mIconId = iconid;
			this.mText = text;
			this.mSmalltext = small;
		}

		public File getDirectory() {
			return mDirectory;
		}

		public String getFilename() {
			return mFname;
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
		
		public void updateEntries(ArrayList<FileEntry> entries) {
			mFiles = entries;
			notifyDataSetChanged();
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
			final FileEntry mitem = mFiles.get(position);

			ImageView icon = (ImageView) view.findViewById(R.id.imageFileListIcon);
			final ImageView fav = (ImageView) view.findViewById(R.id.imageFileListRating);
			TextView text = (TextView) view.findViewById(R.id.textFileListName);
			TextView smalltext = (TextView) view.findViewById(R.id.textFileListSmall);

			icon.setImageResource(mitem.getIconId());
			text.setText(mitem.getText());
			smalltext.setText(mitem.getSmallText());
			
			final String favkey = FileUtils.makeFavoriteKey(mitem.getDirectory(), mitem.getFilename());
			
			if (mFavorites) {
				fav.setVisibility(View.VISIBLE);
				
				if (mPreferences.getBoolean(favkey, false)) {
					fav.setImageResource(R.drawable.rating_important);
				} else {
					fav.setImageResource(R.drawable.rating_not_important);
				}
				
				fav.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean newpref = false;
						
						if (mPreferences.getBoolean(favkey, false)) {
							newpref = false;
							fav.setImageResource(R.drawable.rating_not_important);
						} else {
							newpref = true;
							fav.setImageResource(R.drawable.rating_important);
						}
						
						SharedPreferences.Editor e = mPreferences.edit();
						e.putBoolean(favkey, newpref);
						e.commit();

					}
				});
				
			} else {
				fav.setVisibility(View.GONE);
			}

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ImageView popup = (ImageView) v.findViewById(R.id.imageFileListPopup);
					final ListPopupWindow popupWindow = new ListPopupWindow(mContext);
					popupWindow.setModal(true);
					
					PopupMenuAdapter.PopupMenuItem popupItems[] = new PopupMenuAdapter.PopupMenuItem[] {
							new PopupMenuAdapter.PopupMenuItem(R.drawable.ic_menu_share, R.string.popup_share, 
								new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									Intent i = new Intent(Intent.ACTION_SEND); 
									i.setType("application/cap"); 
									// i.setType("application/binary");
									i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + mitem.getDirectory() + 
											"/" + mitem.getFilename())); 
									startActivity(Intent.createChooser(i, "Share Pcap file"));
									popupWindow.dismiss();
								}
							}),
							new PopupMenuAdapter.PopupMenuItem(R.drawable.ic_menu_delete, R.string.popup_delete, 
								new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									
								}
							}),
					};

					PopupMenuAdapter popupAdapter = new PopupMenuAdapter(mContext, R.layout.popup_item_row, 
							popupItems);
					popupWindow.setAdapter(popupAdapter);
					popupAdapter.notifyDataSetChanged();
					
					popupWindow.setContentWidth(v.getWidth() / 2);
					popupWindow.setAnchorView(popup);
					popupWindow.show();
				}
			});

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