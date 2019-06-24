package com.fenctose.imagecropper;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.fenchtose.nocropper.BitmapCropCallback;
import com.fenchtose.nocropper.CropperCallback;
import com.fenchtose.nocropper.CropperImageView;
import com.fenchtose.nocropper.ImageCache;
import com.fenchtose.nocropper.IntentHelpers;
import com.fenchtose.nocropper.ScaledCropper;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_READ_PERMISSION = 22;
    private static final int REQUEST_GALLERY = 21;
    private static final String TAG = "MainActivity";

    @BindView(R.id.imageview)
    CropperImageView mImageView;

    @BindView(R.id.original_checkbox)
    CheckBox originalImageCheckbox;

    @BindView(R.id.crop_checkbox)
    CheckBox cropAsyncCheckbox;

    @BindView(R.id.imageViewReview)
    ImageView imageViewReview;

    private Bitmap originalBitmap;
    private Bitmap mBitmap;
    private boolean isSnappedToCenter = false;

    private int rotationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_main_portrait);
        } else {
            Log.i(TAG, "Set landscape mode");
            setContentView(R.layout.activity_main_landscape);
        }
        ButterKnife.bind(this);
        mImageView.setHasZoom(false);
        mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mImageView.setGestureEnabled(true);

    }

    @OnClick(R.id.image_button)
    public void onImageButtonClicked() {
        startGalleryIntent();
    }

    @OnClick(R.id.crop_button)
    public void onImageCropClicked() {

        PackageManager m = getPackageManager();
        String s = getPackageName();
        PackageInfo p = null;
        try {
            p = m.getPackageInfo(s, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        s = "file://"+p.applicationInfo.dataDir+"/crop_image.png";

        try {
            mImageView.cropBitmapAsync(this, new URL(s), new BitmapCropCallback() {
                @Override
                public void onBitmapCropped(@NonNull URL resultUri) {
                    Bitmap bitmap = new ImageCache(getCacheDir(), 1000).readFromDiskCache(resultUri);
                    imageViewReview.setImageBitmap(bitmap);
                }

                @Override
                public void onCropFailure(@NonNull Throwable t) {
                    t.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.rotate_button)
    public void onImageRotateClicked() {
        rotateImage();
    }

    @OnClick(R.id.snap_button)
    public void onImageSnapClicked() {
        snapImage();
    }

    @OnCheckedChanged(R.id.gesture_checkbox)
    public void toggleGestures() {
        boolean enabled = mImageView.isGestureEnabled();
        enabled = !enabled;
        mImageView.setGestureEnabled(enabled);
        Toast.makeText(this, "Gesture " + (enabled ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
    }

    private void loadNewImage(String filePath) {
        rotationCount = 0;
        Log.i(TAG, "load image: " + filePath);
        mBitmap = BitmapFactory.decodeFile(filePath);
        originalBitmap = mBitmap;
        Log.i(TAG, "bitmap: " + mBitmap.getWidth() + " " + mBitmap.getHeight());

        int maxP = Math.max(mBitmap.getWidth(), mBitmap.getHeight());
        float scale1280 = (float)maxP / 1280;
        Log.i(TAG, "scaled: " + scale1280 + " - " + (1/scale1280));

        if (mImageView.getWidth() != 0) {
            mImageView.setMaxZoom(mImageView.getWidth() * 2 / 1280f);
        } else {

            ViewTreeObserver vto = mImageView.getViewTreeObserver();
            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mImageView.getViewTreeObserver().removeOnPreDrawListener(this);
                    mImageView.setMaxZoom(mImageView.getWidth() * 2 / 1280f);
                    return true;
                }
            });

        }

        mBitmap = Bitmap.createScaledBitmap(mBitmap, (int)(mBitmap.getWidth()/scale1280),
                (int)(mBitmap.getHeight()/scale1280), true);

        mImageView.setImageBitmap(mBitmap);
    }

    private void startGalleryIntent() {

        if (!hasGalleryPermission()) {
            askForGalleryPermission();
            return;
        }

        Intent intent = IntentHelpers.getPickImageChooserIntent(this, "Image Picker", true, true);
        startActivityForResult(intent, REQUEST_GALLERY);
    }



    private boolean hasGalleryPermission() {
        return ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void askForGalleryPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_CODE_READ_PERMISSION);
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent resultIntent) {
        super.onActivityResult(requestCode, responseCode, resultIntent);

        if (responseCode == RESULT_OK) {
            String absPath = BitmapUtils.getFilePathFromUri(this, resultIntent.getData());
            loadNewImage(absPath);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_READ_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGalleryIntent();
                return;
            }
        }

        Toast.makeText(this, "Gallery permission not granted", Toast.LENGTH_SHORT).show();
    }

    private void cropImageAsync() {

    }

    private void cropImage() {


    }

    private ScaledCropper prepareCropForOriginalImage() {
        return null;
    }

    private void cropOriginalImage() {
        if (originalBitmap != null) {
            ScaledCropper cropper = prepareCropForOriginalImage();
            if (cropper == null) {
                return;
            }

            Bitmap bitmap = cropper.cropBitmap();
            if (bitmap != null) {
                try {
                    BitmapUtils.writeBitmapToFile(bitmap, new File(Environment.getExternalStorageDirectory() + "/crop_test_info_orig.jpg"), 90);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void cropOriginalImageAsync() {
        if (originalBitmap != null) {
            ScaledCropper cropper = prepareCropForOriginalImage();
            if (cropper == null) {
                return;
            }

            cropper.crop(new CropperCallback() {
                @Override
                public void onCropped(Bitmap bitmap) {
                    if (bitmap != null) {
                        try {
                            BitmapUtils.writeBitmapToFile(bitmap, new File(Environment.getExternalStorageDirectory() + "/crop_test_info_orig.jpg"), 90);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void rotateImage() {
        if (mBitmap == null) {
            Log.e(TAG, "bitmap is not loaded yet");
            return;
        }

        mBitmap = BitmapUtils.rotateBitmap(mBitmap, 90);
        mImageView.setImageBitmap(mBitmap);
        rotationCount++;
    }

    private void snapImage() {
        if (isSnappedToCenter) {
            mImageView.cropToCenter();
        } else {
            mImageView.fitToCenter();
        }

        isSnappedToCenter = !isSnappedToCenter;
    }
}
