package com.fenchtose.nocropper;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Crops part of image that fills the crop bounds.
 * <p/>
 * First image is downscaled if max size was set and if resulting image is larger that max size.
 * Then image is rotated accordingly.
 * Finally new Bitmap object is created and saved to file.
 */
public class BitmapCropTask extends AsyncTask<Void, Void, Throwable> {

    private static final String TAG = "BitmapCropTask";
    private Bitmap bitmap;
    private BitmapCropCallback cropCallback;
    private ImageCache imageCache;
    private final WeakReference<Context> context;
    private final URL saveURL;

    public BitmapCropTask(Context context, @Nullable Bitmap bitmap,
                          @Nullable BitmapCropCallback cropCallback, URL saveURL) {
        this.bitmap = bitmap;
        this.cropCallback = cropCallback;
        this.context = new WeakReference<>(context);
        this.imageCache = new ImageCache(this.context.get().getCacheDir(), bitmap.getByteCount());
        this.saveURL = saveURL;
    }

    @Override
    @Nullable
    protected Throwable doInBackground(Void... params) {
        if (bitmap == null) {
            return new NullPointerException("ViewBitmap is null");
        } else if (bitmap.isRecycled()) {
            return new NullPointerException("ViewBitmap is recycled");
        }

        try {
            crop();
            bitmap = null;
        } catch (Throwable throwable) {
            return throwable;
        }

        return null;
    }

    private boolean crop() {
        saveImage(bitmap);
        return true;
    }

    private void saveImage(@NonNull Bitmap croppedBitmap) {
        try {
            boolean saved = imageCache.writeToDiskCache(this.saveURL, bitmap);
            if (!saved) {
                cropCallback.onCropFailure(new Exception("save bitmap failed"));
            }
            croppedBitmap.recycle();
        } finally {

        }
    }

    @Override
    protected void onPostExecute(@Nullable Throwable t) {
        if (cropCallback != null) {
            if (t == null) {
                try {
                    URI oldUri = saveURL.toURI();
                    Uri uri  = new Uri.Builder().scheme(oldUri.getScheme())
                            .encodedAuthority(oldUri.getRawAuthority())
                            .encodedPath(oldUri.getRawPath())
                            .query(oldUri.getRawQuery())
                            .fragment(oldUri.getRawFragment())
                            .build();

                    cropCallback.onBitmapCropped(saveURL);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    cropCallback.onCropFailure(e);
                }
            } else {
                cropCallback.onCropFailure(t);
            }
        }
    }

}
