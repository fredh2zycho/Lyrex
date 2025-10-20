package com.redlab.lyrex;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LyricDbHelper extends SQLiteOpenHelper {
	private static final String TAG = "LyricDbHelper";
	
	private static final String DATABASE_NAME = "lyricpad.db";
	private static final int DATABASE_VERSION = 2;
	
	public static final String TABLE_LYRICS = "lyrics";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_CONTENT = "content";
	public static final String COLUMN_TIMESTAMP = "timestamp";
	public static final String COLUMN_SECTION_TYPE = "section_type";
	
	public LyricDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public static class RestoreResult {
		public final boolean parsed;
		public final int addedCount;
		public final String errorMessage;
		
		public RestoreResult(boolean parsed, int addedCount, String errorMessage) {
			this.parsed = parsed;
			this.addedCount = addedCount;
			this.errorMessage = errorMessage;
		}
		
		public boolean isSuccessfulInsert() {
			return parsed && addedCount > 0;
		}
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		String TABLE_CREATE = "CREATE TABLE " + TABLE_LYRICS + " (" +
		COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		COLUMN_TITLE + " TEXT NOT NULL, " +
		COLUMN_CONTENT + " TEXT NOT NULL, " +
		COLUMN_TIMESTAMP + " INTEGER, " +
		COLUMN_SECTION_TYPE + " TEXT DEFAULT 'verse');";
		db.execSQL(TABLE_CREATE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			db.execSQL("ALTER TABLE " + TABLE_LYRICS +
			" ADD COLUMN " + COLUMN_SECTION_TYPE + " TEXT DEFAULT 'verse'");
		}
	}
	
	public long insertLyric(Lyric lyric) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLUMN_TITLE, lyric.getTitle());
		values.put(COLUMN_CONTENT, lyric.getContent());
		values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
		values.put(COLUMN_SECTION_TYPE, lyric.getSectionType());
		
		long id = db.insert(TABLE_LYRICS, null, values);
		db.close();
		return id;
	}
	
	public Lyric getLyricById(int id) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(
		TABLE_LYRICS,
		null,
		COLUMN_ID + " = ?",
		new String[]{String.valueOf(id)},
		null, null, null);
		
		Lyric lyric = null;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				lyric = new Lyric(
				cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
				cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
				);
				lyric.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
				lyric.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
				lyric.setSectionType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SECTION_TYPE)));
			}
			cursor.close();
		}
		db.close();
		return lyric;
	}
	
	public List<Lyric> getAllLyrics(String sortOrder) {
		List<Lyric> lyrics = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor cursor = db.query(
		TABLE_LYRICS,
		null, null, null, null, null,
		sortOrder.equals("title") ?
		COLUMN_TITLE + " COLLATE NOCASE ASC" :
		COLUMN_TIMESTAMP + " DESC");
		
		if (cursor != null) {
			while (cursor.moveToNext()) {
				Lyric lyric = new Lyric(
				cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
				cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
				);
				lyric.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
				lyric.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
				lyric.setSectionType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SECTION_TYPE)));
				lyrics.add(lyric);
			}
			cursor.close();
		}
		db.close();
		return lyrics;
	}
	
	public int updateLyric(Lyric lyric) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLUMN_TITLE, lyric.getTitle());
		values.put(COLUMN_CONTENT, lyric.getContent());
		values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
		values.put(COLUMN_SECTION_TYPE, lyric.getSectionType());
		
		int rowsAffected = db.update(
		TABLE_LYRICS,
		values,
		COLUMN_ID + " = ?",
		new String[]{String.valueOf(lyric.getId())});
		db.close();
		return rowsAffected;
	}
	
	public List<Lyric> getLyricsBySection(String sectionType) {
		List<Lyric> lyrics = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor cursor = db.query(
		TABLE_LYRICS,
		null,
		COLUMN_SECTION_TYPE + " = ?",
		new String[]{sectionType},
		null, null,
		COLUMN_TIMESTAMP + " DESC");
		
		if (cursor != null) {
			while (cursor.moveToNext()) {
				Lyric lyric = new Lyric(
				cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
				cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
				);
				lyric.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
				lyric.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
				lyric.setSectionType(sectionType);
				lyrics.add(lyric);
			}
			cursor.close();
		}
		db.close();
		return lyrics;
	}
	
	public void deleteLyric(int id) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(
		TABLE_LYRICS,
		COLUMN_ID + " = ?",
		new String[]{String.valueOf(id)});
		db.close();
	}
	
	// BACKUP / RESTORE
	
	public String createBackup() {
		List<Lyric> lyrics = getAllLyrics("timestamp");
		StringBuilder backup = new StringBuilder();
		
		backup.append("=== LYREX BACKUP ===\n");
		
		for (Lyric lyric : lyrics) {
			backup.append("\nTitle: ").append(lyric.getTitle()).append("\n");
			backup.append("Content:\n").append(lyric.getContent()).append("\n");
		}
		return backup.toString();
	}
	
	// --- New Restore Logic ---
	public RestoreResult restoreFromBackup(SQLiteDatabase db, String backupContent) {
		List<Lyric> parsedLyrics = null;
		
		// ... (same parsing logic as before) ...
		if (isNewFormatBackup(backupContent)) {
			parsedLyrics = parseNewFormat(backupContent);
			} else if (isOldFormatBackup(backupContent)) {
			parsedLyrics = parseOldFormat(backupContent);
			} else {
			parsedLyrics = parseGenericFormat(backupContent);
		}
		
		if (parsedLyrics == null) {
			return new RestoreResult(false, 0, "Invalid backup format or content.");
		}
		
		// Call the new, non-destructive insert method
		return addParsedLyrics(db, parsedLyrics);
	}
	
	public RestoreResult addParsedLyrics(SQLiteDatabase db, List<Lyric> lyrics) {
		int addedCount = 0;
		try {
			db.beginTransaction();
			
			for (Lyric lyric : lyrics) {
				// Check if a lyric with the same title and content already exists
				if (!lyricExists(db, lyric.getTitle(), lyric.getContent())) {
					ContentValues values = new ContentValues();
					values.put(COLUMN_TITLE, lyric.getTitle());
					values.put(COLUMN_CONTENT, lyric.getContent());
					values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
					values.put(COLUMN_SECTION_TYPE, lyric.getSectionType());
					db.insert(TABLE_LYRICS, null, values);
					addedCount++;
				}
			}
			
			db.setTransactionSuccessful();
			return new RestoreResult(true, addedCount, null);
			} catch (Exception e) {
			return new RestoreResult(false, 0, e.getMessage());
			} finally {
			try {
				if (db.inTransaction()) {
					db.endTransaction();
				}
				} catch (Exception e) {
				// Ignore
			}
		}
	}
	
	public RestoreResult insertParsedLyrics(SQLiteDatabase db, List<Lyric> lyrics) {
		int addedCount = 0;
		try {
			db.beginTransaction();
			db.delete(TABLE_LYRICS, null, null);
			
			for (Lyric lyric : lyrics) {
				ContentValues values = new ContentValues();
				values.put(COLUMN_TITLE, lyric.getTitle());
				values.put(COLUMN_CONTENT, lyric.getContent());
				values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
				values.put(COLUMN_SECTION_TYPE, lyric.getSectionType());
				db.insert(TABLE_LYRICS, null, values);
				addedCount++;
			}
			
			db.setTransactionSuccessful();
			return new RestoreResult(true, addedCount, null);
			} catch (Exception e) {
			return new RestoreResult(false, 0, e.getMessage());
			} finally {
			try {
				if (db.inTransaction()) {
					db.endTransaction();
				}
				} catch (Exception e) {
				// Ignore
			}
		}
	}
	
	public static List<Lyric> parseNewFormat(String content) {
		List<Lyric> lyrics = new ArrayList<>();
		String[] blocks = content.split("=== LYREX BACKUP ===");
		if (blocks.length <= 1) return null;
		
		for (String block : blocks) {
			if (block.trim().isEmpty()) continue;
			
			String title = null;
			StringBuilder contentBuilder = new StringBuilder();
			
			String[] lines = block.split("\n");
			for (String line : lines) {
				line = line.trim();
				if (line.startsWith("Title:")) {
					title = line.substring(6).trim();
					} else if (!line.startsWith("Backup Date:") && !line.isEmpty()) {
					contentBuilder.append(line).append("\n");
				}
			}
			if (title != null) {
				lyrics.add(new Lyric(title, contentBuilder.toString().trim()));
			}
		}
		return lyrics;
	}
	
	public static List<Lyric> parseOldFormat(String content) {
		List<Lyric> lyrics = new ArrayList<>();
		String[] blocks = content.split("---");
		
		for (String block : blocks) {
			String[] lines = block.trim().split("\n");
			if (lines.length < 2) continue;
			
			String title = lines[0].replaceAll("[\\*]", "").trim();
			StringBuilder contentBuilder = new StringBuilder();
			
			for (int i = 1; i < lines.length; i++) {
				if (lines[i].trim().isEmpty()) continue;
				contentBuilder.append(lines[i].trim()).append("\n");
			}
			lyrics.add(new Lyric(title, contentBuilder.toString().trim()));
		}
		return lyrics;
	}
	
	public static List<Lyric> parseGenericFormat(String content) {
		List<Lyric> lyrics = new ArrayList<>();
		// Simple fallback
		String[] lines = content.split("\n");
		String title = null;
		StringBuilder contentBuilder = new StringBuilder();
		
		for (String line : lines) {
			if (line.trim().isEmpty()) {
				if (title != null) {
					lyrics.add(new Lyric(title, contentBuilder.toString().trim()));
					title = null;
					contentBuilder = new StringBuilder();
				}
				} else if (title == null) {
				title = line.trim();
				} else {
				contentBuilder.append(line.trim()).append("\n");
			}
		}
		
		if (title != null) {
			lyrics.add(new Lyric(title, contentBuilder.toString().trim()));
		}
		return lyrics;
	}
	
	public static boolean isNewFormatBackup(String content) {
		return content.contains("=== LYREX BACKUP ===");
	}
	
	public static boolean isOldFormatBackup(String content) {
		return content.contains("╔") && content.contains("LYREX EXPORT");
	}
	
	// You already have a lyricExists method, which is perfect for this.
	private boolean lyricExists(SQLiteDatabase db, String title, String content) {
		Cursor cursor = null;
		try {
			cursor = db.query(
			TABLE_LYRICS,
			new String[]{COLUMN_ID},
			COLUMN_TITLE + " = ? AND " + COLUMN_CONTENT + " = ?",
			new String[]{title, content},
			null, null, null
			);
			return cursor != null && cursor.getCount() > 0;
			} catch (Exception e) {
			Log.e(TAG, "lyricExists check failed", e);
			return false;
			} finally {
			if (cursor != null) cursor.close();
		}
	}
	
	// escaping/unescaping (kept as-is)
	private String escapeString(String input) {
		if (input == null) return "";
		return input.replace("\\", "\\\\").replace("|", "\\p").replace("\r", "\\r").replace("\n", "\\n");
	}
	
	private String unescapeString(String input) {
		if (input == null) return "";
		return input.replace("\\n", "\n").replace("\\r", "\r").replace("\\p", "|").replace("\\\\", "\\");
	}
	
	public String exportLyrics() {
		List<Lyric> lyrics = getAllLyrics("title");
		StringBuilder export = new StringBuilder();
		
		for (Lyric lyric : lyrics) {
			export.append("[").append(lyric.getSectionType().toUpperCase()).append("]\n")
			.append(lyric.getTitle()).append("\n\n")
			.append(lyric.getContent()).append("\n\n")
			.append("---\n\n");
		}
		
		return export.toString();
	}
}