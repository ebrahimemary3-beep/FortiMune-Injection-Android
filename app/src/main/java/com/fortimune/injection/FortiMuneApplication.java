package com.fortimune.injection;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import org.json.JSONObject;
import com.onesignal.OneSignal;

public class FortiMuneApplication extends Application {
    // Held in memory; never log or persist a device possession challenge.
    private static JSONObject proof;
    private static JSONObject pendingOpen;
    static synchronized void captureProof(JSONObject data) { proof=data; }
    static synchronized void clearProof() { proof=null; }
    static synchronized JSONObject getProof() { return proof; }
    static synchronized JSONObject takeOpen() { JSONObject p=pendingOpen; pendingOpen=null; return p; }
    // IVAS: high-priority channel with the IVAS tone for notifications that need action (approval, new batch, assignment).
    static final String ALERT_CHANNEL="ivas_alerts";
    static boolean alertChannelReady=false;
    private void createAlertChannel() {
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.O) return;
        try {
            NotificationManager nm=getSystemService(NotificationManager.class);
            if(nm==null) return;
            NotificationChannel ch=new NotificationChannel(ALERT_CHANNEL,"تنبيهات IVAS — مطلوب إجراء",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("دفعة جديدة، موافقة مطلوبة، أو دفعة متوزعة عليك");
            Uri sound=Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.ivas_alert);
            ch.setSound(sound,new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0,350,180,350,180,600});
            ch.enableLights(true);
            ch.setLightColor(0xFF26478A);
            ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
            alertChannelReady=nm.getNotificationChannel(ALERT_CHANNEL)!=null;
        } catch(Exception ignored) { alertChannelReady=false; }
    }
    @Override public void onCreate() {
        super.onCreate();
        createAlertChannel();
        OneSignal.initWithContext(this,"027bf229-1372-4209-9ba5-e86ea84ebb74");
        OneSignal.getNotifications().addForegroundLifecycleListener(event -> {
            JSONObject data=event.getNotification().getAdditionalData();
            if(data!=null && data.optBoolean("nativeChallenge")) {
                event.preventDefault();
                synchronized(FortiMuneApplication.class) { proof=data; }
            }
        });
        OneSignal.getNotifications().addClickListener(event -> {
            JSONObject data=event.getNotification().getAdditionalData();
            if(data==null || data.optBoolean("nativeChallenge")) return;
            synchronized(FortiMuneApplication.class) { pendingOpen=data; }
        });
    }
}
