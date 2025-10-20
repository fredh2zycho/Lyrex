package com.redlab.lyrex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class SetlistActivity extends AppCompatActivity {
	private SetlistDbHelper setlistDbHelper;
	private LyricDbHelper lyricDbHelper;
	private ListView setlistListView;
	private List<Lyric> allLyrics = new ArrayList<>();
	private List<Integer> selectedLyricIds = new ArrayList<>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setlist);
		
		setlistDbHelper = new SetlistDbHelper(this);
		lyricDbHelper = new LyricDbHelper(this);
		
		Button btnCreateSetlist = findViewById(R.id.btnCreateSetlist);
		ExtendedFloatingActionButton fabShareQR = findViewById(R.id.fabShareQR);
		EditText etSetlistName = findViewById(R.id.etSetlistName);
		setlistListView = findViewById(R.id.setlistListView);
		
		// Load all lyrics for selection
		allLyrics = lyricDbHelper.getAllLyrics("title");
		setupListView();
		
		btnCreateSetlist.setOnClickListener(v -> {
			String setName = etSetlistName.getText().toString().trim();
			
			if (setName.isEmpty()) {
				Toast.makeText(this, "Please enter a setlist name", Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (selectedLyricIds.isEmpty()) {
				Toast.makeText(this, "Please select at least one song", Toast.LENGTH_SHORT).show();
				return;
			}
			
			setlistDbHelper.createSetlist(setName, selectedLyricIds);
			Toast.makeText(this, "Setlist created successfully", Toast.LENGTH_SHORT).show();
			finish();
		});
		
		fabShareQR.setOnClickListener(v -> {
			if (selectedLyricIds.isEmpty()) {
				Toast.makeText(this, "Please select songs to share", Toast.LENGTH_SHORT).show();
				return;
			}
			
			String setName = etSetlistName.getText().toString().trim();
			if (setName.isEmpty()) {
				setName = "My Setlist"; // Default name
			}
			
			Intent intent = new Intent(this, QRShareActivity.class);
			intent.putIntegerArrayListExtra("lyric_ids", new ArrayList<>(selectedLyricIds));
			intent.putExtra("setlist_name", setName);
			startActivity(intent);
		});
	}
	
	private void setupListView() {
		List<String> lyricTitles = new ArrayList<>();
		for (Lyric lyric : allLyrics) {
			lyricTitles.add(lyric.getTitle());
		}
		
		ArrayAdapter<String> adapter = new ArrayAdapter<>(
		this,
		android.R.layout.simple_list_item_multiple_choice,
		lyricTitles
		);
		
		setlistListView.setAdapter(adapter);
		setlistListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		setlistListView.setOnItemClickListener((parent, view, position, id) -> {
			// Update selected lyric IDs based on checkbox state
			if (setlistListView.isItemChecked(position)) {
				selectedLyricIds.add(allLyrics.get(position).getId());
				} else {
				selectedLyricIds.remove(Integer.valueOf(allLyrics.get(position).getId()));
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (setlistDbHelper != null) {
			setlistDbHelper.close();
		}
		if (lyricDbHelper != null) {
			lyricDbHelper.close();
		}
	}
}