package com.fenchtose.nocropper;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.net.URI;
import java.net.URL;

public interface BitmapCropCallback {

    void onBitmapCropped(@NonNull URL resultUri, int byteCount);

    void onCropFailure(@NonNull Throwable t);

}