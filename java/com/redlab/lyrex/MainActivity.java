package com.redlab.lyrex;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.zip.Inflater;
import java.util.zip.Deflater;

// Custom ActivityResultContract for OpenDocumentMultiple
class OpenMultipleDocumentsContract extends androidx.activity.result.contract.ActivityResultContract<String[], List<Uri>> {
	
	@Override
	public Intent createIntent(Context context, String[] mimeTypes) {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		
		if (mimeTypes != null && mimeTypes.length > 0) {
			// Use the first MIME type as primary, add others as extra
			intent.setType(mimeTypes[0]);
			if (mimeTypes.length > 1) {
				intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
			}
			} else {
			intent.setType("*/*");
		}
		
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		return intent;
	}
	
	@Override
	public List<Uri> parseResult(int resultCode, Intent intent) {
		if (resultCode != android.app.Activity.RESULT_OK || intent == null) {
			return Collections.emptyList();
		}
		
		List<Uri> uris = new ArrayList<>();
		
		// Handle multiple selections (clipData)
		ClipData clipData = intent.getClipData();
		if (clipData != null) {
			for (int i = 0; i < clipData.getItemCount(); i++) {
				Uri uri = clipData.getItemAt(i).getUri();
				if (uri != null) {
					uris.add(uri);
				}
			}
		}
		// Handle single selection (data)
		else if (intent.getData() != null) {
			uris.add(intent.getData());
		}
		
		return uris;
	}
}

public class MainActivity extends AppCompatActivity {
	private static final int QR_SCAN_REQUEST_CODE = 1001;
	private LyricAdapter adapter;
	private LyricDbHelper dbHelper;
	private SetlistDbHelper setlistDbHelper;
	private boolean sortByTitle = true;
	private boolean isFabMenuOpen = false;
	private View fabMenu;
	
	// Using custom OpenMultipleDocumentsContract
	private ActivityResultLauncher<String[]> pickBackupLauncher;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		dbHelper = new LyricDbHelper(this);
		setlistDbHelper = new SetlistDbHelper(this);
		
		// Register custom contract
		pickBackupLauncher = registerForActivityResult(
		new OpenMultipleDocumentsContract(),
		uris -> {
			if (uris != null && !uris.isEmpty()) {
				for (Uri uri : uris) {
					try {
						getContentResolver().takePersistableUriPermission(
						uri,
						Intent.FLAG_GRANT_READ_URI_PERMISSION
						);
					} catch (Exception ignored) {}
				}
				processBackupFiles(uris);
			}
		}
		);
		
		setupRecyclerView();
		setupFabMenu();
		loadLyrics();
	}
	
	private void setupRecyclerView() {
		RecyclerView recyclerView = findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		
		adapter = new LyricAdapter(this, new ArrayList<>(), lyric -> showLyricOptions(lyric));
		recyclerView.setAdapter(adapter);
	}
	
	private void setupFabMenu() {
		FloatingActionButton fabMain = findViewById(R.id.fab_main);
		
		fabMenu = getLayoutInflater().inflate(R.layout.layout_fab_menu, null);
		fabMenu.setVisibility(View.GONE);
		
		// Get your actual CoordinatorLayout by ID instead of android.R.id.content
		CoordinatorLayout rootView = findViewById(R.id.coordinator_layout); // You need to add this ID
		
		CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		);
		params.gravity = Gravity.BOTTOM | Gravity.END;
		params.setMargins(0, 0, 16, 150);
		rootView.addView(fabMenu, params);
		
		setupMenuClickListeners();
		
		fabMain.setOnClickListener(v -> {
			if (isFabMenuOpen) hideFabMenu();
			else showFabMenu();
		});
		
		setupOutsideClickListener();
	}
	
	private void setupMenuClickListeners() {
		fabMenu.findViewById(R.id.menu_add_lyric).setOnClickListener(v -> {
			startActivity(new Intent(this, AddEditActivity.class));
			hideFabMenu();
		});
		
		fabMenu.findViewById(R.id.menu_create_setlist).setOnClickListener(v -> {
			List<Lyric> allLyrics = dbHelper.getAllLyrics("title");
			if (allLyrics.isEmpty()) {
				Toast.makeText(this, "Create some lyrics first!", Toast.LENGTH_SHORT).show();
				} else {
				startActivity(new Intent(this, SetlistActivity.class));
			}
			hideFabMenu();
		});
		
		fabMenu.findViewById(R.id.menu_view_setlists).setOnClickListener(v -> {
			startActivity(new Intent(this, SetlistManagerActivity.class));
			hideFabMenu();
		});
		
		fabMenu.findViewById(R.id.menu_qr_scanner).setOnClickListener(v -> {
			startActivityForResult(new Intent(this, QRScannerActivity.class), QR_SCAN_REQUEST_CODE);
			hideFabMenu();
		});
		
		fabMenu.findViewById(R.id.menu_sort).setOnClickListener(v -> {
			sortByTitle = !sortByTitle;
			loadLyrics();
			Toast.makeText(this,
			sortByTitle ? "Sorted by Title" : "Sorted by Date",
			Toast.LENGTH_SHORT).show();
			hideFabMenu();
		});
		
		fabMenu.findViewById(R.id.menu_audio_player).setOnClickListener(v -> {
			openAudioPlayer();
			hideFabMenu();
		});
		
		fabMenu.findViewById(R.id.menu_restore_backup).setOnClickListener(v -> {
			if (dbHelper != null) {
				dbHelper.close();
			}
			restoreBackup();
			hideFabMenu();
		});
	}
	
	// Add this method to handle the QR scan result
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == QR_SCAN_REQUEST_CODE) {
			if (resultCode == RESULT_OK && data != null) {
				String qrData = data.getStringExtra("qr_data");
				if (qrData != null) {
					processScannedQRData(qrData);
				}
			}
		}
	}
	
	// Add this method to process the scanned QR data
	private void processScannedQRData(String qrData) {
		try {
			// Auto-detect payload type based on your workflow
			if (qrData.startsWith("PLANA|")) {
				processPlanA(qrData);
				} else if (qrData.startsWith("PLANB|")) {
				processPlanB(qrData);
				} else {
				// Fallback: try to detect type automatically
				if (qrData.contains("\n") && qrData.split("\n").length > 1) {
					processPlanB(qrData); // Likely a titles list
					} else {
					processPlanA(qrData); // Likely full lyrics
				}
			}
			} catch (Exception e) {
			Toast.makeText(this, "Error processing QR data: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	// Add this method to handle Plan A (full lyrics)
	private void processPlanA(String planAPayload) {
		try {
			// Parse the payload: PLANA|v1|<base64_compressed_bytes>
			String[] parts = planAPayload.split("\\|");
			if (parts.length >= 3) {
				String version = parts[1];
				String base64Data = parts[2];
				
				// Base64 decode
				byte[] compressedBytes = Base64.getDecoder().decode(base64Data);
				
				// Decompress using your existing method
				String decompressedLyrics = decompressWithInflater(compressedBytes);
				
				// Parse and save the lyrics
				saveImportedLyrics(decompressedLyrics);
				Toast.makeText(this, "Lyrics imported successfully!", Toast.LENGTH_SHORT).show();
			}
			} catch (Exception e) {
			Toast.makeText(this, "Failed to import lyrics: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	// Add this method to handle Plan B (titles only)
	private void processPlanB(String planBPayload) {
		try {
			// Parse the payload: PLANB|v1|<base64_compressed_bytes>
			String[] parts = planBPayload.split("\\|");
			if (parts.length >= 3) {
				String version = parts[1];
				String base64Data = parts[2];
				
				// Base64 decode
				byte[] compressedBytes = Base64.getDecoder().decode(base64Data);
				
				// Decompress
				String decompressedTitles = decompressWithInflater(compressedBytes);
				
				// Split into lines - first line is the setlist name, rest are song titles
				String[] lines = decompressedTitles.split("\n");
				
				if (lines.length == 0) {
					Toast.makeText(this, "Empty setlist data", Toast.LENGTH_SHORT).show();
					return;
				}
				
				String setName = lines[0].trim(); // First line is the setlist name
				List<Lyric> foundLyrics = new ArrayList<>();
				List<Lyric> allLyrics = dbHelper.getAllLyrics("title");
				
				// Start from index 1 to skip the setlist name
				for (int i = 1; i < lines.length; i++) {
					String searchTitle = lines[i].trim();
					if (searchTitle.isEmpty()) continue;
					
					for (Lyric lyric : allLyrics) {
						if (lyric.getTitle() != null && lyric.getTitle().equalsIgnoreCase(searchTitle)) {
							foundLyrics.add(lyric);
							break;
						}
					}
				}
				
				if (!foundLyrics.isEmpty()) {
					// Use the actual setlist name from the QR code
					createSetlistFromTitles(setName, foundLyrics);
					Toast.makeText(this, "Imported \"" + setName + "\" with " + foundLyrics.size() + " songs", Toast.LENGTH_SHORT).show();
					} else {
					Toast.makeText(this, "No matching lyrics found locally", Toast.LENGTH_SHORT).show();
				}
			}
			} catch (Exception e) {
			Toast.makeText(this, "Failed to import setlist: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	// Add these helper methods
	private void saveImportedLyrics(String lyricsContent) {
		// Parse the lyrics and save to database
		String title = extractTitleFromLyrics(lyricsContent);
		String content = lyricsContent;
		
		Lyric newLyric = new Lyric(title, content);
		dbHelper.insertLyric(newLyric); // ← CORRECT METHOD NAME
		loadLyrics(); // Refresh the list
	}
	
	private String extractTitleFromLyrics(String lyrics) {
		// Simple extraction - first line is usually the title
		if (lyrics.contains("\n")) {
			return lyrics.substring(0, lyrics.indexOf("\n")).trim();
		}
		return lyrics.length() > 20 ? lyrics.substring(0, 20) + "..." : lyrics;
	}
	
	private void createSetlistFromTitles(String setName, List<Lyric> lyrics) {
		List<Integer> lyricIds = new ArrayList<>();
		for (Lyric lyric : lyrics) {
			lyricIds.add(lyric.getId());
		}
		
		// Use the existing method that takes name and lyric IDs directly
		setlistDbHelper.createSetlist(setName, lyricIds);
	}
	
	private void restoreBackup() {
		new AlertDialog.Builder(this)
		.setTitle("Import Backup")
		.setMessage("Select backup source to import from")
		.setNegativeButton("Choose File", (d, w) ->
		pickBackupLauncher.launch(new String[]{"text/plain", "*/*"}))
		.setNeutralButton("Cancel", (d, w) -> {
			dbHelper = new LyricDbHelper(this);
			loadLyrics();
		})
		.show();
	}
	
	private void autoDetectAndRestore() {
		new Thread(() -> {
			List<File> backupFiles = new ArrayList<>();
			
			// Check both folders for backup files
			File appDir = getExternalFilesDir(null);
			if (appDir != null) {
				File lyricFolder = new File(appDir, "LyrEx/Lyrics");
				File backupFolder = new File(appDir, "LyrEx/Backups");
				
				addBackupFilesFromFolder(backupFiles, lyricFolder);
				addBackupFilesFromFolder(backupFiles, backupFolder);
			}
			
			runOnUiThread(() -> {
				if (backupFiles.isEmpty()) {
					Toast.makeText(this, "No backup found in app folders, please choose manually.", Toast.LENGTH_SHORT).show();
					pickBackupLauncher.launch(new String[]{"text/plain", "*/*"});
					} else {
					// Convert Files to URIs using FileProvider
					List<Uri> uris = new ArrayList<>();
					for (File file : backupFiles) {
						try {
							Uri uri = FileProvider.getUriForFile(
							this,
							"com.redlab.lyrex.fileprovider",
							file
							);
							uris.add(uri);
							} catch (Exception e) {
							Toast.makeText(this, "Error accessing: " + file.getName(), Toast.LENGTH_SHORT).show();
						}
					}
					
					if (!uris.isEmpty()) {
						processBackupFiles(uris);
						} else {
						pickBackupLauncher.launch(new String[]{"text/plain", "*/*"});
					}
				}
			});
		}).start();
	}
	
	private void addBackupFilesFromFolder(List<File> backupFiles, File folder) {
		if (folder.exists() && folder.isDirectory()) {
			File[] files = folder.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile() && isBackupFile(file) && file.canRead()) {
						backupFiles.add(file);
					}
				}
			}
		}
	}
	
	private Uri findBackupInLyricFolder() {
		File appDir = getExternalFilesDir(null);
		if (appDir == null) return null;
		
		File lyricFolder = new File(appDir, "LyrEx/Lyrics");
		return findLatestBackupFile(lyricFolder);
	}
	
	private Uri findBackupInBackupFolder() {
		File appDir = getExternalFilesDir(null);
		if (appDir == null) return null;
		
		File backupFolder = new File(appDir, "LyrEx/Backups");
		return findLatestBackupFile(backupFolder);
	}
	
	private Uri findLatestBackupFile(File folder) {
		if (!folder.exists() || !folder.isDirectory()) return null;
		
		File[] files = folder.listFiles();
		if (files == null) return null;
		
		File latestBackup = null;
		long latestModified = 0;
		
		for (File file : files) {
			if (file.isFile() && isBackupFile(file)) {
				if (file.lastModified() > latestModified) {
					latestModified = file.lastModified();
					latestBackup = file;
				}
			}
		}
		
		return latestBackup != null ? Uri.fromFile(latestBackup) : null;
	}
	
	private boolean isBackupFile(File file) {
		String fileName = file.getName().toLowerCase();
		return fileName.endsWith(".txt") || fileName.contains("backup") || fileName.contains("lyrex");
	}
	
	// Now this method accepts a list of URIs
	private void processBackupFiles(List<Uri> uris) {
		new Thread(() -> {
			List<Lyric> allParsedLyrics = new ArrayList<>();
			
			for (Uri uri : uris) {
				List<Lyric> parsedLyrics = parseBackupFileInMemory(uri);
				if (parsedLyrics != null) {
					allParsedLyrics.addAll(parsedLyrics);
				}
			}
			
			if (allParsedLyrics.isEmpty()) {
				runOnUiThread(() -> Toast.makeText(this, "Could not parse any lyrics from the selected files.", Toast.LENGTH_LONG).show());
				return;
			}
			
			runOnUiThread(() -> {
				new AlertDialog.Builder(this)
				.setTitle("Import Lyrics from Backup")
				.setMessage("Found " + allParsedLyrics.size() + " total lyrics in the selected files.")
				.setPositiveButton("Import All Songs", (dialog, which) -> restoreLyricsToDb(allParsedLyrics))
				.setNegativeButton("Select Specific Songs", (dialog, which) -> showLyricSelectionDialog(allParsedLyrics))
				.setNeutralButton("Cancel", null)
				.show();
			});
		}).start();
	}
	
	private List<Lyric> parseBackupFileInMemory(Uri uri) {
		List<Lyric> parsedLyrics = null;
		try {
			InputStream inputStream = getContentResolver().openInputStream(uri);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			String line;
			
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
			reader.close();
			inputStream.close();
			
			String backupContent = sb.toString();
			
			dbHelper = new LyricDbHelper(this); // Temporary helper to access parsing methods
			if (dbHelper.isNewFormatBackup(backupContent)) {
				parsedLyrics = dbHelper.parseNewFormat(backupContent);
				} else if (dbHelper.isOldFormatBackup(backupContent)) {
				parsedLyrics = dbHelper.parseOldFormat(backupContent);
				} else {
				parsedLyrics = dbHelper.parseGenericFormat(backupContent);
			}
			dbHelper.close(); // Close the temporary helper
			return parsedLyrics;
			
			} catch (Exception e) {
			e.printStackTrace();
			runOnUiThread(() -> Toast.makeText(this, "Error parsing backup: " + e.getMessage(), Toast.LENGTH_LONG).show());
			return null;
		}
	}
	
	private void showLyricSelectionDialog(List<Lyric> lyrics) {
		CharSequence[] titles = new CharSequence[lyrics.size()];
		boolean[] checkedItems = new boolean[lyrics.size()];
		for (int i = 0; i < lyrics.size(); i++) {
			titles[i] = lyrics.get(i).getTitle();
		}
		
		new AlertDialog.Builder(this)
		.setTitle("Select Lyrics to Restore")
		.setMultiChoiceItems(titles, checkedItems, (dialog, which, isChecked) -> {
			checkedItems[which] = isChecked;
		})
		.setPositiveButton("Restore Selected", (dialog, which) -> {
			List<Lyric> selectedLyrics = new ArrayList<>();
			for (int i = 0; i < checkedItems.length; i++) {
				if (checkedItems[i]) {
					selectedLyrics.add(lyrics.get(i));
				}
			}
			
			if (selectedLyrics.isEmpty()) {
				Toast.makeText(this, "No lyrics selected.", Toast.LENGTH_SHORT).show();
				} else {
				restoreLyricsToDb(selectedLyrics);
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	// In MainActivity.java, modify the restoreLyricsToDb method to handle the list of lyrics
	private void restoreLyricsToDb(List<Lyric> lyricsToRestore) {
		new Thread(() -> {
			LyricDbHelper.RestoreResult result = null;
			try {
				dbHelper = new LyricDbHelper(this); // Get a fresh connection
				result = dbHelper.addParsedLyrics(dbHelper.getWritableDatabase(), lyricsToRestore);
				} catch (Exception e) {
				e.printStackTrace();
				// Handle error on UI thread
				} finally {
				if (dbHelper != null) dbHelper.close();
				dbHelper = new LyricDbHelper(this); // Re-initialize
			}
			
			final LyricDbHelper.RestoreResult finalResult = result;
			runOnUiThread(() -> {
				if (finalResult != null && finalResult.parsed) {
					Toast.makeText(this, "Imported " + finalResult.addedCount + " new lyrics.", Toast.LENGTH_LONG).show();
					loadLyrics();
					} else {
					Toast.makeText(this, "Import failed: " + finalResult.errorMessage, Toast.LENGTH_LONG).show();
				}
			});
		}).start();
	}
	
	private String getFileName(Uri uri) {
		if ("file".equals(uri.getScheme())) {
			return new File(uri.getPath()).getName();
		}
		
		try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
				if (nameIndex != -1) {
					return cursor.getString(nameIndex);
				}
			}
			} catch (Exception e) {
			// Fall through
		}
		
		return "backup file";
	}
	
	private boolean isNewFormatBackup(String content) {
		return content.contains("=== LYREX BACKUP ===") &&
		content.contains("Title:") &&
		content.contains("Backup Date:");
	}
	
	private boolean isOldFormatBackup(String content) {
		return content.contains("╔") &&
		content.contains("║") &&
		content.contains("╚") &&
		content.contains("LYREX EXPORT");
	}
	
	private void setupOutsideClickListener() {
		findViewById(android.R.id.content).setOnTouchListener((v, event) -> {
			if (isFabMenuOpen && event.getAction() == MotionEvent.ACTION_DOWN) {
				hideFabMenu();
				return true;
			}
			return false;
		});
	}
	
	private void showFabMenu() {
		isFabMenuOpen = true;
		fabMenu.setVisibility(View.VISIBLE);
		fabMenu.animate().alpha(1f).setDuration(200).start();
	}
	
	private void hideFabMenu() {
		isFabMenuOpen = false;
		fabMenu.animate().alpha(0f).setDuration(200).withEndAction(() ->
		fabMenu.setVisibility(View.GONE)).start();
	}
	
	private void openAudioPlayer() {
		List<Lyric> lyrics = dbHelper.getAllLyrics("title");
		if (lyrics.isEmpty()) {
			Toast.makeText(this, "Create some lyrics to practice with!", Toast.LENGTH_SHORT).show();
			return;
		}
		startActivity(new Intent(this, AudioPlayerActivity.class));
	}
	
	private void loadLyrics() {
		List<Lyric> lyrics = dbHelper.getAllLyrics(sortByTitle ? "title" : "date");
		adapter.updateData(lyrics);
		showEmptyState(lyrics.isEmpty());
	}
	
	private void showEmptyState(boolean show) {
		TextView tvEmptyState = findViewById(R.id.tv_empty_state);
		tvEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
	}
	
	private void showLyricOptions(Lyric lyric) {
		CharSequence[] options = {"Edit", "Perform", "Add to Setlist", "Delete", "Share"};
		
		new AlertDialog.Builder(this)
		.setTitle(lyric.getTitle())
		.setItems(options, (dialog, which) -> handleLyricOptionSelection(which, lyric))
		.show();
	}
	
	private void handleLyricOptionSelection(int option, Lyric lyric) {
		switch (option) {
			case 0: editLyric(lyric); break;
			case 1: performLyric(lyric); break;
			case 2: addToSetlist(lyric); break;
			case 3: deleteLyric(lyric); break;
			case 4: shareLyricsOption(lyric); break;
		}
	}
	
	private void shareLyricsOption(Lyric lyric) {
		// This will directly share the lyric passed to the method
		shareLyricsAsQr(Collections.singletonList(lyric));
	}
	
	private void shareLyricsAsQr(List<Lyric> lyrics) {
		try {
			if (lyrics == null || lyrics.isEmpty()) return;
			
			StringBuilder sb = new StringBuilder();
			for (Lyric l : lyrics) {
				String title = (l.getTitle() != null) ? l.getTitle() : "Untitled";
				String content = (l.getContent() != null) ? l.getContent() : "";
				
				sb.append(title).append("\n")
				.append(content).append("\n")
				.append("---\n");
			}
			String rawData = sb.toString();
			
			// Determine which plan to use based on size
			if (rawData.getBytes(StandardCharsets.UTF_8).length > 2000) {
				List<String> titles = new ArrayList<>();
				for (Lyric l : lyrics) {
					titles.add(l.getTitle() != null ? l.getTitle() : "Untitled");
				}
				String titlesString = String.join("\n", titles);
				generateQr(titlesString, "PLANB");
				} else {
				generateQr(rawData, "PLANA");
			}
			} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void generateQr(String rawData, String plan) {
		new Thread(() -> {
			String payload = "";
			try {
				// Use Deflater compression instead of Zstd
				byte[] compressedBytes = compressWithDeflater(rawData);
				String base64Payload = Base64.getEncoder().encodeToString(compressedBytes);
				payload = plan + "|v1|" + base64Payload;
				
				// Run QR code generation on the main thread
				final String finalPayload = payload;
				runOnUiThread(() -> {
					try {
						MultiFormatWriter writer = new MultiFormatWriter();
						BitMatrix bitMatrix = writer.encode(
						finalPayload, BarcodeFormat.QR_CODE, 800, 800);
						
						int width = bitMatrix.getWidth();
						int height = bitMatrix.getHeight();
						Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
						
						for (int x = 0; x < width; x++) {
							for (int y = 0; y < height; y++) {
								bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
							}
						}
						
						ImageView imageView = new ImageView(MainActivity.this);
						imageView.setImageBitmap(bmp);
						new AlertDialog.Builder(MainActivity.this)
						.setTitle("Share QR Code")
						.setView(imageView)
						.setPositiveButton("Share", (d, w) -> shareBitmap(bmp))
						.setNegativeButton("Close", null)
						.show();
						
						} catch (Exception e) {
						Toast.makeText(MainActivity.this, "QR generation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
					}
				});
				
				} catch (Exception e) {
				runOnUiThread(() -> Toast.makeText(MainActivity.this, "Compression or encoding failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
			}
		}).start();
	}
	
	// Replace Zstd compression with Deflater compression
	private byte[] compressWithDeflater(String text) {
		try {
			byte[] input = text.getBytes(StandardCharsets.UTF_8);
			Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
			deflater.setInput(input);
			deflater.finish();
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
			byte[] buffer = new byte[1024];
			while (!deflater.finished()) {
				int count = deflater.deflate(buffer);
				bos.write(buffer, 0, count);
			}
			bos.close();
			deflater.end();
			
			return bos.toByteArray();
			} catch (Exception e) {
			e.printStackTrace();
			return text.getBytes(StandardCharsets.UTF_8); // Fallback to uncompressed
		}
	}
	
	// You'll also need this method to decompress when reading QR codes
	public static String decompressWithInflater(byte[] compressed) {
		try {
			Inflater inflater = new Inflater();
			inflater.setInput(compressed);
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream(compressed.length * 2);
			byte[] buffer = new byte[1024];
			while (!inflater.finished()) {
				int count = inflater.inflate(buffer);
				bos.write(buffer, 0, count);
			}
			bos.close();
			inflater.end();
			
			return bos.toString("UTF-8");
			} catch (Exception e) {
			e.printStackTrace();
			return new String(compressed, StandardCharsets.UTF_8); // Fallback to uncompressed
		}
	}
	
	private void shareBitmap(Bitmap bmp) {
		try {
			File cachePath = new File(getCacheDir(), "images");
			cachePath.mkdirs();
			File file = new File(cachePath, "qr.png");
			FileOutputStream stream = new FileOutputStream(file);
			bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
			stream.close();
			
			Uri contentUri = FileProvider.getUriForFile(
			this,
			"com.redlab.lyrex.fileprovider",
			file
			);
			
			if (contentUri != null) {
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("image/png");
				shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
				shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
			}
			} catch (Exception e) {
			Toast.makeText(this, "Failed to share QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
	
	private void editLyric(Lyric lyric) {
		Intent intent = new Intent(this, AddEditActivity.class);
		intent.putExtra("lyric_id", lyric.getId());
		startActivity(intent);
	}
	
	private void performLyric(Lyric lyric) {
		Intent intent = new Intent(this, PerformanceModeActivity.class);
		
		ArrayList<Integer> lyricIds = new ArrayList<>();
		lyricIds.add(lyric.getId());
		
		intent.putIntegerArrayListExtra("lyric_ids", lyricIds);
		intent.putExtra("setlist_name", lyric.getTitle());
		
		startActivity(intent);
	}
	
	private void addToSetlist(Lyric lyric) {
		List<Setlist> setlists = setlistDbHelper.getAllSetlists();
		
		if (setlists.isEmpty()) {
			Toast.makeText(this, "Create a setlist first!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		showSetlistSelectionDialog(lyric, setlists);
	}
	
	private void deleteLyric(Lyric lyric) {
		new AlertDialog.Builder(this)
		.setTitle("Delete Lyric")
		.setMessage("Are you sure you want to delete \"" + lyric.getTitle() + "\"?")
		.setPositiveButton("Delete", (d, w) -> {
			dbHelper.deleteLyric(lyric.getId());
			loadLyrics();
			Toast.makeText(this, "Lyric deleted", Toast.LENGTH_SHORT).show();
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void showSetlistSelectionDialog(Lyric lyric, List<Setlist> setlists) {
		CharSequence[] setlistNames = new CharSequence[setlists.size()];
		for (int i = 0; i < setlists.size(); i++) {
			setlistNames[i] = setlists.get(i).getName();
		}
		
		new AlertDialog.Builder(this)
		.setTitle("Add to Setlist")
		.setItems(setlistNames, (dialog, which) ->
		addLyricToSetlist(lyric, setlists.get(which)))
		.show();
	}
	
	private void addLyricToSetlist(Lyric lyric, Setlist setlist) {
		List<Integer> currentLyricIds = setlist.getLyricIds();
		if (currentLyricIds.contains(lyric.getId())) {
			Toast.makeText(this, "Song already in setlist", Toast.LENGTH_SHORT).show();
			return;
		}
		currentLyricIds.add(lyric.getId());
		boolean success = setlistDbHelper.updateSetlistLyrics(setlist.getId(), currentLyricIds);
		Toast.makeText(this, success ? "Added to " + setlist.getName() : "Failed to add to setlist", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		loadLyrics();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.sort_menu) {
			sortByTitle = !sortByTitle;
			loadLyrics();
			Toast.makeText(this,
			sortByTitle ? "Sorted by Title" : "Sorted by Date",
			Toast.LENGTH_SHORT).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (dbHelper != null) dbHelper.close();
		if (setlistDbHelper != null) setlistDbHelper.close();
		if (fabMenu != null && fabMenu.getParent() != null) {
			((ViewGroup) fabMenu.getParent()).removeView(fabMenu);
		}
	}
}