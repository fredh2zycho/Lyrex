package com.redlab.lyrex;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import java.io.IOException;

public class AudioPlayerActivity extends AppCompatActivity {
	private ImageButton btnPlayPause, btnSelectAudio;
	private TextView tvNowPlaying;
	private SeekBar seekBarProgress;
	private boolean isPlaying = false;
	private Uri audioUri;
	private Handler seekbarHandler = new Handler();
	
	// Service connection
	private AudioPlayerService audioService;
	private boolean isServiceBound = false;
	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			AudioPlayerService.LocalBinder binder = (AudioPlayerService.LocalBinder) service;
			audioService = binder.getService();
			isServiceBound = true;
			
			// Update UI based on service state
			if (audioService.isPlaying()) {
				isPlaying = true;
				updatePlayButton();
				updateSeekbar();
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			isServiceBound = false;
			audioService = null;
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_player);
		
		// Start and bind to the service
		Intent serviceIntent = new Intent(this, AudioPlayerService.class);
		startService(serviceIntent);
		bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
		
		setupWindowProperties();
		initializeViews();
		setupListeners();
		
		tvNowPlaying.setText("Ready to practice - Select an audio file");
	}
	
	private void setupWindowProperties() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		
		WindowManager.LayoutParams params = getWindow().getAttributes();
		params.width = WindowManager.LayoutParams.MATCH_PARENT;
		params.height = WindowManager.LayoutParams.WRAP_CONTENT;
		params.x = 0;
		params.y = 100;
		params.alpha = 0.92f;
		
		params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
		WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
		WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		
		getWindow().setAttributes(params);
		getWindow().setBackgroundDrawableResource(android.R.color.transparent);
	}
	
	private void initializeViews() {
		btnPlayPause = findViewById(R.id.btn_play_pause);
		btnSelectAudio = findViewById(R.id.btn_select_audio);
		tvNowPlaying = findViewById(R.id.tv_now_playing);
		seekBarProgress = findViewById(R.id.seekbar_progress);
		seekBarProgress.setEnabled(false);
	}
	
	private void setupListeners() {
		// Close button - STOPS playback completely
		findViewById(R.id.btn_close).setOnClickListener(v -> {
			stopPlayback();
			finish();
		});
		
		// Minimize button - continues playback in background
		findViewById(R.id.btn_minimize).setOnClickListener(v -> minimizePlayer());
		
		// Select Audio button
		btnSelectAudio.setOnClickListener(v -> openAudioFilePicker());
		
		// Play/Pause button
		btnPlayPause.setOnClickListener(v -> {
			if (audioUri == null) {
				Toast.makeText(this, "Please select an audio file first", Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (isPlaying) {
				pauseAudio();
				} else {
				playAudio();
			}
			updatePlayButton();
		});
		
		// Previous button (skip backward 10 seconds)
		findViewById(R.id.btn_prev).setOnClickListener(v -> {
			if (isServiceBound && audioService != null) {
				int newPosition = audioService.getCurrentPosition() - 10000;
				newPosition = Math.max(0, newPosition);
				audioService.seekTo(newPosition);
				seekBarProgress.setProgress(newPosition);
			}
		});
		
		// Next button (skip forward 10 seconds)
		findViewById(R.id.btn_next).setOnClickListener(v -> {
			if (isServiceBound && audioService != null) {
				int newPosition = audioService.getCurrentPosition() + 10000;
				int duration = audioService.getDuration();
				if (duration > 0) {
					newPosition = Math.min(newPosition, duration);
				}
				audioService.seekTo(newPosition);
				seekBarProgress.setProgress(newPosition);
			}
		});
		
		// Seekbar listener
		seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser && isServiceBound && audioService != null) {
					audioService.seekTo(progress);
				}
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		
		// Touch outside container to minimize
		View container = findViewById(R.id.player_container);
		if (container != null) {
			container.setOnTouchListener((v, event) -> {
				if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
					minimizePlayer();
					return true;
				}
				return false;
			});
		}
	}
	
	private void minimizePlayer() {
		// Just finish this activity - service keeps playing in background
		finish();
	}
	
	private void stopPlayback() {
		if (isServiceBound && audioService != null) {
			audioService.stopAudio();
		}
		isPlaying = false;
	}
	
	private void openAudioFilePicker() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("audio/*");
		startActivityForResult(intent, 1);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
			audioUri = data.getData();
			if (audioUri != null) {
				// Service will handle the playback
				String fileName = "Selected audio";
				DocumentFile documentFile = DocumentFile.fromSingleUri(this, audioUri);
				if (documentFile != null && documentFile.getName() != null) {
					fileName = documentFile.getName();
				}
				
				tvNowPlaying.setText("Ready: " + fileName);
				seekBarProgress.setEnabled(true);
				btnPlayPause.setEnabled(true);
				
				Toast.makeText(this, "Audio file loaded", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private void playAudio() {
		if (isServiceBound && audioService != null && audioUri != null) {
			audioService.playAudio(audioUri.toString());
			isPlaying = true;
			updateSeekbar();
			
			// Set max duration once we have it
			new Handler().postDelayed(() -> {
				if (isServiceBound && audioService != null) {
					seekBarProgress.setMax(audioService.getDuration());
				}
			}, 1000);
		}
	}
	
	private void pauseAudio() {
		if (isServiceBound && audioService != null) {
			audioService.pauseAudio();
			isPlaying = false;
		}
	}
	
	private void updateSeekbar() {
		if (isServiceBound && audioService != null && audioService.isPlaying()) {
			seekBarProgress.setProgress(audioService.getCurrentPosition());
			seekbarHandler.postDelayed(this::updateSeekbar, 1000);
		}
	}
	
	private void updatePlayButton() {
		if (isPlaying) {
			btnPlayPause.setImageResource(R.drawable.ic_pause);
			} else {
			btnPlayPause.setImageResource(R.drawable.ic_play);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
			minimizePlayer();
			return true;
		}
		return super.onTouchEvent(event);
	}
	
	@Override
	public void onBackPressed() {
		minimizePlayer(); // Minimize instead of closing
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unbind from service but don't stop it - keeps playing in background
		if (isServiceBound) {
			unbindService(serviceConnection);
			isServiceBound = false;
		}
		seekbarHandler.removeCallbacksAndMessages(null);
	}
}