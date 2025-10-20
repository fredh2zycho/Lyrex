package com.redlab.lyrex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class SetlistViewerActivity extends AppCompatActivity {
	private Setlist currentSetlist;
	private List<Lyric> setlistLyrics;
	private LyricDbHelper lyricDbHelper;
	private SetlistDbHelper setlistDbHelper;
	
	// Corrected variable names to match XML
	private TextView tvSetlistName;
	private ListView lvSetlistLyrics;
	private Button btnStartPerformance, btnShareQR;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setlist_viewer);
		
		lyricDbHelper = new LyricDbHelper(this);
		setlistDbHelper = new SetlistDbHelper(this);
		
		// Corrected findViewById calls
		tvSetlistName = findViewById(R.id.tv_setlist_name);
		lvSetlistLyrics = findViewById(R.id.lv_setlist_lyrics);
		btnStartPerformance = findViewById(R.id.btn_start_performance);
		btnShareQR = findViewById(R.id.btn_share_qr);
		
		initializeViews();
		loadSetlistData();
	}
	
	private void initializeViews() {
		btnStartPerformance.setOnClickListener(v -> startPerformanceMode());
		btnShareQR.setOnClickListener(v -> shareSetlistAsQR());
	}
	
	private void loadSetlistData() {
		Intent intent = getIntent();
		if (intent.hasExtra("setlist_id")) {
			int setlistId = intent.getIntExtra("setlist_id", -1);
			if (setlistId != -1) {
				loadSetlistById(setlistId);
				} else {
				Toast.makeText(this, "Invalid setlist ID", Toast.LENGTH_SHORT).show();
				finish();
			}
			} else if (intent.hasExtra("lyric_ids")) {
			ArrayList<Integer> lyricIds = intent.getIntegerArrayListExtra("lyric_ids");
			String setName = intent.getStringExtra("setlist_name");
			if (lyricIds != null && !lyricIds.isEmpty()) {
				loadLyricsFromIds(lyricIds, setName);
				} else {
				Toast.makeText(this, "No songs selected", Toast.LENGTH_SHORT).show();
				finish();
			}
			} else {
			Toast.makeText(this, "No setlist data provided", Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	
	private void loadSetlistById(int setlistId) {
		currentSetlist = setlistDbHelper.getSetlistById(setlistId);
		
		if (currentSetlist != null) {
			tvSetlistName.setText(currentSetlist.getName());
			
			setlistLyrics = new ArrayList<>();
			List<Integer> lyricIds = currentSetlist.getLyricIds();
			
			if (lyricIds != null && !lyricIds.isEmpty()) {
				for (Integer id : lyricIds) {
					Lyric lyric = lyricDbHelper.getLyricById(id);
					if (lyric != null) {
						setlistLyrics.add(lyric);
					}
				}
			}
			
			// Update song count if you have that TextView
			TextView tvSongCount = findViewById(R.id.tv_song_count);
			if (tvSongCount != null) {
				tvSongCount.setText(setlistLyrics.size() + " songs");
			}
			
			setupListView();
			} else {
			Toast.makeText(this, "Setlist not found", Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	
	private void loadLyricsFromIds(List<Integer> lyricIds, String setName) {
		tvSetlistName.setText(setName != null ? setName : "Imported Setlist");
		
		setlistLyrics = new ArrayList<>();
		for (Integer id : lyricIds) {
			Lyric lyric = lyricDbHelper.getLyricById(id);
			if (lyric != null) {
				setlistLyrics.add(lyric);
			}
		}
		
		// Update song count
		TextView tvSongCount = findViewById(R.id.tv_song_count);
		if (tvSongCount != null) {
			tvSongCount.setText(setlistLyrics.size() + " songs");
		}
		
		setupListView();
	}
	
	private void setupListView() {
		if (setlistLyrics == null || setlistLyrics.isEmpty()) {
			findViewById(R.id.tv_empty_state).setVisibility(View.VISIBLE);
			lvSetlistLyrics.setVisibility(View.GONE);
			
			// Disable buttons if no songs
			btnStartPerformance.setEnabled(false);
			btnShareQR.setEnabled(false);
			return;
		}
		
		findViewById(R.id.tv_empty_state).setVisibility(View.GONE);
		lvSetlistLyrics.setVisibility(View.VISIBLE);
		
		// Enable buttons since we have songs
		btnStartPerformance.setEnabled(true);
		btnShareQR.setEnabled(true);
		
		List<String> lyricTitles = new ArrayList<>();
		for (Lyric lyric : setlistLyrics) {
			lyricTitles.add(lyric.getTitle());
		}
		
		ArrayAdapter<String> adapter = new ArrayAdapter<>(
		this,
		android.R.layout.simple_list_item_1,
		lyricTitles
		);
		lvSetlistLyrics.setAdapter(adapter);
		
		lvSetlistLyrics.setOnItemClickListener((parent, view, position, id) -> {
			Lyric selectedLyric = setlistLyrics.get(position);
			Intent intent = new Intent(SetlistViewerActivity.this, AddEditActivity.class);
			intent.putExtra("lyric_id", selectedLyric.getId());
			startActivity(intent);
		});
	}
	
	private void startPerformanceMode() {
		if (setlistLyrics == null || setlistLyrics.isEmpty()) {
			Toast.makeText(this, "No songs in setlist", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Intent intent = new Intent(this, PerformanceModeActivity.class);
		intent.putIntegerArrayListExtra("lyric_ids", getLyricIds());
		intent.putExtra("setlist_name", tvSetlistName.getText().toString());
		startActivity(intent);
	}
	
	private void shareSetlistAsQR() {
		if (setlistLyrics == null || setlistLyrics.isEmpty()) {
			Toast.makeText(this, "No songs in setlist", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Intent intent = new Intent(this, QRShareActivity.class);
		intent.putIntegerArrayListExtra("lyric_ids", getLyricIds());
		intent.putExtra("setlist_name", tvSetlistName.getText().toString());
		startActivity(intent);
	}
	
	private ArrayList<Integer> getLyricIds() {
		ArrayList<Integer> ids = new ArrayList<>();
		if (setlistLyrics != null) {
			for (Lyric lyric : setlistLyrics) {
				ids.add(lyric.getId());
			}
		}
		return ids;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// Refresh data if lyrics were edited
		if (currentSetlist != null) {
			// Reload the setlist to get any updates
			loadSetlistById(currentSetlist.getId());
			} else if (setlistLyrics != null && !setlistLyrics.isEmpty()) {
			// For direct lyric IDs, just refresh the list
			setupListView();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (lyricDbHelper != null) {
			lyricDbHelper.close();
		}
		if (setlistDbHelper != null) {
			setlistDbHelper.close();
		}
	}
}