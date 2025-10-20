package com.redlab.lyrex;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.text.method.ScrollingMovementMethod;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest; 
import android.content.pm.PackageManager; 

public class AddEditActivity extends AppCompatActivity {
	private EditText titleEditText, contentEditText;
	private TextView previewTextView;
	private Button previewButton, editButton;
	private LyricDbHelper dbHelper;
	private int lyricId = -1;
	private boolean isPreviewMode = false;
	private Lyric currentLyric;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_edit);
		
		initializeViews();
		setupListeners();
		
		dbHelper = new LyricDbHelper(this);
		
		// Check if editing existing lyric
		if (getIntent().hasExtra("lyric_id")) {
			lyricId = getIntent().getIntExtra("lyric_id", -1);
			loadLyricData(lyricId);
		}
	}
	
	private void initializeViews() {
		titleEditText = findViewById(R.id.titleEditText);
		contentEditText = findViewById(R.id.contentEditText);
		previewTextView = findViewById(R.id.previewTextView);
		previewButton = findViewById(R.id.previewButton);
		editButton = findViewById(R.id.editButton);
		
		// Set up preview TextView
		if (previewTextView != null) {
			previewTextView.setMovementMethod(new ScrollingMovementMethod());
			previewTextView.setVisibility(View.GONE);
		}
		
		// Initially hide edit button
		if (editButton != null) {
			editButton.setVisibility(View.GONE);
		}
	}
	
	private void setupListeners() {
		// Auto-highlight as user types
		if (contentEditText != null) {
			contentEditText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {}
				
				@Override
				public void afterTextChanged(Editable s) {
					// Auto-highlight after a short delay to avoid constant highlighting
					contentEditText.removeCallbacks(highlightRunnable);
					contentEditText.postDelayed(highlightRunnable, 500);
				}
			});
		}
		
		// Preview button listener
		if (previewButton != null) {
			previewButton.setOnClickListener(v -> togglePreviewMode());
		}
		
		// Edit button listener
		if (editButton != null) {
			editButton.setOnClickListener(v -> togglePreviewMode());
		}
	}
	
	private final Runnable highlightRunnable = new Runnable() {
		@Override
		public void run() {
			applyHighlightingToEditText();
		}
	};
	
	private void applyHighlightingToEditText() {
		if (contentEditText == null) return;
		
		String content = contentEditText.getText().toString();
		if (content.trim().isEmpty()) return;
		
		// Create a temporary lyric object for highlighting
		Lyric tempLyric = new Lyric("", content);
		tempLyric.highlightSections();
		
		if (tempLyric.getStyledContent() != null) {
			// Save cursor position
			int cursorPos = contentEditText.getSelectionStart();
			
			// Apply highlighting
			contentEditText.setText(tempLyric.getStyledContent());
			
			// Restore cursor position
			if (cursorPos <= contentEditText.getText().length()) {
				contentEditText.setSelection(cursorPos);
			}
		}
	}
	
	private void loadLyricData(int id) {
		currentLyric = dbHelper.getLyricById(id);
		if (currentLyric != null) {
			titleEditText.setText(currentLyric.getTitle());
			contentEditText.setText(currentLyric.getContent());
			
			// Apply highlighting
			applyHighlightingToEditText();
			
			// Set activity title
			setTitle("Edit Lyric");
		}
	}
	
	private void togglePreviewMode() {
		if (isPreviewMode) {
			// Switch to edit mode
			showEditMode();
			} else {
			// Switch to preview mode
			showPreviewMode();
		}
		isPreviewMode = !isPreviewMode;
	}
	
	private void showPreviewMode() {
		if (contentEditText == null || previewTextView == null) return;
		
		String content = contentEditText.getText().toString();
		if (content.trim().isEmpty()) {
			Toast.makeText(this, "No content to preview", Toast.LENGTH_SHORT).show();
			return;
		}
		
		// Create lyric with current content for highlighting
		Lyric previewLyric = new Lyric("", content);
		previewLyric.highlightSections();
		
		// Show highlighted content in preview
		if (previewLyric.getStyledContent() != null) {
			previewTextView.setText(previewLyric.getStyledContent());
			} else {
			previewTextView.setText(content);
		}
		
		// Toggle visibility
		contentEditText.setVisibility(View.GONE);
		previewTextView.setVisibility(View.VISIBLE);
		
		if (previewButton != null) previewButton.setVisibility(View.GONE);
		if (editButton != null) editButton.setVisibility(View.VISIBLE);
	}
	
	private void showEditMode() {
		// Toggle visibility back
		if (contentEditText != null) contentEditText.setVisibility(View.VISIBLE);
		if (previewTextView != null) previewTextView.setVisibility(View.GONE);
		
		if (previewButton != null) previewButton.setVisibility(View.VISIBLE);
		if (editButton != null) editButton.setVisibility(View.GONE);
		
		// Reapply highlighting to edit text
		applyHighlightingToEditText();
	}
	
	public void saveLyric(View view) {
		String title = titleEditText.getText().toString().trim();
		String content = contentEditText.getText().toString().trim();
		
		if (title.isEmpty() || content.isEmpty()) {
			Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
			return;
		}
		
		try {
			if (lyricId == -1) {
				// New lyric
				Lyric newLyric = new Lyric(title, content);
				newLyric.detectSectionType(); // Auto-detect sections and apply highlighting
				long result = dbHelper.insertLyric(newLyric);
				
				if (result > 0) {
					Toast.makeText(this, "Lyric saved successfully", Toast.LENGTH_SHORT).show();
					} else {
					Toast.makeText(this, "Failed to save lyric", Toast.LENGTH_SHORT).show();
					return;
				}
				} else {
				// Update existing
				if (currentLyric == null) {
					currentLyric = dbHelper.getLyricById(lyricId);
				}
				
				if (currentLyric != null) {
					currentLyric.setTitle(title);
					currentLyric.setContent(content);
					currentLyric.setTimestamp(System.currentTimeMillis());
					currentLyric.detectSectionType(); // Re-detect sections on update
					
					int result = dbHelper.updateLyric(currentLyric);
					if (result > 0) {
						Toast.makeText(this, "Lyric updated successfully", Toast.LENGTH_SHORT).show();
						} else {
						Toast.makeText(this, "Failed to update lyric", Toast.LENGTH_SHORT).show();
						return;
					}
				}
			}
			} catch (Exception e) {
			Toast.makeText(this, "Error saving lyric: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			return;
		}
		
		finish();
	}
	
	public void previewLyric(View view) {
		togglePreviewMode();
	}
	
	// Optional: Helper method for section selection UI
	private int getSectionIndex(String sectionType) {
		String[] sections = {"verse", "pre-chorus", "chorus", "bridge", "refrain"};
		for (int i = 0; i < sections.length; i++) {
			if (sections[i].equals(sectionType)) {
				return i;
			}
		}
		return 0; // default to verse
	}
	
	public void clearContent(View view) {
		// Show confirmation dialog
		new androidx.appcompat.app.AlertDialog.Builder(this)
		.setTitle("Clear Content")
		.setMessage("Are you sure you want to clear all content? This action cannot be undone.")
		.setPositiveButton("Clear", (dialog, which) -> {
			titleEditText.setText("");
			contentEditText.setText("");
			if (isPreviewMode) {
				showEditMode(); // Switch back to edit mode
			}
			Toast.makeText(this, "Content cleared", Toast.LENGTH_SHORT).show();
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	public void exportLyric(View view) {
		String title = titleEditText.getText().toString().trim();
		String content = contentEditText.getText().toString().trim();
		
		if (title.isEmpty() || content.isEmpty()) {
			Toast.makeText(this, "No content to export", Toast.LENGTH_SHORT).show();
			return;
		}
		
		// Check permission first
		if (!checkStoragePermission()) {
			return;
		}
		
		Lyric tempLyric = new Lyric(title, content);
		String filePath = tempLyric.exportToTxtFile(this);
		
		if (filePath != null) {
			Toast.makeText(this, "✅ Exported successfully!\n📁 " + filePath, Toast.LENGTH_LONG).show();
			} else {
			Toast.makeText(this, "❌ Export failed. Check storage permissions.", Toast.LENGTH_LONG).show();
		}
	}
	
	public void performanceMode(View view) {
		String title = titleEditText.getText().toString().trim();
		String content = contentEditText.getText().toString().trim();
		
		if (title.isEmpty() || content.isEmpty()) {
			Toast.makeText(this, "Please add content before entering Performance Mode", Toast.LENGTH_SHORT).show();
			return;
		}
		
		// Save first if this is a new lyric or has changes
		if (lyricId == -1) {
			// Save new lyric first
			saveLyric(view);
			return; // saveLyric will finish() the activity, so we can't continue
			} else {
			// Update existing lyric
			saveLyricSilently(); // Save without finishing activity
			
			// Now open performance mode
			Intent intent = new Intent(this, PerformanceModeActivity.class);
			intent.putExtra("lyric_id", lyricId);
			startActivity(intent);
		}
	}
	
	private void saveLyricSilently() {
		String title = titleEditText.getText().toString().trim();
		String content = contentEditText.getText().toString().trim();
		
		if (currentLyric != null) {
			currentLyric.setTitle(title);
			currentLyric.setContent(content);
			currentLyric.setTimestamp(System.currentTimeMillis());
			currentLyric.detectSectionType();
			dbHelper.updateLyric(currentLyric);
		}
	}
	
	private boolean checkStoragePermission() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
			!= android.content.pm.PackageManager.PERMISSION_GRANTED) {
				
				requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1) {
			if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(this, "Permission granted! Try export again.", Toast.LENGTH_SHORT).show();
				} else {
				Toast.makeText(this, "Permission denied. Export will save to app folder.", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private boolean hasUnsavedChanges() {
		// Check if content has changed from original
		if (currentLyric != null) {
			return !titleEditText.getText().toString().equals(currentLyric.getTitle()) ||
			!contentEditText.getText().toString().equals(currentLyric.getContent());
		}
		return !titleEditText.getText().toString().trim().isEmpty() ||
		!contentEditText.getText().toString().trim().isEmpty();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Remove any pending callbacks
		if (contentEditText != null) {
			contentEditText.removeCallbacks(highlightRunnable);
		}
	}
}