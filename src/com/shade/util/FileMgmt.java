package com.shade.util;

import java.io.File;
import java.io.IOException;

public class FileMgmt {
	public static void checkFolders(String[] folders) {
		for (String folder : folders) {
			File f = new File(folder);
			if (!(f.exists() && f.isDirectory()))
				f.mkdir();
		}
		
	}
	
	public static void checkFiles(String[] files) throws IOException {
		for (String file : files) {
			File f = new File(file);
			if (!(f.exists() && f.isFile()))
				f.createNewFile();
		}
	}
}
