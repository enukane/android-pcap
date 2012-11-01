package net.kismetwireless.android.pcapcapture;

import java.io.File;
import java.io.IOException;

import net.kismetwireless.android.pcapcapture.FilelistFragment.FileEntry;
import android.util.Log;
import android.widget.TextView;

public class PcapFileTyper extends FilelistFragment.FileTyper {
	@Override
	public FilelistFragment.FileEntry getEntry(File directory, String fn) {
		/*
		 * Fetching data is complex so we implement this as a runnable that happens
		 * on demand
		String pcapdetails = "No pcap data";
		try {
			pcapdetails = PcapHelper.countPcapFile(directory.toString() + "/" + fn) + " packets";
		} catch (IOException e) {
			pcapdetails = "Error: " + e;
			Log.e(LOGTAG, "Pcap error: " + e);
		}
		*/
		
		FileEntry f = new FilelistFragment.FileEntry(directory, fn, 
				R.drawable.icon_wireshark, fn, "Fetching Pcap data...", this);

		return f;
	}
	
	@Override
	public void updateDetailsView(final TextView v, final FileEntry fe) {
		v.post(new Runnable() {
			public void run() {
				String pcapdetails = "No pcap data";
				int npackets = 0;
				
				try {
					npackets = 
						PcapHelper.countPcapFile(fe.getDirectory().toString() + "/" + fe.getFilename(), 50000);
					if (npackets == 50000) {
						pcapdetails = "50000+ packets";
					} else {
						pcapdetails = npackets + " packets";
					}
				} catch (IOException e) {
					pcapdetails = "Error: " + e.getMessage();
					// Log.e(LOGTAG, "Pcap error: " + e);
				}
				
				String fd = 
					FileUtils.humanSize(fe.getFile().length()) + ", " + pcapdetails;
				
				fe.setSmallText(fd);
				v.setText(fd);
			}
		});
		
	}
}
