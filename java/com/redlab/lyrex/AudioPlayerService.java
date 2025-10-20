package com.redlab.lyrex;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.Binder;
import java.io.IOException;

public class AudioPlayerService extends Service {
	private MediaPlayer mediaPlayer;
	private final IBinder binder = new LocalBinder();
	private String currentFilePath;
	
	public class LocalBinder extends Binder {
		AudioPlayerService getService() {
			return AudioPlayerService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	public void playAudio(String filePathOrUri) {
		try {
			if (mediaPlayer != null) {
				mediaPlayer.release();
			}
			mediaPlayer = new MediaPlayer();
			
			if (filePathOrUri.startsWith("content://") || filePathOrUri.startsWith("file://")) {
				// It's a Uri
				mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(filePathOrUri));
				} else {
				// It's a file path
				mediaPlayer.setDataSource(filePathOrUri);
			}
			
			mediaPlayer.prepare();
			mediaPlayer.start();
			currentFilePath = filePathOrUri;
			} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void pauseAudio() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
		}
	}
	
	public void resumeAudio() {
		if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
			mediaPlayer.start();
		}
	}
	
	public void stopAudio() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}
	
	public boolean isPlaying() {
		return mediaPlayer != null && mediaPlayer.isPlaying();
	}
	
	public int getCurrentPosition() {
		return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
	}
	
	public int getDuration() {
		return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
	}
	
	public void seekTo(int position) {
		if (mediaPlayer != null) {
			mediaPlayer.seekTo(position);
		}
	}
}