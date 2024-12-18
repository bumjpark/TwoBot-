package com.example.chatbot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class PhotoPickerHelper {

    private final Activity activity;
    private final ActivityResultLauncher<Intent> cameraLauncher;
    private final ImageView selectedImageView;
    private final OnTextDetectedListener onTextDetectedListener;

    public interface OnTextDetectedListener {
        void onTextDetected(String detectedText);
    }

    public PhotoPickerHelper(Activity activity, ActivityResultLauncher<Intent> cameraLauncher, ImageView selectedImageView, OnTextDetectedListener listener) {
        this.activity = activity;
        this.cameraLauncher = cameraLauncher;
        this.selectedImageView = selectedImageView;
        this.onTextDetectedListener = listener;
    }

    // 카메라 실행 메서드
    public void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    // 카메라 결과 처리 및 텍스트 인식
    public void handleCameraResult(Intent data) {
        if (data != null && data.getExtras() != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            if (bitmap != null) {
                selectedImageView.setVisibility(View.VISIBLE);
                selectedImageView.setImageBitmap(bitmap);
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                recognizeText(image);
            }
        }
    }

    // 텍스트 인식
    private void recognizeText(InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS); // 옵션 추가
        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String resultText = visionText.getText();
                    Toast.makeText(activity, "인식된 텍스트: " + resultText, Toast.LENGTH_LONG).show();
                    if (onTextDetectedListener != null) {
                        onTextDetectedListener.onTextDetected(resultText);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(activity, "텍스트 인식 실패", Toast.LENGTH_SHORT).show());
    }
}