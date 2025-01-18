package com.example.ImageToText;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_SELECT = 2;
    private static final int REQUEST_PERMISSIONS = 3;
    private Uri photoUri;
    private File photoFile;
    private ImageView imageView;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Button btnCaptureImage = findViewById(R.id.btnCaptureImage);
        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        Button btnConvert = findViewById(R.id.btnConvert);
        imageView = findViewById(R.id.imageView);
        editText = findViewById(R.id.editText);
        Button btnSave = findViewById(R.id.btnsave);
        btnSave.setOnClickListener(v -> {
            String text = editText.getText().toString();
            if (!text.isEmpty()) {
                saveTextAsWordFile(text);
            } else {
                Toast.makeText(MainActivity.this, "No text to save", Toast.LENGTH_SHORT).show();
            }
        });

        btnCaptureImage.setOnClickListener(v -> {
            if (checkPermissions()) {
                captureImage();
            } else {
                requestPermissions();
            }
        });

        btnSelectImage.setOnClickListener(v -> {
            Intent selectPictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(selectPictureIntent, REQUEST_IMAGE_SELECT);
        });

        btnConvert.setOnClickListener(v -> {
            BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
            if (drawable != null) {
                Bitmap bitmap = drawable.getBitmap();
                processImage(bitmap);
            } else {
                Toast.makeText(MainActivity.this, "Vui lòng chọn hoặc chụp ảnh trước", Toast.LENGTH_SHORT).show();
            }
        });

        if (savedInstanceState != null) {
            photoUri = savedInstanceState.getParcelable("photoUri");
            if (photoUri != null) {
                imageView.setImageURI(photoUri);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelable("photoUri", photoUri);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        photoUri = savedInstanceState.getParcelable("photoUri");
    }
    private void saveTextAsWordFile(String text) {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);

        String fileName = "RecognizedText_" + System.currentTimeMillis() + ".docx";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        try (FileOutputStream out = new FileOutputStream(file)) {
            document.write(out);
            Toast.makeText(this, "File saved: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
    }

    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
                if (photoFile != null) {
                    photoUri = FileProvider.getUriForFile(this, "com.example.textrecognition.fileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    Toast.makeText(this, "Photo file is null", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        Log.d("MainActivity", "Image file created: " + imageFile.getAbsolutePath());
        return imageFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (photoUri != null) {
                    Log.d("MainActivity", "Photo URI: " + photoUri.toString());
                    imageView.setImageURI(photoUri);
                } else {
                    Toast.makeText(this, "Photo URI is null", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_IMAGE_SELECT && data != null) {
                Uri imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Cannot select image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void processImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(latinVisionText -> {
                    String latinText = latinVisionText.getText();
                    editText.setText(latinText);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, "Error recognizing Latin text: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            } else {
                Toast.makeText(this, "Quyền truy cập bị từ chối", Toast.LENGTH_SHORT).show();
            }
        }
    }
}