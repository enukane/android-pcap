package net.kismetwireless.android.pcapcapture;

import java.io.File;

public class FileUtils {
	public static String makeFavoriteKey(File directory, String fname) {
		return "FILEFAV_" + directory.toString() + fname;
	}
}