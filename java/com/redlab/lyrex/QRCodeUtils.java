package com.redlab.lyrex;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.Deflater;

public class QRCodeUtils {
	
	public static Bitmap generateQRCode(String data, int width, int height) {
		if (data == null || data.isEmpty()) {
			return null;
		}
		
		try {
			QRCodeWriter writer = new QRCodeWriter();
			BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height);
			
			Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
				}
			}
			return bitmap;
			} catch (WriterException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// Generate Plan A QR code (full lyrics)
	public static String createPlanAPayload(List<Lyric> lyrics) {
		if (lyrics == null || lyrics.isEmpty()) return null;
		
		StringBuilder sb = new StringBuilder();
		for (Lyric lyric : lyrics) {
			String title = (lyric.getTitle() != null) ? lyric.getTitle() : "Untitled";
			String content = (lyric.getContent() != null) ? lyric.getContent() : "";
			
			sb.append(title).append("\n")
			.append(content).append("\n")
			.append("---\n");
		}
		String rawData = sb.toString();
		
		// Compress and encode
		byte[] compressedBytes = compressWithDeflater(rawData);
		String base64Payload = Base64.getEncoder().encodeToString(compressedBytes);
		
		return "PLANA|v1|" + base64Payload;
	}

	// Generate Plan B QR code (titles only)
	public static String createPlanBPayload(String setlistName, List<Lyric> lyrics) {
		if (lyrics == null || lyrics.isEmpty()) return null;
		
		StringBuilder sb = new StringBuilder();
		sb.append(setlistName).append("\n"); // First line is the setlist name
		
		for (Lyric lyric : lyrics) {
			String title = (lyric.getTitle() != null) ? lyric.getTitle() : "Untitled";
			sb.append(title).append("\n");
		}
		String titlesData = sb.toString();
		
		// Compress and encode
		byte[] compressedBytes = compressWithDeflater(titlesData);
		String base64Payload = Base64.getEncoder().encodeToString(compressedBytes);
		
		return "PLANB|v1|" + base64Payload;
	}
	
	// Generate QR code for full lyrics (Plan A)
	public static Bitmap generatePlanAQRCode(List<Lyric> lyrics, int size) {
		String payload = createPlanAPayload(lyrics);
		if (payload != null) {
			return generateQRCode(payload, size, size);
		}
		return null;
	}
	
	// Generate QR code for titles only (Plan B)
	public static Bitmap generatePlanBQRCode(List<Lyric> lyrics, int size) {
		String payload = QRCodeUtils.createPlanBPayload("1st Set", lyrics);
		if (payload != null) {
			return generateQRCode(payload, size, size);
		}
		return null;
	}
	
	// Auto-detect which plan to use based on data size
	public static Bitmap generateAutoQRCode(List<Lyric> lyrics, int size) {
		if (lyrics == null || lyrics.isEmpty()) return null;
		
		// Build full data first to check size
		StringBuilder fullData = new StringBuilder();
		for (Lyric lyric : lyrics) {
			String title = (lyric.getTitle() != null) ? lyric.getTitle() : "Untitled";
			String content = (lyric.getContent() != null) ? lyric.getContent() : "";
			
			fullData.append(title).append("\n")
			.append(content).append("\n")
			.append("---\n");
		}
		
		String rawData = fullData.toString();
		
		// Determine which plan to use based on size (similar to MainActivity)
		if (rawData.getBytes(StandardCharsets.UTF_8).length > 2000) {
			return generatePlanBQRCode(lyrics, size); // Use Plan B if too large
			} else {
			return generatePlanAQRCode(lyrics, size); // Use Plan A if small enough
		}
	}
	
	// Compression method (same as in MainActivity)
	private static byte[] compressWithDeflater(String text) {
		try {
			byte[] input = text.getBytes(StandardCharsets.UTF_8);
			Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
			deflater.setInput(input);
			deflater.finish();
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
			byte[] buffer = new byte[1024];
			while (!deflater.finished()) {
				int count = deflater.deflate(buffer);
				bos.write(buffer, 0, count);
			}
			bos.close();
			deflater.end();
			
			return bos.toByteArray();
			} catch (Exception e) {
			e.printStackTrace();
			return text.getBytes(StandardCharsets.UTF_8); // Fallback to uncompressed
		}
	}
	
	// Decompression method (same as in MainActivity)
	public static String decompressWithInflater(byte[] compressed) {
		try {
			java.util.zip.Inflater inflater = new java.util.zip.Inflater();
			inflater.setInput(compressed);
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream(compressed.length * 2);
			byte[] buffer = new byte[1024];
			while (!inflater.finished()) {
				int count = inflater.inflate(buffer);
				bos.write(buffer, 0, count);
			}
			bos.close();
			inflater.end();
			
			return bos.toString("UTF-8");
			} catch (Exception e) {
			e.printStackTrace();
			return new String(compressed, StandardCharsets.UTF_8); // Fallback to uncompressed
		}
	}
}