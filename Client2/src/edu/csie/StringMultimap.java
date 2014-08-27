package edu.csie;

import java.util.ArrayList;
import java.util.HashMap;

public class StringMultimap {

	private HashMap<String, ArrayList<String>> mKeySet;
	
	StringMultimap() {
		mKeySet = new HashMap<String, ArrayList<String>>();
	}
	
	void put(String key, String value) {
		if (!mKeySet.containsKey(key)) {
			mKeySet.put(key, new ArrayList<String>());
		}
		mKeySet.get(key).add(value);
	}
	
	ArrayList<String> get(String key) {
		return mKeySet.get(key);
	}
}
