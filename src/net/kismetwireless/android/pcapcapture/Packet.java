package net.kismetwireless.android.pcapcapture;

public class Packet {
	// Needs to be public so that JNI can get at it
	public byte[] bytes;
	public byte[] fcs;
	public int signal = 0;
	public int noise = 0;
	
	public int dlt = 0;
		
	// TODO add encoding, etc, if we can support it
	
	public Packet() {
		// nothing
	}
	
	public Packet(byte[] data) {
		bytes = data;
	}
		
	public Packet(byte[] data, int signal) {
		bytes = data;
		this.signal = signal;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public void setBytes(byte[] b) {
		bytes = b;
	}
	
	public void setFcs(byte[] b) {
		fcs = b;
	}
	
	public int getSignal() {
		return signal;
	}
	
	public void setSignal(int s) {
		signal = s;
	}
	
	public int getNoise() {
		return noise;
	}
	
	public void setNoise(int n) {
		noise = n;
	}
	
	public int getDlt() {
		return dlt;
	}
	
	public void setDlt(int d) {
		dlt = d;
	}
}