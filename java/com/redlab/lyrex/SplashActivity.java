package com.redlab.lyrex;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
	private static final int SPLASH_DURATION = 4000; // Extended to 4 seconds for animations
	
	private TextView redLabText, welcomeText, lyrexText;
	private ImageView backgroundImage;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		
		if (getSupportActionBar() != null) {
			getSupportActionBar().hide();
		}
		
		initializeViews();
		startAnimations();
		
		new Handler().postDelayed(() -> {
			Intent intent = new Intent(SplashActivity.this, MainActivity.class);
			startActivity(intent);
			finish();
		}, SPLASH_DURATION);
	}
	
	private void initializeViews() {
		redLabText = findViewById(R.id.redLabText);
		welcomeText = findViewById(R.id.welcomeText);
		lyrexText = findViewById(R.id.lyrexText);
		backgroundImage = findViewById(R.id.backgroundImage);
	}
	
	private void startAnimations() {
		// Background zoom animation
		animateBackground();
		
		// Text animations with delay
		new Handler().postDelayed(this::animateRedLab, 300);
		new Handler().postDelayed(this::animateLyrex, 600);
		new Handler().postDelayed(this::animateWelcome, 900);
	}
	
	private void animateBackground() {
		// Set initial position (bottom-left)
		backgroundImage.setScaleX(1.2f);
		backgroundImage.setScaleY(1.2f);
		backgroundImage.setTranslationX(-200f);
		backgroundImage.setTranslationY(200f);
		
		// Animate to center position
		ObjectAnimator scaleX = ObjectAnimator.ofFloat(backgroundImage, "scaleX", 1.2f, 1.0f);
		ObjectAnimator scaleY = ObjectAnimator.ofFloat(backgroundImage, "scaleY", 1.2f, 1.0f);
		ObjectAnimator translateX = ObjectAnimator.ofFloat(backgroundImage, "translationX", -200f, 0f);
		ObjectAnimator translateY = ObjectAnimator.ofFloat(backgroundImage, "translationY", 200f, 0f);
		
		AnimatorSet backgroundSet = new AnimatorSet();
		backgroundSet.playTogether(scaleX, scaleY, translateX, translateY);
		backgroundSet.setDuration(2000);
		backgroundSet.setInterpolator(new AccelerateDecelerateInterpolator());
		backgroundSet.start();
	}
	
	private void animateRedLab() {
		// Start off-screen left
		redLabText.setTranslationX(-500f);
		redLabText.setAlpha(0f);
		
		// Slide in from left with size animation
		ObjectAnimator slideIn = ObjectAnimator.ofFloat(redLabText, "translationX", -500f, 0f);
		ObjectAnimator fadeIn = ObjectAnimator.ofFloat(redLabText, "alpha", 0f, 1f);
		
		// Size animation - grow then shrink
		ValueAnimator sizeAnim = ValueAnimator.ofFloat(28f, 40f, 28f);
		sizeAnim.addUpdateListener(animation -> {
			float size = (Float) animation.getAnimatedValue();
			redLabText.setTextSize(size);
		});
		
		AnimatorSet redLabSet = new AnimatorSet();
		redLabSet.playTogether(slideIn, fadeIn, sizeAnim);
		redLabSet.setDuration(1000);
		redLabSet.setInterpolator(new AccelerateDecelerateInterpolator());
		redLabSet.start();
	}
	
	private void animateLyrex() {
		// Start off-screen right
		lyrexText.setTranslationX(500f);
		lyrexText.setAlpha(0f);
		
		// Slide in from right with size animation
		ObjectAnimator slideIn = ObjectAnimator.ofFloat(lyrexText, "translationX", 500f, 0f);
		ObjectAnimator fadeIn = ObjectAnimator.ofFloat(lyrexText, "alpha", 0f, 1f);
		
		// Size animation - grow then shrink
		ValueAnimator sizeAnim = ValueAnimator.ofFloat(36f, 50f, 36f);
		sizeAnim.addUpdateListener(animation -> {
			float size = (Float) animation.getAnimatedValue();
			lyrexText.setTextSize(size);
		});
		
		AnimatorSet lyrexSet = new AnimatorSet();
		lyrexSet.playTogether(slideIn, fadeIn, sizeAnim);
		lyrexSet.setDuration(1000);
		lyrexSet.setInterpolator(new AccelerateDecelerateInterpolator());
		lyrexSet.start();
	}
	
	private void animateWelcome() {
		// Simple fade in for "Welcome to"
		welcomeText.setAlpha(0f);
		ObjectAnimator fadeIn = ObjectAnimator.ofFloat(welcomeText, "alpha", 0f, 1f);
		fadeIn.setDuration(800);
		fadeIn.start();
	}
}