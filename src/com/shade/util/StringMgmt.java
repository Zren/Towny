package com.shade.util;

import java.util.List;

public class StringMgmt {
	public static String join(List<String> arr) {
		return join(arr, " ");
	}
	
	public static String join(List<String> arr, String separator) {
		if (arr == null || arr.size() == 0)
			return "";
		String out = arr.get(0);
		for (int i = 1; i < arr.size(); i++)
			out += separator + arr.get(i);
		return out;
	}
	
	public static String join(String[] arr) {
		return join(arr, " ");
	}
	
	public static String join(String[] arr, String separator) {
		if (arr.length == 0)
			return "";
		String out = arr[0];
		for (int i = 1; i < arr.length; i++)
			out += separator + arr[i];
		return out;
	}
	
	public static String[] remFirstArg(String[] arr) {
		if (arr.length == 0)
			return arr;
		else {
			String[] newSplit = new String[arr.length - 1];
			System.arraycopy(arr, 1, newSplit, 0, arr.length - 1);
			return newSplit;
		}
	}
}
