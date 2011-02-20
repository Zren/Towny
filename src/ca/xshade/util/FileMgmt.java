package ca.xshade.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
	
	public static void zipDirectory(File sourceFolder, File destination) throws IOException {
		ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destination));
		recursiveZipDirectory(sourceFolder, output);
		output.close();
	}

	public static void zipDirectories(File[] sourceFolders, File destination) throws IOException {
		ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destination));
		for (File sourceFolder : sourceFolders)
			recursiveZipDirectory(sourceFolder, output);
		output.close();
	}
	
	public static void recursiveZipDirectory(File sourceFolder, ZipOutputStream zipStream) throws IOException {
		String[] dirList = sourceFolder.list();
		byte[] readBuffer = new byte[2156];
		int bytesIn = 0;
		for (int i = 0; i < dirList.length; i++) {
			File f = new File(sourceFolder, dirList[i]);
			if (f.isDirectory()) {
				recursiveZipDirectory(f, zipStream);
				continue;
			} else {
				FileInputStream input = new FileInputStream(f);
				ZipEntry anEntry = new ZipEntry(f.getPath());
				zipStream.putNextEntry(anEntry);
				while((bytesIn = input.read(readBuffer)) != -1)
					zipStream.write(readBuffer, 0, bytesIn);
				input.close();
			}
		}
	}

}
