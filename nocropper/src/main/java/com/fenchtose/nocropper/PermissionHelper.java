package com.fenchtose.nocropper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;

public class PermissionHelper {

    public static boolean isExplicitCameraPermissionRequired(@NonNull Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && hasPermissionInManifest(context, "android.permission.CAMERA")
                && context.checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPermissionInManifest(
            @NonNull Context context, @NonNull String permissionName) {
        String packageName = context.getPackageName();
        try {
            PackageInfo packageInfo =
                    context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            final String[] declaredPermisisons = packageInfo.requestedPermissions;
            if (declaredPermisisons != null && declaredPermisisons.length > 0) {
                for (String p : declaredPermisisons) {
                    if (p.equalsIgnoreCase(permissionName)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

}
