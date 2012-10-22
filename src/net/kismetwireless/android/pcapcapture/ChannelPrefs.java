package net.kismetwireless.android.pcapcapture;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

public class ChannelPrefs extends PreferenceActivity {	
	String TAG = "channel-pref";
	Preference chlistview = null;
	Preference chlockpick = null;
	
	public OnPreferenceClickListener hopOverrider = new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference pref) {
			Log.d(TAG, "preference hop");
			
			if (((CheckBoxPreference) pref).isChecked()) {
				Log.d(TAG, "channel hop enabled");
				chlistview.setEnabled(true);
				chlistview.setSelectable(true);
				
				chlockpick.setEnabled(false);
				chlockpick.setSelectable(false);
			} else {
				Log.d(TAG, "channel hop disabled");
				chlistview.setEnabled(false);
				chlistview.setSelectable(false);
				
				chlockpick.setEnabled(true);
				chlockpick.setSelectable(true);
			}
			
			return false;
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {    
		super.onCreate(savedInstanceState);       
		addPreferencesFromResource(R.xml.channel_preferences);   
		chlistview = findPreference("channel_list");
		chlockpick = findPreference("channel_lock");
		
		Preference hoppref = findPreference("channel_hop");
		hoppref.setOnPreferenceClickListener(hopOverrider);

		if (((CheckBoxPreference) hoppref).isChecked()) {
			chlistview.setEnabled(true);
			chlistview.setSelectable(true);
			chlockpick.setEnabled(false);
			chlockpick.setSelectable(false);
		} else {
			chlistview.setEnabled(false);
			chlistview.setSelectable(false);
			chlockpick.setEnabled(true);
			chlockpick.setSelectable(true);
		}
	}
}