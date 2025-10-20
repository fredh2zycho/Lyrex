package com.redlab.lyrex;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
* Enhanced Adapter for displaying lyrics in RecyclerView with beautiful Material Design
*/
public class LyricAdapter extends RecyclerView.Adapter<LyricAdapter.LyricViewHolder> {
	private final Context context;
	private List<Lyric> lyrics;
	private final Set<Integer> selectedIds = new HashSet<>();
	private final OnLyricClickListener clickListener;
	
	public interface OnLyricClickListener {
		void onLyricClick(Lyric lyric);
	}
	
	public LyricAdapter(Context context, List<Lyric> lyrics, OnLyricClickListener listener) {
		this.context = context;
		this.lyrics = (lyrics != null) ? lyrics : new ArrayList<>();
		this.clickListener = listener;
	}
	
	@NonNull
	@Override
	public LyricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(context).inflate(R.layout.lyric_item, parent, false);
		return new LyricViewHolder(view);
	}
	
	@Override
	public void onBindViewHolder(@NonNull LyricViewHolder holder, int position) {
		Lyric lyric = lyrics.get(position);
		holder.bind(lyric);
	}
	
	@Override
	public int getItemCount() {
		return lyrics.size();
	}
	
	public void updateData(List<Lyric> newLyrics) {
		this.lyrics = (newLyrics != null) ? newLyrics : new ArrayList<>();
		notifyDataSetChanged();
	}
	
	private String getFormattedDate(long timestamp) {
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
		return sdf.format(new Date(timestamp));
	}
	
	// ---------------- Selection Handling ----------------
	
	public void toggleSelection(Lyric lyric) {
		if (lyric == null) return;
		int id = lyric.getId();
		if (selectedIds.contains(id)) {
			selectedIds.remove(id);
			} else {
			selectedIds.add(id);
		}
		notifyDataSetChanged();
	}
	
	public void clearSelection() {
		selectedIds.clear();
		notifyDataSetChanged();
	}
	
	public void selectAll() {
		selectedIds.clear();
		for (Lyric l : lyrics) {
			selectedIds.add(l.getId());
		}
		notifyDataSetChanged();
	}
	
	public List<Lyric> getSelectedLyrics() {
		List<Lyric> result = new ArrayList<>();
		for (Lyric l : lyrics) {
			if (selectedIds.contains(l.getId())) {
				result.add(l);
			}
		}
		return result;
	}
	
	public int getSelectedCount() {
		return selectedIds.size();
	}
	
	public boolean isSelected(Lyric lyric) {
		return lyric != null && selectedIds.contains(lyric.getId());
	}
	
	// ---------------- Enhanced ViewHolder ----------------
	
	class LyricViewHolder extends RecyclerView.ViewHolder {
		CardView cardView;
		TextView titleView;
		TextView previewView;
		TextView dateView;
		TextView lengthView;
		ImageView selectionIndicator;
		
		LyricViewHolder(@NonNull View itemView) {
			super(itemView);
			titleView = itemView.findViewById(R.id.titleTextView);
			
			// Try to find the new views, but handle case where they might not exist
			try {
				cardView = itemView.findViewById(R.id.card_view);
				previewView = itemView.findViewById(R.id.tv_preview);
				dateView = itemView.findViewById(R.id.tv_date);
				lengthView = itemView.findViewById(R.id.tv_length);
				selectionIndicator = itemView.findViewById(R.id.iv_selection);
				} catch (Exception e) {
				// Views not found, use basic layout
			}
			
			setupClickListeners();
		}
		
		private void setupClickListeners() {
			// Normal click → show options
			itemView.setOnClickListener(v -> {
				int pos = getAdapterPosition();
				if (pos != RecyclerView.NO_POSITION && clickListener != null) {
					clickListener.onLyricClick(lyrics.get(pos));
				}
			});
			
			// Long press → toggle selection
			itemView.setOnLongClickListener(v -> {
				int pos = getAdapterPosition();
				if (pos != RecyclerView.NO_POSITION) {
					toggleSelection(lyrics.get(pos));
				}
				return true;
			});
		}
		
		void bind(Lyric lyric) {
			// Set title
			titleView.setText(lyric.getTitle());
			
			// Check if the card view is available to apply enhanced visuals
			if (cardView != null) {
				// Set preview text
				String preview = getPreviewText(lyric.getContent());
				previewView.setText(preview);
				previewView.setVisibility(TextUtils.isEmpty(preview) ? View.GONE : View.VISIBLE);
				
				// This is the correct way to set the date
				// Create a new method or use a SimpleDateFormat instance
				long timestamp = lyric.getTimestamp();
				if (timestamp > 0) {
					String formattedDate = getFormattedDate(timestamp);
					dateView.setText(formattedDate);
					} else {
					// Fallback for old lyrics without a timestamp
					dateView.setText("No Date");
				}
				
				// Set word/line count
				String lengthText = getLengthText(lyric.getContent());
				lengthView.setText(lengthText);
				
				// Handle selection state
				boolean isSelected = selectedIds.contains(lyric.getId());
				updateSelectionUI(isSelected);
				} else {
				// Basic selection highlighting for old layout
				if (selectedIds.contains(lyric.getId())) {
					itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_background));
					} else {
					itemView.setBackgroundColor(Color.TRANSPARENT);
				}
			}
		}
		
		private String getPreviewText(String content) {
			if (TextUtils.isEmpty(content)) {
				return "";
			}
			
			// Get first line or first 60 characters
			String[] lines = content.split("\n");
			if (lines.length > 0 && !lines[0].trim().isEmpty()) {
				String firstLine = lines[0].trim();
				return firstLine.length() > 60 ? firstLine.substring(0, 60) + "..." : firstLine;
			}
			
			// Fallback: first 60 chars of content
			return content.length() > 60 ? content.substring(0, 60) + "..." : content;
		}
		
		private String getLengthText(String content) {
			if (TextUtils.isEmpty(content)) {
				return "0 words";
			}
			
			int wordCount = content.split("\\s+").length;
			int lineCount = content.split("\n").length;
			
			if (wordCount < 50) {
				return wordCount + " words";
				} else {
				return lineCount + " lines • " + wordCount + " words";
			}
		}
		
		private void updateSelectionUI(boolean isSelected) {
			if (cardView == null) return;
			
			if (isSelected) {
				// Selected state
				cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.selected_background));
				cardView.setCardElevation(8f);
				if (selectionIndicator != null) {
					selectionIndicator.setVisibility(View.VISIBLE);
				}
				titleView.setTextColor(ContextCompat.getColor(context, R.color.selected_text));
				} else {
				// Normal state
				cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background));
				cardView.setCardElevation(2f);
				if (selectionIndicator != null) {
					selectionIndicator.setVisibility(View.GONE);
				}
				titleView.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
			}
		}
	}
}