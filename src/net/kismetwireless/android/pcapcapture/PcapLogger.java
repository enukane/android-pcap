package net.kismetwireless.android.pcapcapture;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.util.Log;


public class PcapLogger extends PacketHandler {
	static {
		System.loadLibrary("pcap");
	}

	private static String LOGTAG = "PcapLogger";

	ByteBuffer pcap_state;
		
	private String pcapPath;
	private boolean loggingEnabled = false;
	
	private native boolean openPcap(String path, int dlt) throws IOException;
	private native void closePcap();
	private native boolean logPacket(Packet p);
	// TODO add signaling
	private native boolean logPPIPacket(Packet p);
	
	public static int DLT_IEEE80211 = 105;
	public static int DLT_PPI = 192;
	
	@Override
	public void handlePacket(UsbSource s, Packet p) {
		if (loggingEnabled) {
			handledPackets++;
			handledBytes += p.getBytes().length;
			
			logPPIPacket(p);
		}
	}
	
	public void stopLogging() {
		if (!loggingEnabled)
			return;
		
		closePcap();
		loggingEnabled = false;
	}
	
	public boolean getLogging() {
		return loggingEnabled;
	}
	
	public String getPath() {
		return pcapPath;
	}
	
	public boolean startLogging(String path) {
		if (loggingEnabled) {
			stopLogging();
		}
	
		handledPackets = 0;
		handledBytes = 0;
		
		pcapPath = path;
		
		try {
			loggingEnabled = openPcap(path, DLT_PPI);
		} catch (IOException e) {
			Log.e(LOGTAG, "Failed to open pcap: " + e);
			loggingEnabled = false;
		}
	
		return loggingEnabled;
	}
}