package com.fenchtose.nocropper;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by trunglt on 11/2/2018.
 */
public class BitmapUtils {

    public static Bitmap getCroppedBitmap(Bitmap bitmap, CropInfo cropInfo) throws OutOfMemoryError, IllegalArgumentException {
        if (!cropInfo.addPadding) {
            return Bitmap.createBitmap(bitmap, cropInfo.x, cropInfo.y, cropInfo.width, cropInfo.height);
        }

        return BitmapUtils.addPadding(bitmap, cropInfo, cropInfo.paddingColor);
    }

    public static Bitmap addPadding(Bitmap bmp, CropInfo info, int color) throws OutOfMemoryError {

        if (bmp == null) {
            return null;
        }

        Bitmap bitmap = null;

        try {

            int biggerParam = Math.max(info.width + 2*info.horizontalPadding, info.height + 2*info.verticalPadding);
            bitmap = Bitmap.createBitmap(biggerParam, biggerParam, bmp.getConfig());
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(color);

            Rect dest = new Rect(info.horizontalPadding, info.verticalPadding, info.horizontalPadding + info.width, info.verticalPadding + info.height);
            Rect src = new Rect(info.x, info.y, info.x + info.width, info.y + info.height);

            canvas.drawBitmap(bmp, src, dest, null);
            return bitmap;

        } catch (OutOfMemoryError e) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }

            throw e;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getFilePathFromUri(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {

                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    try {
                        final Uri contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                        return getDataColumn(context, contentUri, null, null);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {

            cursor = context.getContentResolver()
                    .query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {

                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * Write the given bitmap into the given file. JPEG is used as the compression format with
     * quality set
     * to 100.
     *
     * @param bm   The bitmap.
     * @param file The file to write the bitmap into.
     */
    public static void writeBitmapToFile(Bitmap bm, File file, int quality) throws IOException {

        FileOutputStream fos = new FileOutputStream(file);
        bm.compress(Bitmap.CompressFormat.JPEG, quality, fos);
        fos.flush();
        fos.close();
    }

    public static Bitmap addPadding(@NonNull Bitmap bmp) {

        int biggerParam = Math.max(bmp.getWidth(), bmp.getHeight());
        Bitmap bitmap = Bitmap.createBitmap(biggerParam, biggerParam, bmp.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        int top = bmp.getHeight() > bmp.getWidth() ? 0 : (bmp.getWidth() - bmp.getHeight())/2;
        int left = bmp.getWidth() > bmp.getHeight() ? 0 : (bmp.getHeight() - bmp.getWidth())/2;

        canvas.drawBitmap(bmp, left, top, null);
        return bitmap;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static String encodeTobase64(Bitmap image) {
        Bitmap immagex = image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.PNG, 90, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);
        return imageEncoded;
    }

    public static byte[] encodeToArrayByte(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    public static Bitmap decodeBase64(String input) {
        byte[] decodedByte = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedByte, 0,      decodedByte.length);
    }

}
