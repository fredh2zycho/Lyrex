package com.redlab.lyrex;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class QRShareActivity extends AppCompatActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qr_share);
		
		ImageView qrImageView = findViewById(R.id.qrImageView);
		
		// Get the lyric IDs from intent
		List<Integer> lyricIds = getIntent().getIntegerArrayListExtra("lyric_ids");
		String setlistName = getIntent().getStringExtra("setlist_name");
		
		if (lyricIds == null || lyricIds.isEmpty()) {
			Toast.makeText(this, "No lyrics to share", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		// Get the actual Lyric objects from the database using the EXISTING method
		LyricDbHelper dbHelper = new LyricDbHelper(this);
		List<Lyric> lyrics = new ArrayList<>();
		for (Integer id : lyricIds) {
			Lyric lyric = dbHelper.getLyricById(id); // Use the existing method
			if (lyric != null) {
				lyrics.add(lyric);
			}
		}
		dbHelper.close();
		
		if (lyrics.isEmpty()) {
			Toast.makeText(this, "No valid lyrics found", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		// Generate QR code using the new format
		Bitmap qrCode = QRCodeUtils.generateAutoQRCode(lyrics, 400);
		
		if (qrCode != null) {
			qrImageView.setImageBitmap(qrCode);
			} else {
			Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
			finish();
		}
	}
}