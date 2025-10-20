package com.redlab.lyrex;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class SetlistDbHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "setlists.db";
	private static final int DATABASE_VERSION = 1;
	
	public static final String TABLE_SETLISTS = "setlists";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_LYRIC_IDS = "lyric_ids";
	public static final String COLUMN_TIMESTAMP = "timestamp";
	
	public SetlistDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_TABLE = "CREATE TABLE " + TABLE_SETLISTS + " (" +
		COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		COLUMN_NAME + " TEXT NOT NULL, " +
		COLUMN_LYRIC_IDS + " TEXT, " +
		COLUMN_TIMESTAMP + " INTEGER);";
		db.execSQL(CREATE_TABLE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETLISTS);
		onCreate(db);
	}
	
	public long createSetlist(String name, List<Integer> lyricIds) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLUMN_NAME, name);
		
		// Manual JSON creation
		StringBuilder jsonBuilder = new StringBuilder("[");
		for (int i = 0; i < lyricIds.size(); i++) {
			jsonBuilder.append(lyricIds.get(i));
			if (i < lyricIds.size() - 1) {
				jsonBuilder.append(",");
			}
		}
		jsonBuilder.append("]");
		values.put(COLUMN_LYRIC_IDS, jsonBuilder.toString());
		
		values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
		
		long id = db.insert(TABLE_SETLISTS, null, values);
		db.close();
		return id;
	}
	
	public List<Setlist> getAllSetlists() {
		List<Setlist> setlists = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor cursor = db.query(TABLE_SETLISTS, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC");
		
		if (cursor != null) {
			while (cursor.moveToNext()) {
				Setlist setlist = new Setlist(
				cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
				cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
				cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LYRIC_IDS)),
				cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
				);
				setlists.add(setlist);
			}
			cursor.close();
		}
		db.close();
		return setlists;
	}
	
	public Setlist getSetlistById(int setlistId) {
		SQLiteDatabase db = this.getReadableDatabase();
		Setlist setlist = null;
		
		Cursor cursor = db.query(
		TABLE_SETLISTS,
		null,
		COLUMN_ID + " = ?",
		new String[]{String.valueOf(setlistId)},
		null, null, null
		);
		
		if (cursor != null && cursor.moveToFirst()) {
			setlist = new Setlist(
			cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
			cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
			cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LYRIC_IDS)),
			cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
			);
			cursor.close();
		}
		db.close();
		return setlist;
	}
	
	// Add this method to SetlistDbHelper.java
	public boolean renameSetlist(int setlistId, String newName) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLUMN_NAME, newName);
		values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
		
		int rowsAffected = db.update(
		TABLE_SETLISTS,
		values,
		COLUMN_ID + " = ?",
		new String[]{String.valueOf(setlistId)}
		);
		
		db.close();
		return rowsAffected > 0;
	}
	
	// ADD THIS METHOD TO FIX THE ERROR:
	public boolean updateSetlistLyrics(int setlistId, List<Integer> lyricIds) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		
		// Convert list of IDs to JSON array string
		StringBuilder jsonBuilder = new StringBuilder("[");
		for (int i = 0; i < lyricIds.size(); i++) {
			jsonBuilder.append(lyricIds.get(i));
			if (i < lyricIds.size() - 1) {
				jsonBuilder.append(",");
			}
		}
		jsonBuilder.append("]");
		
		values.put(COLUMN_LYRIC_IDS, jsonBuilder.toString());
		values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
		
		int rowsAffected = db.update(
		TABLE_SETLISTS,
		values,
		COLUMN_ID + " = ?",
		new String[]{String.valueOf(setlistId)}
		);
		
		db.close();
		return rowsAffected > 0;
	}
	
	public void deleteSetlist(int id) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(
		TABLE_SETLISTS,
		COLUMN_ID + " = ?",
		new String[]{String.valueOf(id)}
		);
		db.close();
	}
}