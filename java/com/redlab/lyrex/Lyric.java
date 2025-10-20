package com.redlab.lyrex;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.content.Context;
import android.os.Environment;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Lyric {
	private int id;
	private String title;
	private String content;
	private long timestamp;
	private String sectionType;
	private SpannableStringBuilder styledContent;
	
	// Constructor for new lyrics
	public Lyric(String title, String content) {
		this.title = title;
		this.content = content;
		this.timestamp = System.currentTimeMillis();
		this.sectionType = "verse";
		this.styledContent = null;
	}
	
	// Constructor for database retrieval
	public Lyric(int id, String title, String content, long timestamp, String sectionType) {
		this.id = id;
		this.title = title;
		this.content = content;
		this.timestamp = timestamp;
		this.sectionType = (sectionType != null) ? sectionType : "verse";
		this.styledContent = null;
	}
	
	// Getters
	public int getId() { return id; }
	public String getTitle() { return title; }
	public String getContent() { return content; }
	public long getTimestamp() { return timestamp; }
	public String getSectionType() { return sectionType; }
	public SpannableStringBuilder getStyledContent() { return styledContent; }
	
	// Setters
	public void setId(int id) { this.id = id; }
	public void setTitle(String title) { this.title = title; }
	public void setContent(String content) {
		this.content = content;
		this.styledContent = null;
	}
	public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
	public void setSectionType(String sectionType) {
		this.sectionType = (sectionType != null) ? sectionType.toLowerCase() : "verse";
	}
	
	/**
	* Highlights section keywords in the lyric content
	*/
	public void highlightSections() {
		if (content == null || content.trim().isEmpty()) {
			styledContent = null;
			return;
		}
		
		styledContent = new SpannableStringBuilder(content);
		
		String[] patterns = {
			"\\b(VERSE|Verse|verse|\\[VERSE\\]|\\[Verse\\]|\\[verse\\])\\b",
			"\\b(PRE-CHORUS|Pre-Chorus|pre-chorus|\\[PRE-CHORUS\\]|\\[Pre-Chorus\\]|\\[pre-chorus\\])\\b",
			"\\b(CHORUS|Chorus|chorus|\\[CHORUS\\]|\\[Chorus\\]|\\[chorus\\])\\b",
			"\\b(BRIDGE|Bridge|bridge|\\[BRIDGE\\]|\\[Bridge\\]|\\[bridge\\])\\b",
			"\\b(REFRAIN|Refrain|refrain|\\[REFRAIN\\]|\\[Refrain\\]|\\[refrain\\])\\b"
		};
		
		int[] colors = {
			Color.parseColor("#2196F3"),    // Blue for VERSE
			Color.parseColor("#4CAF50"),    // Green for PRE-CHORUS
			Color.parseColor("#F44336"),    // Red for CHORUS
			Color.parseColor("#9C27B0"),    // Purple for BRIDGE
			Color.parseColor("#FF9800")     // Orange for REFRAIN
		};
		
		for (int i = 0; i < patterns.length; i++) {
			Pattern pattern = Pattern.compile(patterns[i], Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(content);
			
			while (matcher.find()) {
				styledContent.setSpan(
				new ForegroundColorSpan(colors[i]),
				matcher.start(),
				matcher.end(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				);
				
				styledContent.setSpan(
				new StyleSpan(Typeface.BOLD),
				matcher.start(),
				matcher.end(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				);
			}
		}
	}
	
	/**
	* Export lyric to TXT file in external storage
	*/
	public String exportToTxtFile(Context context) {
		try {
			// Create main LyrEx folder in external storage
			File externalStorage = Environment.getExternalStorageDirectory();
			File lyrexMainFolder = new File(externalStorage, "LyrEx");
			
			// Create subfolders for organization
			File lyricsFolder = new File(lyrexMainFolder, "Lyrics");
			File backupsFolder = new File(lyrexMainFolder, "Backups");
			File exportsFolder = new File(lyrexMainFolder, "Exports");
			
			// Create all folders if they don't exist
			if (!lyricsFolder.exists()) {
				lyricsFolder.mkdirs();
			}
			if (!backupsFolder.exists()) {
				backupsFolder.mkdirs();
			}
			if (!exportsFolder.exists()) {
				exportsFolder.mkdirs();
			}
			
			// Create filename with proper formatting
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
			String timestamp = sdf.format(new Date());
			String fileName = sanitizeFileName(title) + "_" + timestamp + ".txt";
			
			File txtFile = new File(lyricsFolder, fileName);
			
			// Create separator line
			String separator = "========================================";
			
			// Write content to file with professional formatting
			FileWriter writer = new FileWriter(txtFile);
			writer.write("╔══════════════════════════════════════╗\n");
			writer.write("║            LYREX EXPORT              ║\n");
			writer.write("╚══════════════════════════════════════╝\n\n");
			
			writer.write("📝 SONG INFORMATION:\n");
			writer.write(separator + "\n");
			writer.write("Title: " + title + "\n");
			writer.write("Created: " + new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(new Date(this.timestamp)) + "\n");
			writer.write("Section Type: " + sectionType.toUpperCase() + "\n");
			writer.write("Export Date: " + new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(new Date()) + "\n");
			writer.write("File Location: " + txtFile.getAbsolutePath() + "\n");
			
			writer.write("\n🎵 LYRICS:\n");
			writer.write(separator + "\n\n");
			writer.write(content);
			writer.write("\n\n" + separator + "\n");
			writer.write("📱 Exported by LyrEx - Professional Lyrics Manager\n");
			writer.write("🌐 Version: 1.0.0\n");
			writer.write("⏰ " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n");
			writer.write(separator);
			writer.close();
			
			// Also create a backup in the Backups folder
			createBackupCopy(new File(backupsFolder, fileName), content);
			
			return txtFile.getAbsolutePath();
			
			} catch (Exception e) {
			e.printStackTrace();
			
			// Fallback to app-specific directory
			try {
				File appDir = new File(context.getExternalFilesDir(null), "LyrEx_Professional");
				File lyricsDir = new File(appDir, "Lyrics");
				lyricsDir.mkdirs();
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
				String timestamp = sdf.format(new Date());
				String fileName = sanitizeFileName(title) + "_" + timestamp + ".txt";
				
				File txtFile = new File(lyricsDir, fileName);
				FileWriter writer = new FileWriter(txtFile);
				writer.write("=== LYREX PROFESSIONAL ===\n\n");
				writer.write("Title: " + title + "\n");
				writer.write("Created: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n\n");
				writer.write(content);
				writer.write("\n\n--- End of Export ---");
				writer.close();
				
				return txtFile.getAbsolutePath();
				
				} catch (Exception e2) {
				e2.printStackTrace();
				return null;
			}
		}
	}
	
	private void createBackupCopy(File backupFile, String content) {
		try {
			FileWriter backupWriter = new FileWriter(backupFile);
			backupWriter.write("=== LYREX BACKUP ===\n");
			backupWriter.write("Title: " + title + "\n");
			backupWriter.write("Backup Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n\n");
			backupWriter.write(content);
			backupWriter.close();
			} catch (Exception e) {
			// Backup failed, but main export succeeded
		}
	}
	
	private String sanitizeFileName(String fileName) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return "Untitled_Song";
		}
		// Remove invalid characters and make it professional
		String sanitized = fileName.replaceAll("[^a-zA-Z0-9._\\-\\s]", "_");
		// Replace spaces with underscores for better file handling
		sanitized = sanitized.replaceAll("\\s+", "_");
		// Limit length and make it look professional
		if (sanitized.length() > 40) {
			sanitized = sanitized.substring(0, 40);
		}
		return sanitized.trim().replaceAll("_+", "_"); // Remove multiple underscores
	}
	
	public void detectSectionType() {
		if (content == null) return;
		
		String lowerContent = content.toLowerCase();
		
		if (lowerContent.contains("chorus")) {
			sectionType = "chorus";
			} else if (lowerContent.contains("pre-chorus")) {
			sectionType = "pre-chorus";
			} else if (lowerContent.contains("bridge")) {
			sectionType = "bridge";
			} else if (lowerContent.contains("refrain")) {
			sectionType = "refrain";
			} else {
			sectionType = "verse";
		}
		
		highlightSections();
	}
	
	public void autoDetectSection() {
		if (content == null || content.trim().isEmpty()) return;
		
		String[] lines = content.split("\n");
		if (lines.length == 0) return;
		
		String firstLine = lines[0].toLowerCase().trim();
		
		if (firstLine.contains("chorus:") || firstLine.contains("[chorus]")) {
			sectionType = "chorus";
			} else if (firstLine.contains("pre-chorus:") || firstLine.contains("[pre-chorus]")) {
			sectionType = "pre-chorus";
			} else if (firstLine.contains("bridge:") || firstLine.contains("[bridge]")) {
			sectionType = "bridge";
			} else if (firstLine.contains("refrain:") || firstLine.contains("[refrain]")) {
			sectionType = "refrain";
			} else if (firstLine.contains("verse:") || firstLine.contains("[verse]")) {
			sectionType = "verse";
		}
		
		highlightSections();
	}
	
	public boolean hasHighlights() {
		return styledContent != null && !styledContent.toString().equals(content);
	}
	
	public void clearHighlights() {
		styledContent = null;
	}
}