package net.kismetwireless.android.pcapcapture;

import java.io.File;

import android.content.SharedPreferences;

public class FileUtils {
	/* Make a shared preferences key for a file...  beware, passing a trailing /
	 * on a directory one time and not on another can lead to mis-matched keys */
	public static String makeFavoriteKey(File directory, String fname) {
		return "FILEFAV_" + directory.toString() + fname;
	}
	
	/* Count # of files
	 * Favorite = false && nonfavorite = false, count all files which match extensions
	 * Otherwise count favorite or nonfavorite files.
	 * An empty set of extensions implies all files
	 */
	public static int countFiles(File directory, String extensions[],
			boolean favorite, boolean nonfavorite, SharedPreferences prefs) {
		int total = 0;

		for (String fn : directory.list()) {
			boolean pass = false;
			
			int pos = fn.lastIndexOf('.');
			String ext = fn.substring(pos+1);

			for (String e : extensions) {
				if (e.equalsIgnoreCase(ext))
					pass = true;
			}
			
			if (!pass && extensions.length != 0)
				continue;
			
			if (prefs != null) {
				// Favorite only files and this isn't one
				if (favorite && !prefs.getBoolean(FileUtils.makeFavoriteKey(directory, fn), false)) {
					continue;
				}
			
				// Nonfavorite only files, and this isn't one
				if (nonfavorite && prefs.getBoolean(FileUtils.makeFavoriteKey(directory, fn), false)) {
					continue;
				}
			}
			
			total++;
		}
		
		return total;
	}
	
	/* Count file sizes.
	 * Favorite = false && nonfavorite = false, count all files which match extensions
	 * Otherwise count favorite or nonfavorite files for size.
	 * An empty set of extensions implies all files
	 */
	public static long countFileSizes(File directory, String extensions[], 
			boolean favorite, boolean nonfavorite, SharedPreferences prefs) {
		long total = 0;
	
		for (String fn : directory.list()) {
			boolean pass = false;
			
			int pos = fn.lastIndexOf('.');
			String ext = fn.substring(pos+1);

			for (String e : extensions) {
				if (e.equalsIgnoreCase(ext))
					pass = true;
			}
			
			if (!pass && extensions.length != 0)
				continue;
			
			if (prefs != null) {
				// Favorite only files and this isn't one
				if (favorite && !prefs.getBoolean(FileUtils.makeFavoriteKey(directory, fn), false)) {
					continue;
				}
			
				// Nonfavorite only files, and this isn't one
				if (nonfavorite && prefs.getBoolean(FileUtils.makeFavoriteKey(directory, fn), false)) {
					continue;
				}
			}
			
			File f = new File(directory + "/" + fn);
			
			total += f.length();
			// total += getFileSize(f.toString());
		}
		
		return total;
	}

	/* DELETE FILES.  CAN BE DANGEROUS!
	 * Favorite = false && nonfavorite = false, DELETE ALL FILES which match extensions
	 * Otherwise DELETE favorite or nonfavorite files.
	 * An empty set of extensions implies ALL FILES
	 */
	public static void deleteFiles(File directory, String extensions[],
			boolean favorite, boolean nonfavorite, SharedPreferences prefs) {
		int total = 0;

		for (String fn : directory.list()) {
			boolean pass = false;
			
			int pos = fn.lastIndexOf('.');
			String ext = fn.substring(pos+1);

			for (String e : extensions) {
				if (e.equalsIgnoreCase(ext))
					pass = true;
			}
			
			if (!pass && extensions.length != 0)
				continue;
			
			if (prefs != null) {
				// Favorite only files and this isn't one
				if (favorite && !prefs.getBoolean(FileUtils.makeFavoriteKey(directory, fn), false)) {
					continue;
				}
			
				// Nonfavorite only files, and this isn't one
				if (nonfavorite && prefs.getBoolean(FileUtils.makeFavoriteKey(directory, fn), false)) {
					continue;
				}
			}
			
			File f = new File(directory + "/" + fn);
			
			f.delete();
		}
	}
}