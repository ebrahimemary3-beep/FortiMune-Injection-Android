package com.fortimune.injection;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;

// IVAS self-update: install status from PackageInstaller, and a "updated — tap to open" notice after replacement.
public class UpdateReceiver extends BroadcastReceiver {
    static final String ACTION_STATUS = "com.fortimune.injection.INSTALL_STATUS";
    interface Listener { void onInstallStatus(int status, String message); }
    static volatile Listener listener;

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) { notifyUpdated(context); return; }
        if (!ACTION_STATUS.equals(action)) return;
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = Build.VERSION.SDK_INT >= 33
                ? intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class)
                : intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { context.startActivity(confirm); } catch (Exception ignored) {}
            }
        }
        Listener l = listener;
        if (l != null) l.onInstallStatus(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
    }

    private void notifyUpdated(Context context) {
        try {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm == null) return;
            NotificationChannel ch = new NotificationChannel("ivas_updates", "تحديثات IVAS", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(ch);
            Intent open = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(context, 2101, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            android.app.Notification n = new android.app.Notification.Builder(context, "ivas_updates")
                .setSmallIcon(R.drawable.ic_fortimune)
                .setContentTitle("IVAS اتحدّث ✓")
                .setContentText("النسخة " + BuildConfig.VERSION_NAME + " جاهزة — اضغط للفتح")
                .setContentIntent(pi).setAutoCancel(true).build();
            nm.notify(2101, n);
        } catch (Exception ignored) {}
    }
}
