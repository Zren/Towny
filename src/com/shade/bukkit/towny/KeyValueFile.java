package com.shade.bukkit.towny;

import java.io.*;
import java.util.*;

public class KeyValueFile {
	BufferedReader fin;
	public HashMap<String, String> keys;
	
	public KeyValueFile(String file) {
		String line;
		String[] tokens;
		keys = new HashMap<String, String>();
		try {
			BufferedReader fin = new BufferedReader(new FileReader(file));
			while ((line = fin.readLine()) != null) {
				tokens = line.split("=");
				if (tokens.length >= 2)
					keys.put(tokens[0], tokens[1]);
			}
			fin.close();
		} catch(Exception e) {}
	}
	
	public String get(String key) {
		return keys.get(key);
	}
}