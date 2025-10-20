package com.redlab.lyrex;

import java.util.ArrayList;
import java.util.List;

public class Setlist {
	private int id;
	private String name;
	private String lyricIdsJson;
	private long timestamp;
	
	public Setlist(int id, String name, String lyricIdsJson, long timestamp) {
		this.id = id;
		this.name = name;
		this.lyricIdsJson = lyricIdsJson;
		this.timestamp = timestamp;
	}
	
	// Simple manual JSON parsing instead of Gson
	public List<Integer> getLyricIds() {
		List<Integer> ids = new ArrayList<>();
		if (lyricIdsJson != null && lyricIdsJson.startsWith("[") && lyricIdsJson.endsWith("]")) {
			String[] parts = lyricIdsJson.replace("[", "").replace("]", "").split(",");
			for (String part : parts) {
				try {
					ids.add(Integer.parseInt(part.trim()));
					} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}
		return ids;
	}
	
	// Getters
	public int getId() { return id; }
	public String getName() { return name; }
	public String getLyricIdsJson() { return lyricIdsJson; }
	public long getTimestamp() { return timestamp; }
}