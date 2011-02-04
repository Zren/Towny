package com.shade.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
	
	// http://www.java-tips.org/java-se-tips/java.io/how-to-copy-a-directory-from-one-location-to-another-loc.html
	public static void copyDirectory(File sourceLocation , File targetLocation) throws IOException {
		if (sourceLocation.isDirectory()) {
			if (!targetLocation.exists())
				targetLocation.mkdir();
			            
			String[] children = sourceLocation.list();
			for (int i=0; i<children.length; i++)
				copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
		} else {      
			InputStream in = new FileInputStream(sourceLocation);
			OutputStream out = new FileOutputStream(targetLocation);
			// Copy the bits from in stream to out stream.
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
				out.write(buf, 0, len);
			in.close();
			out.close();
		}
	}
}
