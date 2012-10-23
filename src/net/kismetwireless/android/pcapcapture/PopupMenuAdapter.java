package net.kismetwireless.android.pcapcapture;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PopupMenuAdapter extends ArrayAdapter<PopupMenuAdapter.PopupMenuItem> {
	Context context;
	int layoutResourceId;
	PopupMenuItem data[] = null;
	
	public static class PopupMenuItem {
		int id_icon;
		int id_text;
		View.OnClickListener listener;
		
		public PopupMenuItem(int icon, int text, View.OnClickListener listener) {
			this.id_icon = icon;
			this.id_text = text;
			this.listener = listener;
		}
	}

	public PopupMenuAdapter(Context context, int layoutResourceId, PopupMenuItem[] data) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;

		// initialize a view first
		if (view == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			view = inflater.inflate(layoutResourceId, parent, false);
		}

		// String pItem = data[position];
		PopupMenuItem mitem = data[position];
		
		TextView text = (TextView) view.findViewById(R.id.menuItem);
		ImageView img = (ImageView) view.findViewById(R.id.menuImage);
		
		text.setText(mitem.id_text);
		
		if (mitem.id_icon >= 0) {
			img.setImageResource(mitem.id_icon);
			img.setVisibility(View.VISIBLE);
		}
		
		view.setOnClickListener(mitem.listener);

		return view;
	}
}	