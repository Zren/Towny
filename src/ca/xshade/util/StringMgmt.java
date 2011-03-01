package ca.xshade.util;

import java.util.List;

/**
 * Useful functions related to strings, or arrays of them.
 * 
 * @author Shade (Chris H)
 * @version 1.0
 */

public class StringMgmt {
	@SuppressWarnings("rawtypes")
	public static String join(List arr) {
		return join(arr, " ");
	}
	
	@SuppressWarnings("rawtypes")
	public static String join(List arr, String separator) {
		if (arr == null || arr.size() == 0)
			return "";
		String out = arr.get(0).toString();
		for (int i = 1; i < arr.size(); i++)
			out += separator + arr.get(i);
		return out;
	}
	
	public static String join(Object[] arr) {
		return join(arr, " ");
	}
	
	public static String join(Object[] arr, String separator) {
		if (arr.length == 0)
			return "";
		String out = arr[0].toString();
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
