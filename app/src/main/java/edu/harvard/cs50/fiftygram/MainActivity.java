package edu.harvard.cs50.fiftygram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import jp.wasabeef.glide.transformations.gpu.InvertFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.SepiaFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.SketchFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.ToonFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.VignetteFilterTransformation;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private ImageView imageView;
    private Bitmap original;
    private Bitmap modified;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    savePhoto(findViewById(android.R.id.content).getRootView());
                } else {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // user also CHECKED "never ask again"
                        // you can either enable some fall back,
                        // disable features of your app
                        // or open another dialog explaining
                        // again the permission and directing to
                        // the app setting
                        showMessageOK("To be able to save, you need to allow write access to your media (Storage permission). Go into your app settings to manually set this permission.");
                    }

                    return;
                }
            }
        }
    }

    public void apply(Transformation<Bitmap> filter) {
        if (modified != null) {
            modified = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            Glide
                    .with(this)
                    .load(modified)
                    .apply(RequestOptions.bitmapTransform(filter))
                    .into(imageView);
        }
    }

    public void applySepia(View view) {
        apply(new SepiaFilterTransformation());
    }

    public void applyToon(View view) {
        apply(new ToonFilterTransformation());
    }

    public void applySketch(View view) {
        apply(new SketchFilterTransformation());
    }

    public void applyInvert(View view) {
        apply(new InvertFilterTransformation());
    }

    public void applyVignette(View view) {
        apply(new VignetteFilterTransformation());
    }

    public void choosePhoto(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    public void savePhoto(View view) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
        if (bitmapDrawable != null) {
            modified = bitmapDrawable.getBitmap();
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.d("savePhoto", "devemos explicar");
                    showMessageOKCancel("To be able to save, you need to allow write access to your media.");
                    return;
                }
                Log.d("savePhoto", "nao devemos explicar");

                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            } else
                createDirectoryAndSaveFile(modified, UUID.randomUUID().toString());
            //MediaStore.Images.Media.insertImage(getContentResolver(), modified, UUID.randomUUID().toString() + ".jpg", "generated with Fiftygram.");
        } else {
            Log.d("savePhoto", "bitmapDrawable is null.");
        }
    }

    private void showMessageOKCancel(String message) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", listener)
                .setNegativeButton("Cancel", listener)
                .create()
                .show();
    }

    private void showMessageOK(String message) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", listener)
                .create()
                .show();
    }

    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

        final int BUTTON_NEGATIVE = -2;
        final int BUTTON_POSITIVE = -1;

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case BUTTON_NEGATIVE:
                    // int which = -2
                    dialog.dismiss();
                    break;

                case BUTTON_POSITIVE:
                    // int which = -1
                    if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    dialog.dismiss();
                    break;
            }
        }
    };

    private void createDirectoryAndSaveFile(Bitmap imageToSave, String fileName) {
        File direct = new File(Environment.getExternalStorageDirectory() + "/Fiftygram");

        if (!direct.exists()) {
            File wallpaperDirectory = new File("/sdcard/Fiftygram/");
            wallpaperDirectory.mkdirs();
        }

        @SuppressLint("SdCardPath")
        File file = new File("/sdcard/Fiftygram/", fileName + ".jpeg");
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reset(View view) {
        if (original != null) {
            modified = original.copy(original.getConfig(), true);
            imageView.setImageBitmap(modified);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();
                ParcelFileDescriptor parcelFileDescriptor =
                        getContentResolver().openFileDescriptor(uri, "r");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                original = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
                modified = original.copy(original.getConfig(), true);
                imageView.setImageBitmap(modified);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
