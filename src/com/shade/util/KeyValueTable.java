package com.shade.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class KeyValueTable {
	private List<KeyValue> keyValues = new ArrayList<KeyValue>();
	
	public List<KeyValue> getKeyValues() {
		return keyValues;
	}

	public void setKeyValues(List<KeyValue> keyValues) {
		this.keyValues = keyValues;
	}

	public KeyValueTable(Hashtable<?,?> table) {
		this(Arrays.asList(table.keySet().toArray()), new ArrayList<Object>(table.values()));
	}
	
	public KeyValueTable(List<Object> keys, List<Object> values)  {
		//if (keys.size() != values.size())
		//	throw new Exception();
		
		for (int i = 0; i < keys.size(); i++)
			keyValues.add(new KeyValue(keys.get(i), values.get(i)));
	}
	
	public void sortByKey() {
		Collections.sort(keyValues, new Sorting.KeySort());
	}
	
	public void sortByValue() {
		Collections.sort(keyValues, new Sorting.ValueSort());
	}
	
	public void revese() {
		Collections.reverse(keyValues);
	}
}