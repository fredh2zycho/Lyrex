package com.redlab.lyrex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.zxing.Result;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import java.util.List;

public class QRScannerActivity extends AppCompatActivity {
	private static final int CAMERA_PERMISSION_REQUEST = 100;
	private DecoratedBarcodeView barcodeView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qr_scanner);
		
		barcodeView = findViewById(R.id.barcode_scanner);
		
		// Check camera permission
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
		!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
			new String[]{Manifest.permission.CAMERA},
			CAMERA_PERMISSION_REQUEST);
			} else {
			startScanner();
		}
	}
	
	private void startScanner() {
		barcodeView.decodeSingle(new BarcodeCallback() {
			@Override
			public void barcodeResult(BarcodeResult result) {
				handleResult(result);
			}
		});
		barcodeView.resume();
	}
	
	private void handleResult(BarcodeResult result) {
		String qrData = result.getText();
		
		if (qrData != null && (qrData.startsWith("PLANA|") || qrData.startsWith("PLANB|"))) {
			processLyrexQRData(qrData);
			} else {
			Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show();
			barcodeView.resume();
		}
	}
	
	private void processLyrexQRData(String qrData) {
		try {
			// Return any valid PLANA or PLANB QR code data
			Intent resultIntent = new Intent();
			resultIntent.putExtra("qr_data", qrData);
			setResult(RESULT_OK, resultIntent);
			finish();
			} catch (Exception e) {
			Toast.makeText(this, "Error processing QR data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			barcodeView.resume();
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
	@NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == CAMERA_PERMISSION_REQUEST) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				startScanner();
				} else {
				Toast.makeText(this, "Camera permission required for QR scanning", Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		barcodeView.pause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (ContextCompat.checkSelfPermission(this,
		Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
			barcodeView.resume();
		}
	}
}