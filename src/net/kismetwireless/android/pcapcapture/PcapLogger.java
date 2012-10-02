package net.kismetwireless.android.pcapcapture;

import android.util.Log;

public class PcapLogger extends PacketHandler {
	private static String LOGTAG = "PcapLogger";
	
	@Override
	public void handlePacket(UsbSource s, Packet p) {
		// Log.d(LOGTAG, "Packet: " + p.getBytes().length);
	}
}