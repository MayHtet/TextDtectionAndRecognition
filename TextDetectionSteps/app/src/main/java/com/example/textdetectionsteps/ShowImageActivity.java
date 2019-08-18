package com.example.textdetectionsteps;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class ShowImageActivity extends AppCompatActivity {

    private static final String TAG = ShowImageActivity.class.getSimpleName();
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString();
    private Uri outputFileDir;
    Button btnProcess;
    ImageView imgBitmap;
    Bitmap bitmap;
    Activity activity;




    public static Intent getIntent(Context context){
        return new Intent(context, ShowImageActivity.class);
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_image);

        btnProcess = findViewById(R.id.btn_process);
        imgBitmap = findViewById(R.id.img_bitmap);


        activity = ShowImageActivity.this;

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 120);
        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 121);
        }

        startCameraActivity();




        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = ProcessActivity.getIntent(activity);
                intent.putExtra("BitmapImage", outputFileDir.getPath());
                startActivity(intent);
                finish();
            }
        });

    }

    public Bitmap rotateImage(Bitmap img, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    public void startCameraActivity() {
        try {
            String imagePath = DATA_PATH + "/imgs";
            File dir = new File(imagePath);
            if (!dir.exists()) {
                dir.mkdir();
            }
            Log.e("file exist", "" + dir.exists());
            String imageFilePath = imagePath + "/ocr.jpg";


            outputFileDir = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", new File(imageFilePath));

            this.grantUriPermission("com.example.textdetection", outputFileDir, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            this.grantUriPermission("com.example.textdetection", outputFileDir, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


            Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);


            imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileDir);
            imageIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            imageIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


            if (imageIntent.resolveActivity(getPackageManager()) != null) {

                startActivityForResult(imageIntent, 100);

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {


        if ( resultCode == Activity.RESULT_OK && requestCode == 100) {




            ExifInterface exifInterface = null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;

            bitmap = BitmapFactory.decodeFile(outputFileDir.getPath(), options);


            try {
                exifInterface = new ExifInterface(outputFileDir.getPath());


            } catch (IOException e) {
                Log.d("Error", "IO problem");
            }

            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    bitmap = rotateImage(bitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    bitmap = rotateImage(bitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    bitmap = rotateImage(bitmap, 270);
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    break;

            }

            imgBitmap.setImageBitmap(bitmap);

        }
    }
}
