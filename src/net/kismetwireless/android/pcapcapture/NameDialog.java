package net.kismetwireless.android.pcapcapture;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class NameDialog extends DialogFragment {
	static DialogListener mListener;
	View mDialogView;
	FilelistFragment.FileEntry mEntry;
	
	public String getNameString() {
		EditText nametext = (EditText) mDialogView.findViewById(R.id.inputName);
		
		return nametext.getText().toString();
	}
	
	public FilelistFragment.FileEntry getFileEntry() {
		return mEntry;
	}
	
	private void setFileEntry(FilelistFragment.FileEntry e) {
		mEntry = e;
	}

	public static NameDialog newInstance(Activity activity, DialogListener listener, 
			FilelistFragment.FileEntry fileentry) {
		mListener = listener;
			
		NameDialog frag = new NameDialog();
		frag.setFileEntry(fileentry);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		mDialogView = inflater.inflate(R.layout.dialog_rename, null);

		builder.setView(mDialogView)
		.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
                mListener.onDialogPositiveClick(NameDialog.this, FilelistFragment.RENAME_DIALOG_ID);
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
                mListener.onDialogNegativeClick(NameDialog.this, FilelistFragment.RENAME_DIALOG_ID);
			}
		})
		.setTitle("Rename");      
		
		int pos = mEntry.getFilename().lastIndexOf('.');
		String base = mEntry.getFilename().substring(0, pos);
		
		EditText nameText = (EditText) mDialogView.findViewById(R.id.inputName);
		nameText.setText(base);
		
		return builder.create();
	}
}
