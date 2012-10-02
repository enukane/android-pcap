package net.kismetwireless.android.pcapcapture;

import java.util.ArrayList;

abstract public class PacketHandler {
	private Object mPacketLock = new Object();
	ArrayList<UsbSource> mSourceArray = new ArrayList<UsbSource>();

	public PacketHandler() {
		// Nothing?
	}
	
	public void addUsbSource(UsbSource s) {
		synchronized (mPacketLock) {
			if (mSourceArray.contains(s))
				return;

			mSourceArray.add(s);
		}
	}

	public void removeUsbSource(UsbSource s) {
		synchronized (mPacketLock) {
			if (mSourceArray.contains(s)) {
				mSourceArray.remove(s);
			}
		}
	}

	abstract public void handlePacket(UsbSource source, Packet packet);

}