package com.redlab.lyrex;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PerformanceModeActivity extends AppCompatActivity {
	private LyricDbHelper lyricDbHelper;
	private List<Lyric> performanceLyrics;
	private RecyclerView recyclerView;
	private PerformanceAdapter adapter;
	private TextView tvSetlistName, tvCurrentSong, tvEmptyState;
	
	// Auto-scroll variables
	private Timer autoScrollTimer;
	private int scrollSpeed = 3;
	private boolean isAutoScrolling = true;
	
	// Pinch-to-zoom variables
	private ScaleGestureDetector scaleGestureDetector;
	private float fontSize = 16f;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_performance_mode);
		
		lyricDbHelper = new LyricDbHelper(this);
		recyclerView = findViewById(R.id.performance_recycler_view);
		tvSetlistName = findViewById(R.id.tv_performance_title);
		tvEmptyState = findViewById(R.id.tv_empty_state);
		
		// Setup pinch-to-zoom
		scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
		
		// Setup touch listener
		recyclerView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				scaleGestureDetector.onTouchEvent(event);
				return false;
			}
		});
		
		// In onCreate(), after setting up other views
		ImageButton btnSettings = findViewById(R.id.btn_settings);
		btnSettings.setOnClickListener(v -> showSettingsDialog());
		
		// Get the setlist data from intent
		Intent intent = getIntent();
		ArrayList<Integer> lyricIds = intent.getIntegerArrayListExtra("lyric_ids");
		String setlistName = intent.getStringExtra("setlist_name");
		
		if (setlistName != null) {
			tvSetlistName.setText(setlistName);
			} else {
			tvSetlistName.setText("Performance Mode");
		}
		
		if (lyricIds != null && !lyricIds.isEmpty()) {
			loadPerformanceLyrics(lyricIds);
			} else {
			showEmptyState("No songs available");
			Toast.makeText(this, "No lyrics found", Toast.LENGTH_SHORT).show();
		}
		
		// Start auto-scroll by default
		startAutoScroll(2000);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.performance_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		
		if (id == R.id.menu_settings) {
			showSettingsDialog();
			return true;
			} else if (id == R.id.menu_scroll_toggle) {
			toggleAutoScroll();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void showSettingsDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View dialogView = getLayoutInflater().inflate(R.layout.dialog_performance_settings, null);
		builder.setView(dialogView);
		
		// Setup font size seekbar
		SeekBar seekbarFontSize = dialogView.findViewById(R.id.seekbar_font_size);
		TextView tvFontSize = dialogView.findViewById(R.id.tv_font_size);
		seekbarFontSize.setProgress((int) fontSize - 12); // Convert 12-48sp to 0-36
		tvFontSize.setText((int) fontSize + "sp");
		
		seekbarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				fontSize = progress + 12; // Convert back to 12-48sp
				tvFontSize.setText((int) fontSize + "sp");
				updateFontSize();
			}
			
			@Override public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		
		// Setup scroll speed seekbar
		SeekBar seekbarScrollSpeed = dialogView.findViewById(R.id.seekbar_scroll_speed);
		TextView tvScrollSpeed = dialogView.findViewById(R.id.tv_scroll_speed);
		seekbarScrollSpeed.setProgress(scrollSpeed - 1); // Convert 1-20 to 0-19
		tvScrollSpeed.setText("Speed: " + scrollSpeed);
		
		seekbarScrollSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				scrollSpeed = progress + 3; // Convert back to 1-20
				tvScrollSpeed.setText("Speed: " + scrollSpeed);
				if (isAutoScrolling) {
					stopAutoScroll();
					startAutoScroll(0);
				}
			}
			
			@Override public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		
		// Setup auto-scroll switch
		SwitchCompat switchAutoScroll = dialogView.findViewById(R.id.switch_auto_scroll);
		switchAutoScroll.setChecked(isAutoScrolling);
		switchAutoScroll.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (isChecked) {
				startAutoScroll(0);
				} else {
				stopAutoScroll();
			}
		});
		
		builder.setTitle("Performance Settings");
		builder.setPositiveButton("OK", null); // Just close the dialog
		
		AlertDialog dialog = builder.create();
		dialog.show();
		
		// Allow tapping outside to dismiss
		dialog.setCanceledOnTouchOutside(true);
	}
	
	private void toggleAutoScroll() {
		if (isAutoScrolling) {
			stopAutoScroll();
			} else {
			startAutoScroll(0);
		}
	}
	
	private void loadPerformanceLyrics(ArrayList<Integer> lyricIds) {
		performanceLyrics = new ArrayList<>();
		
		for (Integer id : lyricIds) {
			Lyric lyric = lyricDbHelper.getLyricById(id);
			if (lyric != null) {
				performanceLyrics.add(lyric);
			}
		}
		
		if (performanceLyrics.isEmpty()) {
			showEmptyState("No lyrics found");
			return;
		}
		
		// Hide empty state and show lyrics
		tvEmptyState.setVisibility(View.GONE);
		recyclerView.setVisibility(View.VISIBLE);
		
		// Setup RecyclerView - FIXED THIS LINE
		// Get screen density for proper sizing
		float density = getResources().getDisplayMetrics().density;
		adapter = new PerformanceAdapter(this, performanceLyrics, density);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setAdapter(adapter);
	}
	
	private void startAutoScroll(long delay) {
		stopAutoScroll();
		
		autoScrollTimer = new Timer();
		autoScrollTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(() -> {
					if (recyclerView.canScrollVertically(1)) {
						recyclerView.smoothScrollBy(3, scrollSpeed);
					}
				});
			}
		}, delay, 30);
		isAutoScrolling = true;
		Toast.makeText(this, "Auto-scroll started", Toast.LENGTH_SHORT).show();
	}
	
	private void stopAutoScroll() {
		if (autoScrollTimer != null) {
			autoScrollTimer.cancel();
			autoScrollTimer.purge();
			autoScrollTimer = null;
		}
		isAutoScrolling = false;
		Toast.makeText(this, "Auto-scroll stopped", Toast.LENGTH_SHORT).show();
	}
	
	private void updateFontSize() {
		if (adapter != null) {
			adapter.setFontSize(fontSize);
			adapter.notifyDataSetChanged();
		}
	}
	
	// Pinch-to-zoom gesture detector
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			fontSize *= detector.getScaleFactor();
			fontSize = Math.max(12f, Math.min(fontSize, 48f));
			updateFontSize();
			return true;
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		scaleGestureDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}
	
	private void showEmptyState(String message) {
		tvEmptyState.setText(message);
		tvEmptyState.setVisibility(View.VISIBLE);
		recyclerView.setVisibility(View.GONE);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopAutoScroll();
		if (lyricDbHelper != null) {
			lyricDbHelper.close();
		}
	}
}