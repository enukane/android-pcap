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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
	private SharedPreferences mPreferences;
	
	public static final int RENAME_DIALOG_ID = 1001;
	
	private NameListener mNameListener = new NameListener();

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
		mTimeHandler.removeCallbacks(updateTask);
		
		ArrayList<FileEntry> al = new ArrayList<FileEntry>();

		for (String fn : mDirectory.list()) {
			int pos = fn.lastIndexOf('.');
			String ext = fn.substring(pos+1);

			if (mFileTypeMap.containsKey(ext)) {
				// Find if we have an older version and update it
				FileEntry fe = mFileTypeMap.get(ext).getEntry(mDirectory, fn);
				
				if (fe != null) {
					int i = -1;
				
					if (mFileList != null)
						i = mFileList.lastIndexOf(fe);
					
					if (i >= 0) {
						fe = mFileList.get(i);
						fe.refreshFile(mDirectory, fn);
					}
					
					al.add(fe);
				}
			}
		}

		mFileList = al;
		
		Collections.sort(mFileList, new FileDateComparator());
		
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
	
		this.setEmptyText("No files...");
	}

	public static class FileEntry {
		int mIconId;
		String mText;
		String mSmalltext;
		File mDirectory;
		String mFname;
		File mFile;
		FileTyper mFileTyper;
		long mLastModified;
		boolean mDirty;

		public FileEntry(File directory, String fname, int iconid, String text, String small,
				FileTyper typer) {
			this.mDirectory = directory;
			this.mFname = fname;
			this.mIconId = iconid;
			this.mText = text;
			this.mSmalltext = small;
			this.mFileTyper = typer;
			this.mFile = new File(directory + "/" + fname);
			
			this.mLastModified = this.mFile.lastModified();
			this.mDirty = true;
		}
	
		@Override
		public boolean equals(Object x) {
			return ((FileEntry) x).getFile().equals(this.getFile());
		}
		
		@Override
		public int hashCode() {
			return this.getFile().hashCode();
		}
		
		public boolean getDirty() {
			boolean d = mDirty;
			mDirty = false;
		
			// Log.d("FILEDIRTY", "File " + mFname + " " + d);
			
			return d;
		}
		
		public File getFile() {
			return mFile;
		}
		
		public void refreshFile(File directory, String fname) {
			File f = new File(directory + "/" + fname);
			
			if (f.lastModified() != mLastModified) {
				mLastModified = f.lastModified();
				mDirty = true;
			}
		}
		
		public FileTyper getFileTyper() {
			return mFileTyper;
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
		
		public void setSmallText(String t) {
			mSmalltext = t;
		}
		
		public void setDirty() {
			mDirty = true;
			mLastModified = 0;
		}
	}
	
	public class FileDateComparator implements Comparator<FileEntry> {
		@Override
		public int compare(FileEntry o1, FileEntry o2) {
			if (o1.getFile().lastModified() < o2.getFile().lastModified())
				return 1;
			return 0;
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
			boolean newView = false;

			// initialize a view first
			if (view == null) {
				LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
				view = inflater.inflate(mLayoutId, parent, false);
				newView = true;
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
			
			// Queue an update to the details
			if (newView || mitem.getDirty())
				mitem.getFileTyper().updateDetailsView(smalltext, mitem);
			
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
							new PopupMenuAdapter.PopupMenuItem(R.drawable.ic_menu_rename, R.string.popup_rename, 
								new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									// deleteFileDialog(mitem.getFile());
									
									int pos = mitem.getFilename().lastIndexOf('.');
									String base = mitem.getFilename().substring(0, pos);
			
									DialogFragment dialog = 
										NameDialog.newInstance((Activity) mContext, mNameListener, mitem);
									dialog.show(((Activity) mContext).getFragmentManager(), 
											"NameDialog");
									
									popupWindow.dismiss();
								}
							}),
							new PopupMenuAdapter.PopupMenuItem(R.drawable.ic_menu_delete, R.string.popup_delete, 
								new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									deleteFileDialog(mitem.getFile());
									popupWindow.dismiss();
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
	
	public void deleteFileDialog(final File f) {
		AlertDialog.Builder alertbox = new AlertDialog.Builder(getActivity());

		alertbox.setTitle("Delete file?");

		String msg = "Delete file '" + f.getName() + "'?  This can not be undone.";

		alertbox.setMessage(msg);

		alertbox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
			}
		});

		alertbox.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				f.delete();
				Populate();
			}
		});

		alertbox.show();
	}


	public static abstract class FileTyper {
		public abstract FileEntry getEntry(File directory, String fname);
	
		// Perform a long/complex update of the details, this should be done as a runnable
		// posting to the view
		public abstract void updateDetailsView(final TextView v, final FileEntry fe); 
	}
	
	public class NameListener implements DialogListener {	
		@Override
		public void onDialogPositiveClick(DialogFragment dialog, int id) {
			NameDialog nd = (NameDialog) dialog;
			FileEntry f = nd.getFileEntry();
			String nn = nd.getNameString();
			
			// mFileList.remove(f);
			
			f.getFile().renameTo(new File(f.getDirectory() + "/" + nn + ".cap"));
			f.setDirty();
			
			Populate();
		}

		@Override
		public void onDialogNegativeClick(DialogFragment dialog, int id) {
			
		}

		@Override
		public void onDialogNeutralClick(DialogFragment dialog, int id) {
			
		}
		
	}

}