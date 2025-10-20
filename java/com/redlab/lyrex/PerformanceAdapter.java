package com.redlab.lyrex;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PerformanceAdapter extends RecyclerView.Adapter<PerformanceAdapter.ViewHolder> {
	private final List<Lyric> lyrics;
	private float fontSize;
	private final Context context;
	
	public PerformanceAdapter(Context context, List<Lyric> lyrics, float density) {
		this.context = context;
		this.lyrics = lyrics;
		this.fontSize = 16f * density; // Scale initial font size by density
		
		// Pre-highlight all lyrics
		for (Lyric lyric : lyrics) {
			lyric.highlightSections();
		}
	}
	
	public void setFontSize(float fontSize) {
		this.fontSize = fontSize;
		notifyDataSetChanged(); // Refresh all items when font size changes
	}
	
	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext())
		.inflate(R.layout.item_performance_lyric, parent, false);
		return new ViewHolder(view);
	}
	
	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		Lyric lyric = lyrics.get(position);
		holder.bind(lyric, fontSize);
	}
	
	private void applySectionBackground(View itemView, String sectionType) {
		if (sectionType == null) {
			itemView.setBackgroundColor(android.graphics.Color.BLACK);
			return;
		}
		
		int colorRes;
		switch (sectionType.toLowerCase()) {
			case "chorus":
			colorRes = R.color.highlight_chorus;
			break;
			case "verse":
			colorRes = R.color.highlight_verse;
			break;
			case "pre-chorus":
			colorRes = R.color.highlight_prechorus;
			break;
			case "bridge":
			colorRes = R.color.highlight_bridge;
			break;
			case "refrain":
			colorRes = R.color.highlight_refrain;
			break;
			case "intro":
			colorRes = R.color.highlight_intro;
			break;
			case "outro":
			colorRes = R.color.highlight_outro;
			break;
			default:
			itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
			return;
		}
		
		// FIXED: Use ContextCompat to get color properly
		itemView.setBackgroundColor(ContextCompat.getColor(context, colorRes));
	}
	
	@Override
	public int getItemCount() {
		return lyrics != null ? lyrics.size() : 0;
	}
	
	public static class ViewHolder extends RecyclerView.ViewHolder {
		private final TextView tvTitle;
		private final TextView tvContent;
		private final TextView tvSectionBadge;
		
		public ViewHolder(@NonNull View itemView) {
			super(itemView);
			tvTitle = itemView.findViewById(R.id.tv_lyric_title);
			tvContent = itemView.findViewById(R.id.tv_lyric_content);
			tvSectionBadge = itemView.findViewById(R.id.tv_section_badge);
		}
		
		public void bind(Lyric lyric, float fontSize) {
			if (lyric != null) {
				// Always hide the section badge (if that's what you want)
				tvSectionBadge.setVisibility(View.GONE);
				
				tvTitle.setText(lyric.getTitle());
				
				// Use highlighted content if available, otherwise use regular content
				SpannableStringBuilder styledContent = lyric.getStyledContent();
				if (styledContent != null) {
					tvContent.setText(styledContent);
					} else {
					tvContent.setText(lyric.getContent());
				}
				
				// Apply font size - ensure minimum readable size
				float titleSize = Math.max(14f, fontSize + 4f);
				float contentSize = Math.max(12f, fontSize);
				
				tvTitle.setTextSize(titleSize);
				tvContent.setTextSize(contentSize);
				tvSectionBadge.setTextSize(Math.max(10f, fontSize - 2f));
			}
		}
	}
}