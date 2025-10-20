package com.redlab.lyrex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetlistManagerActivity extends AppCompatActivity {
	private SetlistDbHelper setlistDbHelper;
	private ListView setlistListView;
	private TextView tvEmptyState;
	private Button btnDelete;
	private List<Setlist> currentSetlists;
	private ArrayAdapter<String> adapter;
	private Set<Integer> selectedPositions = new HashSet<>();
	private boolean isSelectionMode = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setlist_manager);
		
		setlistDbHelper = new SetlistDbHelper(this);
		setlistListView = findViewById(R.id.lv_setlists);
		tvEmptyState = findViewById(R.id.tv_empty_state);
		btnDelete = findViewById(R.id.btn_delete);
		ImageButton btnBack = findViewById(R.id.btn_back);
		Button btnCreateNew = findViewById(R.id.btn_create_new);
		
		btnBack.setOnClickListener(v -> {
			if (isSelectionMode) {
				exitSelectionMode();
				} else {
				finish();
			}
		});
		
		btnCreateNew.setOnClickListener(v -> {
			Intent intent = new Intent(this, SetlistActivity.class);
			startActivity(intent);
		});
		
		btnDelete.setOnClickListener(v -> showDeleteConfirmation());
		
		loadSetlists();
	}
	
	private void loadSetlists() {
		currentSetlists = setlistDbHelper.getAllSetlists();
		
		if (currentSetlists == null || currentSetlists.isEmpty()) {
			tvEmptyState.setVisibility(View.VISIBLE);
			setlistListView.setVisibility(View.GONE);
			btnDelete.setVisibility(View.GONE);
			} else {
			tvEmptyState.setVisibility(View.GONE);
			setlistListView.setVisibility(View.VISIBLE);
			
			updateListView();
			setupListViewListeners();
		}
	}
	
	private void updateListView() {
		String[] setlistNames = new String[currentSetlists.size()];
		for (int i = 0; i < currentSetlists.size(); i++) {
			Setlist setlist = currentSetlists.get(i);
			int songCount = setlist.getLyricIds().size();
			String prefix = selectedPositions.contains(i) ? "✓ " : "";
			setlistNames[i] = prefix + setlist.getName() + " (" + songCount + " songs)";
		}
		
		adapter = new ArrayAdapter<>(
		this,
		android.R.layout.simple_list_item_1,
		setlistNames
		);
		setlistListView.setAdapter(adapter);
	}
	
	private void setupListViewListeners() {
		// Single tap - open setlist or toggle selection
		setlistListView.setOnItemClickListener((parent, view, position, id) -> {
			if (isSelectionMode) {
				toggleSelection(position);
				} else {
				Setlist selectedSetlist = currentSetlists.get(position);
				openSetlist(selectedSetlist);
			}
		});
		
		// Long press - show options (rename/delete)
		setlistListView.setOnItemLongClickListener((parent, view, position, id) -> {
			if (!isSelectionMode) {
				showSetlistOptions(position);
			}
			return true;
		});
	}
	
	private void showSetlistOptions(int position) {
		Setlist setlist = currentSetlists.get(position);
		String[] options = {"Rename", "Delete"};
		
		new AlertDialog.Builder(this)
		.setTitle(setlist.getName())
		.setItems(options, (dialog, which) -> {
			switch (which) {
				case 0: // Rename
				showRenameDialog(setlist);
				break;
				case 1: // Delete
				showSingleDeleteConfirmation(setlist);
				break;
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void showRenameDialog(Setlist setlist) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View dialogView = getLayoutInflater().inflate(R.layout.dialog_rename_setlist, null);
		
		EditText etSetlistName = dialogView.findViewById(R.id.etSetlistName);
		etSetlistName.setText(setlist.getName());
		etSetlistName.setSelection(etSetlistName.getText().length());
		
		builder.setView(dialogView)
		.setTitle("Rename Setlist")
		.setPositiveButton("Rename", (dialog, which) -> {
			String newName = etSetlistName.getText().toString().trim();
			if (!newName.isEmpty() && !newName.equals(setlist.getName())) {
				renameSetlist(setlist, newName);
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void renameSetlist(Setlist setlist, String newName) {
		boolean success = setlistDbHelper.renameSetlist(setlist.getId(), newName);
		
		if (success) {
			Toast.makeText(this, "Setlist renamed to \"" + newName + "\"", Toast.LENGTH_SHORT).show();
			loadSetlists(); // Refresh the list
			} else {
			Toast.makeText(this, "Failed to rename setlist", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void showSingleDeleteConfirmation(Setlist setlist) {
		new AlertDialog.Builder(this)
		.setTitle("Confirm Delete")
		.setMessage("Delete \"" + setlist.getName() + "\"?\n\nThis action cannot be undone.")
		.setPositiveButton("Delete", (dialog, which) -> deleteSingleSetlist(setlist))
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void deleteSingleSetlist(Setlist setlist) {
		setlistDbHelper.deleteSetlist(setlist.getId());
		Toast.makeText(this, "Setlist deleted", Toast.LENGTH_SHORT).show();
		loadSetlists(); // Refresh the list
	}
	
	private void enterSelectionMode() {
		isSelectionMode = true;
		selectedPositions.clear();
		btnDelete.setVisibility(View.VISIBLE);
		Toast.makeText(this, "Selection mode - tap to select setlists", Toast.LENGTH_SHORT).show();
	}
	
	private void exitSelectionMode() {
		isSelectionMode = false;
		selectedPositions.clear();
		btnDelete.setVisibility(View.GONE);
		updateListView();
	}
	
	private void toggleSelection(int position) {
		if (selectedPositions.contains(position)) {
			selectedPositions.remove(position);
			} else {
			selectedPositions.add(position);
		}
		
		if (selectedPositions.isEmpty()) {
			exitSelectionMode();
			} else {
			updateListView();
		}
	}
	
	private void showDeleteConfirmation() {
		if (selectedPositions.isEmpty()) {
			Toast.makeText(this, "No setlists selected", Toast.LENGTH_SHORT).show();
			return;
		}
		
		String message = selectedPositions.size() == 1 ?
		"Delete this setlist?" :
		"Delete " + selectedPositions.size() + " setlists?";
		
		new AlertDialog.Builder(this)
		.setTitle("Confirm Delete")
		.setMessage(message + "\n\nThis action cannot be undone.")
		.setPositiveButton("Delete", (dialog, which) -> deleteSelectedSetlists())
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void deleteSelectedSetlists() {
		List<Integer> positionsToDelete = new ArrayList<>(selectedPositions);
		// Sort in descending order to delete from end to beginning
		positionsToDelete.sort((a, b) -> b.compareTo(a));
		
		int deletedCount = 0;
		for (int position : positionsToDelete) {
			if (position < currentSetlists.size()) {
				Setlist setlistToDelete = currentSetlists.get(position);
				setlistDbHelper.deleteSetlist(setlistToDelete.getId());
				deletedCount++;
			}
		}
		
		String message = deletedCount == 1 ?
		"Setlist deleted" :
		deletedCount + " setlists deleted";
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
		
		exitSelectionMode();
		loadSetlists(); // Refresh the list
	}
	
	private void openSetlist(Setlist setlist) {
		Intent intent = new Intent(this, SetlistViewerActivity.class);
		intent.putExtra("setlist_id", setlist.getId());
		startActivity(intent);
	}
	
	@Override
	public void onBackPressed() {
		if (isSelectionMode) {
			exitSelectionMode();
			} else {
			super.onBackPressed();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		loadSetlists(); // Refresh when returning from other activities
	}
}